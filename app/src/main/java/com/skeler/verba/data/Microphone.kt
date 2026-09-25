package com.skeler.verba.data

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder

/** 16 kHz is the transcription model's native rate, so nothing gets resampled. */
internal const val MIC_SAMPLE_RATE = 16_000

/** Loudest sample a recording is turned up to, and how far it may be turned up. */
internal const val MIC_TARGET_PEAK = 29_000f
internal const val MIC_MAX_GAIN = 20f

/**
 * Opens the mic as 16 kHz mono 16-bit PCM and starts it, or returns null.
 * Speech from across a room or out of a laptop speaker arrives quiet, and
 * phone voice processing tends to treat it as background noise, so the raw
 * mic is used where the phone offers one. The caller must hold RECORD_AUDIO.
 */
@SuppressLint("MissingPermission")
internal fun openMicrophone(context: Context): AudioRecord? {
    val channel = AudioFormat.CHANNEL_IN_MONO
    val encoding = AudioFormat.ENCODING_PCM_16BIT
    val minBuffer = AudioRecord.getMinBufferSize(MIC_SAMPLE_RATE, channel, encoding)
    if (minBuffer <= 0) return null
    val audio = context.getSystemService(AudioManager::class.java)
    val raw = audio?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
    val source = if (raw == "true") MediaRecorder.AudioSource.UNPROCESSED
    else MediaRecorder.AudioSource.VOICE_RECOGNITION
    val record = try {
        AudioRecord(source, MIC_SAMPLE_RATE, channel, encoding, maxOf(minBuffer, MIC_SAMPLE_RATE))
    } catch (e: Exception) {
        return null
    }
    val started = record.state == AudioRecord.STATE_INITIALIZED &&
        runCatching { record.startRecording() }.isSuccess &&
        record.recordingState == AudioRecord.RECORDSTATE_RECORDING
    if (!started) {
        record.release()
        return null
    }
    return record
}
