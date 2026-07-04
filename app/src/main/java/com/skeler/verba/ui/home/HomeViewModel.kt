package com.skeler.verba.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.verba.data.SavedTranslation
import com.skeler.verba.data.SavedTranslationsRepository
import com.skeler.verba.data.SettingsRepository
import com.skeler.verba.data.TranslationOutcome
import com.skeler.verba.data.TranslationRepository
import com.skeler.verba.model.Language
import com.skeler.verba.model.LanguagePair
import com.skeler.verba.model.LanguageSide
import com.skeler.verba.model.TranslationError
import com.skeler.verba.model.VerbaModel
import com.skeler.verba.model.VerbaModels
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/** What the result pane is showing. Previous text is kept so a reload never blanks the screen. */
sealed interface TranslationUiState {
    data object Empty : TranslationUiState
    data class Loading(val previous: String?) : TranslationUiState
    data class Success(val text: String) : TranslationUiState
    data class Error(val error: TranslationError, val previous: String?) : TranslationUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TranslationRepository,
    private val settings: SettingsRepository,
    private val savedTranslations: SavedTranslationsRepository,
) : ViewModel() {

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    val pair: StateFlow<LanguagePair> = settings.languagePair
        .stateIn(viewModelScope, SharingStarted.Eagerly, LanguagePair.Default)

    val model: StateFlow<VerbaModel> = settings.model
        .stateIn(viewModelScope, SharingStarted.Eagerly, VerbaModels.default)

    private val retryTicker = MutableStateFlow(0)

    /** Last successful translation, kept visible under loading and error states. */
    private var lastTranslation: String? = null

    private data class TranslationRequest(
        val text: String,
        val pair: LanguagePair,
        val model: VerbaModel,
        val attempt: Int,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val translation: StateFlow<TranslationUiState> = combine(
        _input,
        settings.languagePair,
        settings.model,
        retryTicker,
    ) { text, pair, model, attempt ->
        TranslationRequest(text.trim(), pair, model, attempt)
    }
        .distinctUntilChanged()
        .transformLatest { request ->
            if (request.text.isEmpty()) {
                lastTranslation = null
                emit(TranslationUiState.Empty)
                return@transformLatest
            }
            emit(TranslationUiState.Loading(lastTranslation))
            // Debounce: typing cancels the pending request before it fires.
            delay(DEBOUNCE_MILLIS)
            when (
                val outcome = repository.translate(
                    text = request.text,
                    source = request.pair.source,
                    target = request.pair.target,
                    model = request.model,
                )
            ) {
                is TranslationOutcome.Success -> {
                    lastTranslation = outcome.text
                    emit(TranslationUiState.Success(outcome.text))
                }
                is TranslationOutcome.Failure -> {
                    emit(TranslationUiState.Error(outcome.error, lastTranslation))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TranslationUiState.Empty)

    /** Whether the translation on screen is already in the saved list. */
    val isCurrentSaved: StateFlow<Boolean> = combine(
        translation,
        savedTranslations.saved,
    ) { state, saved ->
        val entry = (state as? TranslationUiState.Success)?.let(::entryFor)
        entry != null && saved.any { it.matches(entry) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleSave() {
        val state = translation.value as? TranslationUiState.Success ?: return
        val entry = entryFor(state)
        viewModelScope.launch { savedTranslations.toggle(entry) }
    }

    private fun entryFor(state: TranslationUiState.Success) = SavedTranslation(
        sourceText = _input.value.trim(),
        translatedText = state.text,
        sourceCode = pair.value.source.code,
        targetCode = pair.value.target.code,
        savedAtEpochMs = System.currentTimeMillis(),
    )

    fun onInputChange(text: String) {
        _input.value = text
    }

    fun clearInput() {
        _input.value = ""
    }

    fun retry() {
        retryTicker.value += 1
    }

    /** Swapping through "detect" is meaningless — the chip disables the gesture too. */
    fun swapLanguages() {
        val current = pair.value
        if (current.source.isAuto) return
        viewModelScope.launch { settings.setLanguagePair(current.swapped()) }
    }

    fun selectLanguage(side: LanguageSide, language: Language) {
        val current = pair.value
        val updated = when (side) {
            LanguageSide.SOURCE ->
                if (language == current.target) current.swapped()
                else current.copy(source = language)
            LanguageSide.TARGET ->
                if (language == current.source) current.swapped()
                else current.copy(target = language)
        }
        viewModelScope.launch { settings.setLanguagePair(updated) }
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 550L
    }
}
