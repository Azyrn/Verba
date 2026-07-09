package com.skeler.verba.data

import com.skeler.verba.BuildConfig
import com.skeler.verba.data.remote.ChatApi
import com.skeler.verba.data.remote.ChatMessage
import com.skeler.verba.data.remote.ChatRequest
import com.skeler.verba.model.Language
import com.skeler.verba.model.Provider
import com.skeler.verba.model.TranslationError
import com.skeler.verba.model.VerbaModel
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

sealed interface TranslationOutcome {
    data class Success(val text: String) : TranslationOutcome
    data class Failure(val error: TranslationError) : TranslationOutcome
}

@Singleton
class TranslationRepository @Inject constructor(
    private val api: ChatApi,
    private val settings: SettingsRepository,
    private val offline: MlKitTranslator,
) {

    suspend fun translate(
        text: String,
        source: Language,
        target: Language,
        model: VerbaModel,
    ): TranslationOutcome {
        // On-device engines answer locally — no key, no network, no chat request.
        if (model.provider.onDevice) {
            return offline.translate(text, source, target)
        }

        val key = keyFor(model.provider)
            ?: return TranslationOutcome.Failure(TranslationError.MISSING_KEY)

        val instruction = if (source.isAuto) {
            "Detect the language of the user's text and translate it into ${target.name}."
        } else {
            "Translate the user's text from ${source.name} into ${target.name}."
        }
        val request = chatRequest(
            provider = model.provider,
            modelId = model.id,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "$instruction Reply with the translation only — " +
                        "no quotation marks, no notes, no alternatives.",
                ),
                ChatMessage(role = "user", content = text),
            ),
        )

        return try {
            val response = api.chat(model.provider.chatUrl, "Bearer $key", request)
            if (!response.isSuccessful) {
                return TranslationOutcome.Failure(errorForStatus(response.code()))
            }
            val body = response.body()
                ?: return TranslationOutcome.Failure(TranslationError.EMPTY_RESPONSE)
            // OpenRouter reports some upstream failures as 200s with an error payload.
            body.error?.let { apiError ->
                return TranslationOutcome.Failure(errorForStatus(apiError.code ?: 0))
            }
            val translation = body.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            if (translation.isEmpty()) {
                TranslationOutcome.Failure(TranslationError.EMPTY_RESPONSE)
            } else {
                TranslationOutcome.Success(translation)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            TranslationOutcome.Failure(TranslationError.NETWORK)
        } catch (e: Exception) {
            TranslationOutcome.Failure(TranslationError.UNKNOWN)
        }
    }

    /**
     * A personal key always wins; a bundled key backs whichever free-tier
     * models ship with the app when the user hasn't added their own.
     */
    private suspend fun keyFor(provider: Provider): String? {
        val personal = settings.apiKey(provider).first()
        if (personal != null) return personal
        return bundledKeyFor(provider)
    }

    private fun bundledKeyFor(provider: Provider): String? = when (provider) {
        Provider.OPENROUTER -> BuildConfig.OPENROUTER_API_KEY.takeIf { it.isNotBlank() }
        Provider.GOOGLE -> BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
        else -> null
    }

    private fun errorForStatus(code: Int): TranslationError = when (code) {
        401, 403 -> TranslationError.INVALID_KEY
        402, 429 -> TranslationError.RATE_LIMITED
        400, 404, 502, 503 -> TranslationError.MODEL_UNAVAILABLE
        408 -> TranslationError.NETWORK
        else -> TranslationError.UNKNOWN
    }

    companion object {
        /**
         * Shapes an OpenAI-dialect request for [provider]: GPT-5-family
         * endpoints reject `max_tokens` and pinned temperatures, so OpenAI
         * gets `max_completion_tokens` and the model's default temperature.
         */
        fun chatRequest(
            provider: Provider,
            modelId: String,
            messages: List<ChatMessage>,
            budgetTokens: Int = 2048,
        ): ChatRequest = ChatRequest(
            model = modelId,
            messages = messages,
            temperature = if (provider == Provider.OPENAI) null else 0.2,
            maxTokens = if (provider == Provider.OPENAI) null else budgetTokens,
            maxCompletionTokens = if (provider == Provider.OPENAI) budgetTokens else null,
        )
    }
}
