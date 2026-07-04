package com.skeler.verba.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI-dialect chat request, shared by every provider. Nullable knobs are
 * dropped from the JSON (explicitNulls = false): OpenAI's GPT-5 family
 * rejects `max_tokens` and non-default `temperature`, so those requests send
 * `max_completion_tokens` and no temperature instead.
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("max_completion_tokens") val maxCompletionTokens: Int? = null,
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice> = emptyList(),
    val error: ApiError? = null,
)

@Serializable
data class ChatChoice(
    val message: ChatMessage? = null,
)

@Serializable
data class ApiError(
    val code: Int? = null,
    val message: String? = null,
)
