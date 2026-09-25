package com.skeler.verba.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Verba's translation Worker (worker/src/index.ts). The app sends only the
 * text and language names; the Worker owns the model, prompt and key.
 */
interface TranslateApi {

    @POST(".")
    suspend fun translate(@Body request: TranslateRequest): Response<TranslateResponse>
}

@Serializable
data class TranslateRequest(
    val text: String,
    /** English name of the source language, or null to auto-detect. */
    val source: String?,
    val target: String,
)

@Serializable
data class TranslateResponse(
    val translation: String? = null,
    val error: String? = null,
)
