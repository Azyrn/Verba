package com.skeler.verba.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records the microphone to a small mono M4A — the container xAI detects on
 * its own, at a bitrate speech recognition doesn't need more than. The
 * caller must already hold RECORD_AUDIO.
 */
@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private val file = File(context.cacheDir, "voice-input.m4a")

    /** Starts recording; false if the mic couldn't be opened. [onLimit] fires at [MAX_MILLIS]. */
    fun start(onLimit: () -> Unit): Boolean {
        release()
        @Suppress("DEPRECATION")
        val next = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else MediaRecorder()
        return try {
            next.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(48_000)
                setMaxDuration(MAX_MILLIS)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onLimit()
                }
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = next
            true
        } catch (e: Exception) {
            next.release()
            false
        }
    }

    /** Stops and returns the recording, or null if nothing usable was captured. */
    fun stop(): File? {
        val active = recorder ?: return null
        recorder = null
        // stop() throws when stopped too soon after start — no audio was written.
        val ok = runCatching { active.stop() }.isSuccess
        active.release()
        return file.takeIf { ok && it.length() > 0 }
    }

    fun release() {
        recorder?.let { runCatching { it.stop() }; it.release() }
        recorder = null
    }

    companion object {
        const val MAX_MILLIS = 120_000
    }
}
