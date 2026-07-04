package com.skeler.verba.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * One client for every provider: they all speak OpenAI's chat-completions
 * dialect, so the call site picks the absolute URL and the Bearer token.
 */
interface ChatApi {

    @POST
    suspend fun chat(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest,
    ): Response<ChatResponse>
}
