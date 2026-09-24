package com.skeler.verba.data

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Plays one read-aloud clip at a time; starting another stops the first. */
@Singleton
class SpeechPlayer @Inject constructor() {
    private var player: MediaPlayer? = null

    /** False if the clip couldn't be played; [onDone] fires when it finishes on its own. */
    fun play(file: File, onDone: () -> Unit): Boolean {
        stop()
        val next = MediaPlayer()
        return try {
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            next.setDataSource(file.absolutePath)
            next.setOnCompletionListener {
                stop()
                onDone()
            }
            next.prepare()
            next.start()
            player = next
            true
        } catch (e: Exception) {
            next.release()
            false
        }
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }
}
