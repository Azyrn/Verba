package com.skeler.verba.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skeler.verba.model.LanguagePair
import com.skeler.verba.model.Languages
import com.skeler.verba.model.Provider
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
        val ModelProvider = stringPreferencesKey("model_provider")
        val SourceLanguage = stringPreferencesKey("source_language")
        val TargetLanguage = stringPreferencesKey("target_language")

        fun apiKey(provider: Provider) =
            stringPreferencesKey("api_key_${provider.name.lowercase()}")

        fun customModel(provider: Provider) =
            stringPreferencesKey("custom_model_${provider.name.lowercase()}")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { ThemeMode.fromName(it[Keys.Theme]) }
        .distinctUntilChanged()

    /**
     * The selected model, falling back to the default if its provider's key
     * has since been removed — a BYOK model without a key can never answer —
     * or if it was a hand-typed model the user has since cleared. Anything in
     * the bundled free tier ([VerbaModels.all]) is always unlocked, whichever
     * provider backs it — that's what makes it free — so only a preset from
     * [VerbaModels.byok] or a hand-typed id needs a personal key to stick.
     */
    val model: Flow<VerbaModel> = dataStore.data
        .map { preferences ->
            val id = preferences[Keys.Model].orEmpty()
            val selected = VerbaModels.preset(id)
                ?: preferences.customSelection(id)
                ?: VerbaModels.default
            val unlocked = VerbaModels.all.any { it.id == selected.id } ||
                !preferences[Keys.apiKey(selected.provider)].isNullOrBlank()
            if (unlocked) selected else VerbaModels.default
        }
        .distinctUntilChanged()

    /**
     * Rebuilds a hand-typed selection from the stored provider, but only while
     * that provider still holds exactly this model id — a stale selection whose
     * custom model was edited or cleared resolves to null.
     */
    private fun Preferences.customSelection(id: String): VerbaModel? {
        if (id.isBlank()) return null
        val provider = this[Keys.ModelProvider]
            ?.let { name -> Provider.entries.firstOrNull { it.name == name } }
            ?: return null
        return if (this[Keys.customModel(provider)] == id) {
            VerbaModel.custom(provider, id)
        } else {
            null
        }
    }

    /** Personal model ids the user typed per provider; blanks are absent. */
    val customModels: Flow<Map<Provider, String>> = dataStore.data
        .map { preferences ->
            Provider.entries.mapNotNull { provider ->
                preferences[Keys.customModel(provider)]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { provider to it }
            }.toMap()
        }
        .distinctUntilChanged()

    /** Personal keys by provider; providers without a key are absent. */
    val apiKeys: Flow<Map<Provider, String>> = dataStore.data
        .map { preferences ->
            Provider.entries.mapNotNull { provider ->
                preferences[Keys.apiKey(provider)]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { provider to it }
            }.toMap()
        }
        .distinctUntilChanged()

    fun apiKey(provider: Provider): Flow<String?> = dataStore.data
        .map { it[Keys.apiKey(provider)]?.takeIf(String::isNotBlank) }
        .distinctUntilChanged()

    suspend fun setApiKey(provider: Provider, key: String) {
        dataStore.edit { it[Keys.apiKey(provider)] = key.trim() }
    }

    suspend fun clearApiKey(provider: Provider) {
        dataStore.edit { it.remove(Keys.apiKey(provider)) }
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
        dataStore.edit {
            it[Keys.Model] = model.id
            it[Keys.ModelProvider] = model.provider.name
        }
    }

    suspend fun setCustomModel(provider: Provider, id: String) {
        dataStore.edit { it[Keys.customModel(provider)] = id.trim() }
    }

    suspend fun clearCustomModel(provider: Provider) {
        dataStore.edit { it.remove(Keys.customModel(provider)) }
    }

    suspend fun setLanguagePair(pair: LanguagePair) {
        dataStore.edit {
            it[Keys.SourceLanguage] = pair.source.code
            it[Keys.TargetLanguage] = pair.target.code
        }
    }
}
