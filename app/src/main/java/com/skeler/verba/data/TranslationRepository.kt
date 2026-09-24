package com.skeler.verba.data

import com.skeler.verba.BuildConfig
import com.skeler.verba.data.remote.TranslateApi
import com.skeler.verba.data.remote.TranslateRequest
import com.skeler.verba.model.Language
import com.skeler.verba.model.TranslationError
import com.skeler.verba.model.VerbaModel
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

sealed interface TranslationOutcome {
    data class Success(val text: String) : TranslationOutcome
    data class Failure(val error: TranslationError) : TranslationOutcome
}

@Singleton
class TranslationRepository @Inject constructor(
    private val api: TranslateApi,
    private val offline: MlKitTranslator,
) {

    suspend fun translate(
        text: String,
        source: Language,
        target: Language,
        model: VerbaModel,
    ): TranslationOutcome {
        // On-device engines answer locally — no network, no Worker.
        if (model.provider.onDevice) {
            return offline.translate(text, source, target)
        }
        if (BuildConfig.VERBA_API_URL.isBlank() || BuildConfig.VERBA_APP_TOKEN.isBlank()) {
            return TranslationOutcome.Failure(TranslationError.NOT_CONFIGURED)
        }

        return try {
            val response = api.translate(
                TranslateRequest(
                    text = text,
                    source = source.name.takeUnless { source.isAuto },
                    target = target.name,
                ),
            )
            if (!response.isSuccessful) {
                return TranslationOutcome.Failure(errorForStatus(response.code()))
            }
            val translation = response.body()?.translation?.trim().orEmpty()
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

    /** Status codes as worker/src/index.ts sends them. */
    private fun errorForStatus(code: Int): TranslationError = when (code) {
        401 -> TranslationError.UNAUTHORIZED
        413 -> TranslationError.TEXT_TOO_LONG
        429 -> TranslationError.RATE_LIMITED
        502, 504 -> TranslationError.MODEL_UNAVAILABLE
        else -> TranslationError.UNKNOWN
    }
}
