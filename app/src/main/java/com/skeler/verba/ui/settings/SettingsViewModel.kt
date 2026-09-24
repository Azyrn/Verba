package com.skeler.verba.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.verba.data.MlKitModelManager
import com.skeler.verba.data.OfflineLanguage
import com.skeler.verba.data.SettingsRepository
import com.skeler.verba.model.DictationMode
import com.skeler.verba.model.ThemeMode
import com.skeler.verba.model.VerbaModel
import com.skeler.verba.model.VerbaModels
import com.skeler.verba.model.Voice
import com.skeler.verba.model.Voices
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Whether an offline language model is on disk, being fetched/removed, or absent. */
enum class DownloadState { Absent, Busy, Present }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val modelManager: MlKitModelManager,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val model: StateFlow<VerbaModel> = settings.model
        .stateIn(viewModelScope, SharingStarted.Eagerly, VerbaModels.default)

    val voice: StateFlow<Voice> = settings.voice
        .stateIn(viewModelScope, SharingStarted.Eagerly, Voices.default)

    val voices: List<Voice> = Voices.all

    val dictationMode: StateFlow<DictationMode> = settings.dictationMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, DictationMode.default)

    /** Online and Offline — the only two engines. */
    val models: List<VerbaModel> = VerbaModels.all

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

    fun setVoice(voice: Voice) {
        viewModelScope.launch { settings.setVoice(voice) }
    }

    fun setDictationMode(mode: DictationMode) {
        viewModelScope.launch { settings.setDictationMode(mode) }
    }
}
