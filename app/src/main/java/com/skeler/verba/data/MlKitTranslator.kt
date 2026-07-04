package com.skeler.verba.data

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.skeler.verba.model.Language
import com.skeler.verba.model.TranslationError
import java.io.IOException
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device translation via Google ML Kit — the offline engine. Once a language
 * pair's two small models are on disk, the pair translates with the radios off;
 * the first use of a pair downloads them, which needs a connection that one time.
 * ML Kit covers a fixed set of languages, so a pair it can't map resolves to
 * [TranslationError.LANGUAGE_UNSUPPORTED] rather than a silent wrong answer.
 */
@Singleton
class MlKitTranslator @Inject constructor() {

    suspend fun translate(
        text: String,
        source: Language,
        target: Language,
    ): TranslationOutcome = withContext(Dispatchers.IO) {
        try {
            val targetTag = TranslateLanguage.fromLanguageTag(target.code)
                ?: return@withContext fail(TranslationError.LANGUAGE_UNSUPPORTED)
            val sourceTag = resolveSource(text, source)
                ?: return@withContext fail(TranslationError.LANGUAGE_UNSUPPORTED)

            // Same language both ends — nothing to translate, and ML Kit rejects it.
            if (sourceTag == targetTag) return@withContext TranslationOutcome.Success(text)

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceTag)
                .setTargetLanguage(targetTag)
                .build()
            val translator = Translation.getClient(options)
            try {
                // Downloads the pair's models on first use; a no-op once cached.
                Tasks.await(translator.downloadModelIfNeeded())
                val result = Tasks.await(translator.translate(text)).trim()
                if (result.isEmpty()) fail(TranslationError.EMPTY_RESPONSE)
                else TranslationOutcome.Success(result)
            } finally {
                translator.close()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ExecutionException) {
            // The commonest failure is a missing model with no network to fetch it.
            fail(TranslationError.NETWORK)
        } catch (e: IOException) {
            fail(TranslationError.NETWORK)
        } catch (e: Exception) {
            fail(TranslationError.UNKNOWN)
        }
    }

    /**
     * Maps an explicit source straight through; for "detect" it runs ML Kit's
     * on-device language id first. Null means ML Kit has no model for it.
     */
    private fun resolveSource(text: String, source: Language): String? {
        if (!source.isAuto) return TranslateLanguage.fromLanguageTag(source.code)
        val identifier = LanguageIdentification.getClient()
        return try {
            val identified = Tasks.await(identifier.identifyLanguage(text))
            if (identified == "und") null else TranslateLanguage.fromLanguageTag(identified)
        } finally {
            identifier.close()
        }
    }

    private fun fail(error: TranslationError): TranslationOutcome =
        TranslationOutcome.Failure(error)
}
