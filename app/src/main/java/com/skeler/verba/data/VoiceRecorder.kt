package com.skeler.verba.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min

/**
 * Batch dictation: records the mic ([openMicrophone]) and hands it over as a
 * WAV, turned up so its loudest moment sits near full scale. The caller must
 * already hold RECORD_AUDIO.
 */
@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val file = File(context.cacheDir, "voice-input.wav")
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var recording = false
    private var thread: Thread? = null
    private var pcm = ByteArrayOutputStream()

    /** Starts recording; false if the mic couldn't be opened. [onLimit] fires at [MAX_MILLIS]. */
    fun start(onLimit: () -> Unit): Boolean {
        release()
        val record = openMicrophone(context) ?: return false

        val out = ByteArrayOutputStream(MAX_BYTES / 4)
        pcm = out
        recording = true
        thread = Thread({
            val chunk = ByteArray(CHUNK_BYTES)
            try {
                while (recording) {
                    val read = record.read(chunk, 0, chunk.size)
                    if (read < 0) break
                    val room = MAX_BYTES - out.size()
                    out.write(chunk, 0, min(read, room))
                    if (read >= room) {
                        main.post(onLimit)
                        break
                    }
                }
            } finally {
                runCatching { record.stop() }
                record.release()
            }
        }, "voice-recorder").apply { start() }
        return true
    }

    /** Stops and returns the recording, or null if nothing usable was captured. */
    fun stop(): File? {
        if (thread == null) return null
        finish()
        val samples = ByteBuffer.wrap(pcm.toByteArray()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val audio = ShortArray(samples.remaining()).also { samples.get(it) }
        pcm = ByteArrayOutputStream()
        if (audio.size < SAMPLE_RATE / 4 || !normalize(audio)) return null
        writeWav(audio)
        return file
    }

    fun release() {
        finish()
        pcm = ByteArrayOutputStream()
    }

    private fun finish() {
        recording = false
        thread?.join()
        thread = null
    }

    /**
     * Removes any DC offset and scales the loudest sample to [TARGET_PEAK],
     * up to [MAX_GAIN]. False if the recording is effectively silence.
     */
    private fun normalize(audio: ShortArray): Boolean {
        val offset = audio.sumOf { it.toLong() } / audio.size
        var peak = 0L
        for (s in audio) peak = maxOf(peak, abs(s - offset))
        if (peak < SILENCE_PEAK) return false
        val gain = min(TARGET_PEAK / peak.toFloat(), MAX_GAIN)
        for (i in audio.indices) {
            audio[i] = ((audio[i] - offset) * gain).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return true
    }

    private fun writeWav(audio: ShortArray) {
        val dataBytes = audio.size * 2
        val buffer = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(dataBytes)
        buffer.asShortBuffer().put(audio)
        file.writeBytes(buffer.array())
    }

    companion object {
        const val MAX_MILLIS = 120_000
        private const val SAMPLE_RATE = MIC_SAMPLE_RATE
        private const val MAX_BYTES = SAMPLE_RATE * 2 * (MAX_MILLIS / 1000)
        private const val CHUNK_BYTES = SAMPLE_RATE / 10 * 2 // 100 ms
        private const val TARGET_PEAK = MIC_TARGET_PEAK
        private const val MAX_GAIN = MIC_MAX_GAIN
        /** About -60 dBFS: below this there's nothing to transcribe. */
        private const val SILENCE_PEAK = 33L
    }
}
