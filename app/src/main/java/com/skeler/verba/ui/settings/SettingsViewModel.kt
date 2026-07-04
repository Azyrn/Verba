package com.skeler.verba.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.verba.data.CredentialCheck
import com.skeler.verba.data.KeyValidator
import com.skeler.verba.data.MlKitModelManager
import com.skeler.verba.data.OfflineLanguage
import com.skeler.verba.data.SettingsRepository
import com.skeler.verba.model.Provider
import com.skeler.verba.model.ThemeMode
import com.skeler.verba.model.VerbaModel
import com.skeler.verba.model.VerbaModels
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where a provider's credential row is in its test lifecycle. */
sealed interface KeyRowState {
    data object Idle : KeyRowState
    data object Testing : KeyRowState
    data class Failed(val check: CredentialCheck) : KeyRowState
}

/** Whether an offline language model is on disk, being fetched/removed, or absent. */
enum class DownloadState { Absent, Busy, Present }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val validator: KeyValidator,
    private val modelManager: MlKitModelManager,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val model: StateFlow<VerbaModel> = settings.model
        .stateIn(viewModelScope, SharingStarted.Eagerly, VerbaModels.default)

    val apiKeys: StateFlow<Map<Provider, String>> = settings.apiKeys
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Model ids the user typed by hand, per provider. */
    val customModels: StateFlow<Map<Provider, String>> = settings.customModels
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /**
     * The model list as the user sees it: free tier, the presets their keys
     * unlock, then any model ids they typed in for an unlocked provider.
     */
    val models: StateFlow<List<VerbaModel>> = combine(
        settings.apiKeys,
        settings.customModels,
    ) { keys, custom -> VerbaModels.available(keys.keys, custom) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, VerbaModels.available(emptySet()))

    private val _keyRows = MutableStateFlow<Map<Provider, KeyRowState>>(emptyMap())
    val keyRows: StateFlow<Map<Provider, KeyRowState>> = _keyRows.asStateFlow()

    /** Every language the offline engine can translate, in picker order. */
    val offlineLanguages: List<OfflineLanguage> = modelManager.supported

    /** Per-tag download state for the offline language list. */
    private val _downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadState>> = _downloads.asStateFlow()

    init {
        refreshDownloads()
    }

    /** Reconciles the list against what's actually on disk (e.g. after a restart). */
    private fun refreshDownloads() {
        viewModelScope.launch {
            val present = modelManager.downloadedTags()
            _downloads.value = offlineLanguages.associate { offline ->
                offline.tag to if (offline.tag in present) DownloadState.Present
                else DownloadState.Absent
            }
        }
    }

    fun downloadLanguage(tag: String) {
        if (_downloads.value[tag] == DownloadState.Busy) return
        _downloads.update { it + (tag to DownloadState.Busy) }
        viewModelScope.launch {
            val ok = modelManager.download(tag)
            _downloads.update {
                it + (tag to if (ok) DownloadState.Present else DownloadState.Absent)
            }
        }
    }

    fun deleteLanguage(tag: String) {
        if (_downloads.value[tag] == DownloadState.Busy) return
        _downloads.update { it + (tag to DownloadState.Busy) }
        viewModelScope.launch {
            val ok = modelManager.delete(tag)
            _downloads.update {
                it + (tag to if (ok) DownloadState.Absent else DownloadState.Present)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setModel(model: VerbaModel) {
        viewModelScope.launch { settings.setModel(model) }
    }

    /**
     * Probes [key] and [model] together with one live request. Only when the
     * provider answers with that exact pair is anything stored — and the pair
     * becomes the active translation model, so one successful test is enough to
     * start using it. Any other verdict leaves storage untouched and surfaces a
     * provider-specific error on the row.
     */
    fun test(provider: Provider, key: String, model: String) {
        val trimmedKey = key.trim()
        val trimmedModel = model.trim()
        if (trimmedKey.isEmpty() || trimmedModel.isEmpty()) return
        if (_keyRows.value[provider] == KeyRowState.Testing) return
        _keyRows.update { it + (provider to KeyRowState.Testing) }
        viewModelScope.launch {
            when (val check = validator.verify(provider, trimmedKey, trimmedModel)) {
                CredentialCheck.VALID -> {
                    settings.setApiKey(provider, trimmedKey)
                    settings.setCustomModel(provider, trimmedModel)
                    settings.setModel(VerbaModel.custom(provider, trimmedModel))
                    _keyRows.update { it + (provider to KeyRowState.Idle) }
                }
                else -> _keyRows.update { it + (provider to KeyRowState.Failed(check)) }
            }
        }
    }

    /** Clears a stale error the moment the user edits either field again. */
    fun dismissKeyError(provider: Provider) {
        if (_keyRows.value[provider] is KeyRowState.Failed) {
            _keyRows.update { it + (provider to KeyRowState.Idle) }
        }
    }

    /** Disconnects a provider: forgets its key and its verified model together. */
    fun removeKey(provider: Provider) {
        viewModelScope.launch {
            settings.clearApiKey(provider)
            settings.clearCustomModel(provider)
        }
        _keyRows.update { it + (provider to KeyRowState.Idle) }
    }
}
