package com.skeler.verba.data

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.skeler.verba.model.Language
import com.skeler.verba.model.Languages
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One Verba language ML Kit can translate on-device, paired with its ML Kit tag. */
data class OfflineLanguage(val language: Language, val tag: String)

/**
 * Manages the per-language model files the offline engine translates from. ML
 * Kit downloads a pair's models on first use anyway, but this lets the user
 * fetch their languages ahead of time — over Wi-Fi, before going offline — and
 * reclaim the space by deleting ones they no longer need.
 */
@Singleton
class MlKitModelManager @Inject constructor() {

    private val manager = RemoteModelManager.getInstance()

    /**
     * The Verba languages ML Kit has an on-device model for, each with its
     * canonical ML Kit tag. Fixed set, so it's computed once.
     */
    val supported: List<OfflineLanguage> = Languages.all.mapNotNull { language ->
        TranslateLanguage.fromLanguageTag(language.code)
            ?.let { OfflineLanguage(language, it) }
    }

    /** ML Kit tags whose model is currently on disk. */
    suspend fun downloadedTags(): Set<String> = withContext(Dispatchers.IO) {
        runCatching {
            Tasks.await(manager.getDownloadedModels(TranslateRemoteModel::class.java))
                .map { it.language }
                .toSet()
        }.getOrDefault(emptySet())
    }

    /** Fetches [tag]'s model; true on success. Allows cellular — the user chose to. */
    suspend fun download(tag: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val model = TranslateRemoteModel.Builder(tag).build()
            Tasks.await(manager.download(model, DownloadConditions.Builder().build()))
            true
        }.getOrDefault(false)
    }

    /** Removes [tag]'s model from disk; true on success. */
    suspend fun delete(tag: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val model = TranslateRemoteModel.Builder(tag).build()
            Tasks.await(manager.deleteDownloadedModel(model))
            true
        }.getOrDefault(false)
    }
}
