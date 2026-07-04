package com.skeler.verba.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One kept translation: what was asked, what came back, and between which languages. */
@Serializable
data class SavedTranslation(
    val sourceText: String,
    val translatedText: String,
    val sourceCode: String,
    val targetCode: String,
    val savedAtEpochMs: Long,
) {
    /** Same words between the same languages count as the same entry. */
    fun matches(other: SavedTranslation): Boolean =
        sourceText == other.sourceText &&
            translatedText == other.translatedText &&
            targetCode == other.targetCode
}

/**
 * Saved translations live in the same DataStore as every other persisted
 * thing in Verba, newest first, as one JSON list.
 */
@Singleton
class SavedTranslationsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {

    private val key = stringPreferencesKey("saved_translations")

    val saved: Flow<List<SavedTranslation>> = dataStore.data
        .map { decode(it[key]) }
        .distinctUntilChanged()

    /** Adds the entry, or removes its twin if it was already saved. Returns true when saved. */
    suspend fun toggle(entry: SavedTranslation): Boolean {
        var nowSaved = false
        dataStore.edit { preferences ->
            val current = decode(preferences[key])
            val existing = current.filter { it.matches(entry) }
            val updated = if (existing.isEmpty()) {
                nowSaved = true
                (listOf(entry) + current).take(MAX_ENTRIES)
            } else {
                current - existing.toSet()
            }
            preferences[key] = json.encodeToString(updated)
        }
        return nowSaved
    }

    private fun decode(raw: String?): List<SavedTranslation> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val MAX_ENTRIES = 200
    }
}
