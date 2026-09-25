package com.skeler.verba.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.verba.R
import com.skeler.verba.data.LiveTranscriber
import com.skeler.verba.data.SavedTranslation
import com.skeler.verba.data.SavedTranslationsRepository
import com.skeler.verba.data.SettingsRepository
import com.skeler.verba.data.TranslationOutcome
import com.skeler.verba.data.SpeechPlayer
import com.skeler.verba.data.TranslationRepository
import com.skeler.verba.data.VoiceOutcome
import com.skeler.verba.data.VoiceRecorder
import com.skeler.verba.data.VoiceRepository
import com.skeler.verba.model.DictationMode
import com.skeler.verba.model.Language
import com.skeler.verba.model.LanguagePair
import com.skeler.verba.model.LanguageSide
import com.skeler.verba.model.TranslationError
import com.skeler.verba.model.VerbaModel
import com.skeler.verba.model.VerbaModels
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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

/** The mic button: idle, listening, or waiting on the transcript. */
enum class VoiceInputState { Idle, Recording, Transcribing }

/** Read-aloud for [text]: fetching its audio, or playing it. */
data class SpeechState(val text: String, val playing: Boolean)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TranslationRepository,
    private val settings: SettingsRepository,
    private val savedTranslations: SavedTranslationsRepository,
    private val voice: VoiceRepository,
    private val recorder: VoiceRecorder,
    private val live: LiveTranscriber,
    private val player: SpeechPlayer,
) : ViewModel() {

    private val dictationMode: StateFlow<DictationMode> = settings.dictationMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, DictationMode.default)

    /** True while live dictation is writing into the input; translation waits for the last word. */
    private val liveDictating = MutableStateFlow(false)

    /** The input as it was when live dictation began; live words go after it. */
    private var liveBase = ""

    private val _voiceInput = MutableStateFlow(VoiceInputState.Idle)
    val voiceInput: StateFlow<VoiceInputState> = _voiceInput.asStateFlow()

    /** Null when nothing is being read aloud. */
    private val _speech = MutableStateFlow<SpeechState?>(null)
    val speech: StateFlow<SpeechState?> = _speech.asStateFlow()

    /** A short line under the input when a voice action fails; clears itself. */
    private val _voiceNotice = MutableStateFlow<Int?>(null)
    val voiceNotice: StateFlow<Int?> = _voiceNotice.asStateFlow()
    private var noticeJob: Job? = null
    private var speechJob: Job? = null

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
        val paused: Boolean,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val translation: StateFlow<TranslationUiState> = combine(
        _input,
        settings.languagePair,
        settings.model,
        retryTicker,
        liveDictating,
    ) { text, pair, model, attempt, paused ->
        TranslationRequest(text.trim(), pair, model, attempt, paused)
    }
        .distinctUntilChanged()
        .transformLatest { request ->
            // Mid-dictation text changes every half second; the screen keeps
            // what it has until the transcript is complete.
            if (request.paused) return@transformLatest
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

    init {
        // Read-aloud belongs to the translation on screen: once that's cleared,
        // edited or replaced, its stop button is gone, so the voice stops too.
        viewModelScope.launch {
            translation.collect { state ->
                val reading = _speech.value ?: return@collect
                if ((state as? TranslationUiState.Success)?.text != reading.text) stopSpeaking()
            }
        }
    }

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

    /** Call only once RECORD_AUDIO is granted. */
    fun startRecording() {
        if (_voiceInput.value != VoiceInputState.Idle) return
        stopSpeaking()
        val started = when (dictationMode.value) {
            DictationMode.BATCH -> recorder.start(onLimit = ::stopRecording)
            DictationMode.LIVE -> startLive()
        }
        if (started) {
            _voiceInput.value = VoiceInputState.Recording
        } else {
            liveDictating.value = false
            notify(R.string.voice_mic_failed)
        }
    }

    /** Words go into the input as they're recognised, after any text already there. */
    private fun startLive(): Boolean {
        val before = _input.value.trimEnd()
        liveBase = before
        liveDictating.value = true
        return live.start(
            language = pair.value.source,
            onText = { text -> _input.value = joinInput(before, text) },
            onLimit = ::stopRecording,
            onError = {
                live.cancel()
                liveDictating.value = false
                _voiceInput.value = VoiceInputState.Idle
                notify(R.string.voice_transcribe_failed)
            },
        )
    }

    /** Stops listening and puts the transcript into the input, after any text already there. */
    fun stopRecording() {
        if (_voiceInput.value != VoiceInputState.Recording) return
        if (liveDictating.value) return stopLive()
        val audio = recorder.stop()
        if (audio == null) {
            _voiceInput.value = VoiceInputState.Idle
            notify(R.string.voice_heard_nothing)
            return
        }
        _voiceInput.value = VoiceInputState.Transcribing
        viewModelScope.launch {
            when (val outcome = voice.transcribe(audio, pair.value.source)) {
                is VoiceOutcome.Success -> _input.value = joinInput(_input.value, outcome.value)
                is VoiceOutcome.Failure -> notify(
                    if (outcome.error == TranslationError.EMPTY_RESPONSE) R.string.voice_heard_nothing
                    else R.string.voice_transcribe_failed,
                )
            }
            _voiceInput.value = VoiceInputState.Idle
        }
    }

    private fun stopLive() {
        _voiceInput.value = VoiceInputState.Transcribing
        // What's in the input now is the text from before plus the live words.
        val shown = _input.value
        viewModelScope.launch {
            when (val outcome = live.finish()) {
                is VoiceOutcome.Success -> _input.value = joinInput(liveBase, outcome.value)
                is VoiceOutcome.Failure -> {
                    _input.value = shown
                    notify(
                        if (outcome.error == TranslationError.EMPTY_RESPONSE) R.string.voice_heard_nothing
                        else R.string.voice_transcribe_failed,
                    )
                }
            }
            liveDictating.value = false
            _voiceInput.value = VoiceInputState.Idle
        }
    }

    private fun joinInput(before: String, words: String): String {
        val current = before.trimEnd()
        return if (current.isEmpty()) words else "$current $words"
    }

    fun onMicPermissionDenied() = notify(R.string.voice_mic_denied)

    /** Reads [text] aloud, or stops if it's the text already being read. */
    fun toggleSpeak(text: String) {
        if (_speech.value?.text == text) {
            stopSpeaking()
            return
        }
        stopSpeaking()
        _speech.value = SpeechState(text, playing = false)
        speechJob = viewModelScope.launch {
            val selected = settings.voice.first()
            val speed = settings.speechSpeed.first()
            val outcome = voice.speak(text, selected, speed, pair.value.target) { pcm ->
                player.play(pcm) { _speech.value = SpeechState(text, playing = true) }
            }
            if (_speech.value?.text == text) _speech.value = null
            if (outcome is VoiceOutcome.Failure) notify(R.string.voice_speak_failed)
        }
    }

    private fun stopSpeaking() {
        speechJob?.cancel()
        player.stop()
        _speech.value = null
    }

    private fun notify(@StringRes message: Int) {
        noticeJob?.cancel()
        _voiceNotice.value = message
        noticeJob = viewModelScope.launch {
            delay(NOTICE_MILLIS)
            _voiceNotice.value = null
        }
    }

    override fun onCleared() {
        recorder.release()
        live.cancel()
        player.stop()
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 400L
        const val NOTICE_MILLIS = 3_000L
    }
}
