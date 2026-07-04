package com.skeler.verba.data

import com.skeler.verba.data.remote.ChatApi
import com.skeler.verba.data.remote.ChatMessage
import com.skeler.verba.model.Provider
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * The verdict of a live probe of a key *and* a model id together — every case
 * the settings form can surface a distinct message for.
 */
enum class CredentialCheck {
    /** Key and model both accepted — the exact pair the user will translate with works. */
    VALID,

    /** 401/403: the provider rejected the key, or it isn't authorized for this model. */
    INVALID_KEY,

    /** 400/404: the key is fine, but the provider has no such model id. */
    MODEL_NOT_FOUND,

    /** 402/429: key fine, but throttled or out of quota right now. */
    RATE_LIMITED,

    /** Never reached the provider — says nothing about either value. */
    UNREACHABLE,

    /** Reached the provider, but the failure didn't classify. */
    UNKNOWN,
}

/**
 * Validates a personal key together with the model id it will be used against,
 * in one tiny real request, before anything is persisted. Probing both at once
 * is the point: a green light means that exact pair answers, and the status
 * code tells a bad key apart from a missing model.
 */
@Singleton
class KeyValidator @Inject constructor(
    private val api: ChatApi,
) {

    suspend fun verify(provider: Provider, key: String, modelId: String): CredentialCheck {
        val request = TranslationRepository.chatRequest(
            provider = provider,
            modelId = modelId.trim(),
            messages = listOf(ChatMessage(role = "user", content = "ping")),
            budgetTokens = 16,
        )
        return try {
            val response = api.chat(provider.chatUrl, "Bearer ${key.trim()}", request)
            if (response.isSuccessful) {
                // Some providers report a bad model as a 200 with an error payload
                // instead of a 4xx; honour that before calling the pair valid.
                response.body()?.error?.let { classify(it.code ?: 0) } ?: CredentialCheck.VALID
            } else {
                classify(response.code())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            CredentialCheck.UNREACHABLE
        } catch (e: Exception) {
            CredentialCheck.UNKNOWN
        }
    }

    private fun classify(code: Int): CredentialCheck = when (code) {
        401, 403 -> CredentialCheck.INVALID_KEY
        400, 404 -> CredentialCheck.MODEL_NOT_FOUND
        402, 429 -> CredentialCheck.RATE_LIMITED
        else -> CredentialCheck.UNKNOWN
    }
}
