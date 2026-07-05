package com.skeler.verba.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.verba.data.SavedTranslation
import com.skeler.verba.data.SavedTranslationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val savedTranslations: SavedTranslationsRepository,
) : ViewModel() {

    val saved: StateFlow<List<SavedTranslation>> = savedTranslations.saved
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(entry: SavedTranslation) {
        viewModelScope.launch { savedTranslations.remove(entry) }
    }
}
