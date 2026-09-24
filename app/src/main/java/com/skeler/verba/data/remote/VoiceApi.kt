package com.skeler.verba.data.remote

import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * The Worker's voice routes (worker/src/index.ts). Like translation, the xAI
 * key, model and limits live in the Worker; the app sends audio or text.
 */
interface VoiceApi {

    /** [audio] is the WAV recording; [language] a code hint, or null to detect. */
    @POST("stt")
    suspend fun transcribe(
        @Query("language") language: String?,
        @Body audio: RequestBody,
    ): Response<TranscribeResponse>

    /** Answers with audio bytes in [SpeakRequest.format]. */
    @Streaming
    @POST("tts")
    suspend fun speak(@Body request: SpeakRequest): Response<ResponseBody>
}

@Serializable
data class TranscribeResponse(
    val text: String? = null,
    val error: String? = null,
)

@Serializable
data class SpeakRequest(
    val text: String,
    val voice: String,
    /** Language code of [text], or null to let the voice detect it. */
    val language: String?,
    /** "pcm" for raw 24 kHz 16-bit mono, streamed; the Worker defaults to MP3. */
    val format: String? = null,
)
