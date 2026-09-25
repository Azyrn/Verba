package com.skeler.verba.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Plays read-aloud speech as raw 16-bit mono PCM while it's still arriving,
 * so the voice starts with the first bytes rather than after the whole clip
 * downloads, with no decoder in between. One clip at a time.
 */
@Singleton
class SpeechPlayer @Inject constructor() {
    @Volatile private var track: AudioTrack? = null

    /**
     * Plays [pcm] to its end, calling [onStart] once sound begins. Cancel the
     * caller to end it early.
     */
    suspend fun play(pcm: InputStream, onStart: () -> Unit) = withContext(Dispatchers.IO) {
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        check(minBuffer > 0) { "PCM output unsupported" }
        val next = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL)
                    .setEncoding(ENCODING)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuffer, BUFFER_BYTES))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        check(next.state == AudioTrack.STATE_INITIALIZED) { "AudioTrack failed to initialize" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Otherwise playback waits for a full buffer before making a sound.
            next.setStartThresholdInFrames(PRIME_BYTES / FRAME_BYTES)
        }
        track = next
        try {
            val chunk = ByteArray(8 * 1024)
            var pending = 0 // bytes held back so writes stay whole samples
            var written = 0L
            var started = false
            while (true) {
                ensureActive()
                val read = pcm.read(chunk, pending, chunk.size - pending)
                if (read < 0) break
                val total = pending + read
                val whole = total - total % FRAME_BYTES
                if (whole > 0) {
                    if (!next.writeAll(chunk, whole)) break
                    written += whole
                }
                pending = total - whole
                if (pending > 0) chunk[0] = chunk[whole]
                // A short head start so a slow patch of network doesn't stutter.
                if (!started && written >= PRIME_BYTES) {
                    next.play()
                    started = true
                    onStart()
                }
            }
            ensureActive()
            if (!started && written > 0) {
                next.play()
                onStart()
            }
            // stop() in stream mode plays out what's buffered; wait for the
            // last frame, giving up if the position stops moving.
            next.stop()
            val frames = written / FRAME_BYTES
            var last = -1
            var still = 0
            while (next.playbackHeadPosition < frames && still < STALL_POLLS) {
                ensureActive()
                val head = next.playbackHeadPosition
                if (head == last) still++ else still = 0
                last = head
                delay(POLL_MILLIS)
            }
        } finally {
            track = null
            runCatching { next.pause(); next.flush() }
            next.release()
        }
    }

    /** Silences the clip at once; cancelling the [play] caller ends it. */
    fun stop() {
        track?.let { runCatching { it.pause(); it.flush() } }
    }

    /** Non-blocking writes, so a cancelled caller never hangs on a full buffer. */
    private suspend fun AudioTrack.writeAll(data: ByteArray, size: Int): Boolean {
        var offset = 0
        while (offset < size) {
            currentCoroutineContext().ensureActive()
            val n = write(data, offset, size - offset, AudioTrack.WRITE_NON_BLOCKING)
            if (n < 0) return false
            offset += n
            if (offset < size) delay(POLL_MILLIS / 5)
        }
        return true
    }

    companion object {
        const val SAMPLE_RATE = 24_000
        private const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_BYTES = 2
        private const val BUFFER_BYTES = SAMPLE_RATE * FRAME_BYTES // one second
        private const val PRIME_BYTES = SAMPLE_RATE * FRAME_BYTES / 4 // 250 ms
        private const val POLL_MILLIS = 50L
        private const val STALL_POLLS = 20
    }
}
