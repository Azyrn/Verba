package com.skeler.verba.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.skeler.verba.BuildConfig
import com.skeler.verba.model.Language
import com.skeler.verba.model.TranslationError
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TreeMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * Live dictation: streams the mic to the Worker's `/stt/live` WebSocket and
 * reports the transcript as it grows, instead of uploading one recording at
 * the end. One session at a time; every callback lands on the main thread.
 */
@Singleton
class LiveTranscriber @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val json: Json,
) {
    private val main = Handler(Looper.getMainLooper())
    private var session: Session? = null

    /**
     * Opens the mic and the stream; false if the mic couldn't be opened.
     * [onText] gets the whole transcript so far each time it changes,
     * [onLimit] fires at [VoiceRecorder.MAX_MILLIS], and [onError] if the
     * stream fails before [finish].
     */
    fun start(
        language: Language,
        onText: (String) -> Unit,
        onLimit: () -> Unit,
        onError: (TranslationError) -> Unit,
    ): Boolean {
        cancel()
        if (BuildConfig.VERBA_API_URL.isBlank() || BuildConfig.VERBA_APP_TOKEN.isBlank()) {
            main.post { onError(TranslationError.NOT_CONFIGURED) }
            return true
        }
        val record = openMicrophone(context) ?: return false
        session = Session(record, language, onText, onLimit, onError)
        return true
    }

    /** Stops the mic and waits for the last words; the full transcript, or the failure. */
    suspend fun finish(): VoiceOutcome<String> {
        val active = session ?: return VoiceOutcome.Failure(TranslationError.EMPTY_RESPONSE)
        session = null
        return active.finish()
    }

    fun cancel() {
        session?.close()
        session = null
    }

    private inner class Session(
        private val record: android.media.AudioRecord,
        language: Language,
        private val onText: (String) -> Unit,
        private val onLimit: () -> Unit,
        private val onError: (TranslationError) -> Unit,
    ) : WebSocketListener() {
        /** Transcript segments by start time; a later event for a segment replaces it. */
        private val segments = TreeMap<Double, String>()
        private val done = CompletableDeferred<TranslationError?>()
        @Volatile private var ready = false
        @Volatile private var recording = true
        @Volatile private var finishing = false
        private val socket: WebSocket
        private val thread: Thread

        init {
            val url = BuildConfig.VERBA_API_URL.trimEnd('/') + "/stt/live" +
                if (language.isAuto) "" else "?language=${language.code}"
            socket = client.newWebSocket(Request.Builder().url(url).build(), this)
            thread = Thread(::pump, "live-dictation").apply { start() }
        }

        /** Mic → socket in 100 ms frames, auto-gained; held back until xAI says it's ready. */
        private fun pump() {
            val samples = ShortArray(MIC_SAMPLE_RATE / 10)
            val held = ArrayDeque<ByteString>()
            var envelope = MIC_TARGET_PEAK / MIC_MAX_GAIN
            var sent = 0L
            try {
                while (recording) {
                    val read = record.read(samples, 0, samples.size)
                    if (read < 0) break
                    if (read == 0) continue
                    // Gain follows a slowly decaying peak, so quiet speech is lifted
                    // without pumping on every syllable.
                    var peak = 0
                    for (i in 0 until read) peak = max(peak, abs(samples[i].toInt()))
                    envelope = max(peak.toFloat(), envelope * ENVELOPE_DECAY)
                        .coerceAtLeast(MIC_TARGET_PEAK / MIC_MAX_GAIN)
                    val gain = min(MIC_TARGET_PEAK / envelope, MIC_MAX_GAIN)
                    val bytes = ByteArray(read * 2)
                    for (i in 0 until read) {
                        val s = (samples[i] * gain).toInt().coerceIn(-32768, 32767)
                        bytes[2 * i] = s.toByte()
                        bytes[2 * i + 1] = (s shr 8).toByte()
                    }
                    held.addLast(bytes.toByteString())
                    if (ready) while (held.isNotEmpty()) socket.send(held.removeFirst())
                    sent += bytes.size
                    if (sent >= MAX_BYTES) {
                        main.post(onLimit)
                        break
                    }
                }
                // Whatever was said before xAI was ready still counts.
                while (ready && held.isNotEmpty()) socket.send(held.removeFirst())
            } finally {
                runCatching { record.stop() }
                record.release()
            }
        }

        suspend fun finish(): VoiceOutcome<String> {
            finishing = true
            recording = false
            withContext(Dispatchers.IO) { thread.join() }
            socket.send("""{"type":"audio.done"}""")
            val error = withTimeoutOrNull(FINISH_TIMEOUT_MILLIS) { done.await() }
            val text = transcript()
            socket.close(1000, null)
            return when {
                text.isNotEmpty() -> VoiceOutcome.Success(text)
                error != null -> VoiceOutcome.Failure(error)
                else -> VoiceOutcome.Failure(TranslationError.EMPTY_RESPONSE)
            }
        }

        fun close() {
            finishing = true
            recording = false
            socket.cancel()
            done.complete(null)
        }

        private fun transcript(): String =
            synchronized(segments) { segments.values.filter { it.isNotBlank() }.joinToString(" ") }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val event = runCatching { json.decodeFromString<LiveEvent>(text) }.getOrNull() ?: return
            when (event.type) {
                "transcript.created" -> ready = true
                "transcript.partial" -> {
                    val start = event.start ?: return
                    synchronized(segments) {
                        // An event covers everything from its start on.
                        segments.tailMap(start, true).clear()
                        segments[start] = event.text.orEmpty().trim()
                    }
                    val now = transcript()
                    main.post { if (!done.isCompleted) onText(now) }
                }
                "transcript.done" -> done.complete(null)
                "error" -> fail(TranslationError.UNKNOWN)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            fail(response?.let { TranslationError.fromStatus(it.code) } ?: TranslationError.NETWORK)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            done.complete(null)
        }

        private fun fail(error: TranslationError) {
            recording = false
            if (!done.complete(error)) return
            if (!finishing) main.post { onError(error) }
        }
    }

    @Serializable
    private data class LiveEvent(
        val type: String,
        val text: String? = null,
        val start: Double? = null,
    )

    private companion object {
        const val MAX_BYTES = MIC_SAMPLE_RATE * 2L * (VoiceRecorder.MAX_MILLIS / 1000)
        /** Per 100 ms frame: the envelope halves in about two seconds. */
        const val ENVELOPE_DECAY = 0.966f
        const val FINISH_TIMEOUT_MILLIS = 8_000L
    }
}
