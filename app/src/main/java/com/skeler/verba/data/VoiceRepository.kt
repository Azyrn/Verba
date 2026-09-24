package com.skeler.verba.data

import android.content.Context
import com.skeler.verba.BuildConfig
import com.skeler.verba.data.remote.SpeakRequest
import com.skeler.verba.data.remote.VoiceApi
import com.skeler.verba.model.Language
import com.skeler.verba.model.TranslationError
import com.skeler.verba.model.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody

sealed interface VoiceOutcome<out T> {
    data class Success<T>(val value: T) : VoiceOutcome<T>
    data class Failure(val error: TranslationError) : VoiceOutcome<Nothing>
}

/** Speech-to-text and text-to-speech, both through the Worker's xAI routes. */
@Singleton
class VoiceRepository @Inject constructor(
    private val api: VoiceApi,
    @ApplicationContext private val context: Context,
) {
    /** The last clip fetched, so replaying the same text in the same voice is free. */
    private var cachedKey: String? = null
    private val clip = File(context.cacheDir, "read-aloud.mp3")

    suspend fun transcribe(audio: File, language: Language): VoiceOutcome<String> = call {
        val response = api.transcribe(
            language = language.code.takeUnless { language.isAuto },
            audio = audio.asRequestBody("audio/mp4".toMediaType()),
        )
        if (!response.isSuccessful) {
            return@call VoiceOutcome.Failure(TranslationError.fromStatus(response.code()))
        }
        val text = response.body()?.text?.trim().orEmpty()
        if (text.isEmpty()) VoiceOutcome.Failure(TranslationError.EMPTY_RESPONSE)
        else VoiceOutcome.Success(text)
    }

    suspend fun speak(text: String, voice: Voice, language: Language): VoiceOutcome<File> = call {
        val key = "${voice.id}:${language.code}:$text"
        if (key == cachedKey && clip.exists()) return@call VoiceOutcome.Success(clip)
        val response = api.speak(
            SpeakRequest(
                text = text,
                voice = voice.id,
                language = language.code.takeUnless { language.isAuto },
            ),
        )
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            return@call VoiceOutcome.Failure(TranslationError.fromStatus(response.code()))
        }
        withContext(Dispatchers.IO) {
            body.use { source -> clip.outputStream().use { source.byteStream().copyTo(it) } }
        }
        cachedKey = key
        VoiceOutcome.Success(clip)
    }

    private suspend fun <T> call(block: suspend () -> VoiceOutcome<T>): VoiceOutcome<T> {
        if (BuildConfig.VERBA_API_URL.isBlank() || BuildConfig.VERBA_APP_TOKEN.isBlank()) {
            return VoiceOutcome.Failure(TranslationError.NOT_CONFIGURED)
        }
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            cachedKey = null
            VoiceOutcome.Failure(TranslationError.NETWORK)
        } catch (e: Exception) {
            cachedKey = null
            VoiceOutcome.Failure(TranslationError.UNKNOWN)
        }
    }
}
