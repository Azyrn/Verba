package com.skeler.verba.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skeler.verba.model.LanguagePair
import com.skeler.verba.model.Languages
import com.skeler.verba.model.ThemeMode
import com.skeler.verba.model.VerbaModel
import com.skeler.verba.model.VerbaModels
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private object Keys {
        val Theme = stringPreferencesKey("theme_mode")
        val Model = stringPreferencesKey("model_id")
        val SourceLanguage = stringPreferencesKey("source_language")
        val TargetLanguage = stringPreferencesKey("target_language")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { ThemeMode.fromName(it[Keys.Theme]) }
        .distinctUntilChanged()

    /** The selected engine; a selection from before Online/Offline resolves to the default. */
    val model: Flow<VerbaModel> = dataStore.data
        .map { VerbaModels.byId(it[Keys.Model]) }
        .distinctUntilChanged()

    /**
     * Earlier versions stored personal API keys (encrypted) and per-provider
     * model ids. Nothing reads them any more, so they're dropped rather than
     * left on disk.
     */
    suspend fun clearLegacyKeys() {
        dataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { it.name.startsWith("api_key_") || it.name.startsWith("custom_model_") ||
                    it.name == "model_provider" }
                .forEach { prefs.remove(it) }
        }
    }

    val languagePair: Flow<LanguagePair> = dataStore.data
        .map { preferences ->
            val source = preferences[Keys.SourceLanguage]
            val target = preferences[Keys.TargetLanguage]
            if (source == null || target == null) {
                LanguagePair.Default
            } else {
                LanguagePair(Languages.byCode(source), Languages.byCode(target))
            }
        }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.Theme] = mode.name }
    }

    suspend fun setModel(model: VerbaModel) {
        dataStore.edit { it[Keys.Model] = model.id }
    }

    suspend fun setLanguagePair(pair: LanguagePair) {
        dataStore.edit {
            it[Keys.SourceLanguage] = pair.source.code
            it[Keys.TargetLanguage] = pair.target.code
        }
    }
}
