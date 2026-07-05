package com.skeler.verba.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skeler.verba.model.LanguageSide
import com.skeler.verba.ui.home.HomeScreen
import com.skeler.verba.ui.home.HomeViewModel
import com.skeler.verba.ui.picker.LanguagePickerScreen
import com.skeler.verba.ui.saved.SavedScreen
import com.skeler.verba.ui.saved.SavedViewModel
import com.skeler.verba.ui.settings.SettingsScreen
import com.skeler.verba.ui.settings.SettingsViewModel
import kotlin.math.pow

sealed interface Screen {
    data object Home : Screen
    data class Picker(val side: LanguageSide) : Screen
    data object Settings : Screen
    data object Saved : Screen
}

private val ScreenSaver = Saver<Screen, String>(
    save = { screen ->
        when (screen) {
            Screen.Home -> "home"
            Screen.Settings -> "settings"
            Screen.Saved -> "saved"
            is Screen.Picker -> "picker:${screen.side.name}"
        }
    },
    restore = { value ->
        when {
            value == "settings" -> Screen.Settings
            value == "saved" -> Screen.Saved
            value.startsWith("picker:") ->
                Screen.Picker(LanguageSide.valueOf(value.removePrefix("picker:")))
            else -> Screen.Home
        }
    },
)

// Shared-axis X, ported from Scanly: screens travel a fraction of the width
// from the edge with a fade-through — the incoming fade is delayed so the two
// screens never blend into a double exposure. EaseOutExpo lands them softly.
private val EaseOutExpo = Easing { fraction ->
    if (fraction == 1f) 1f else 1f - 2f.pow(-10f * fraction)
}

private const val AXIS_DURATION_MS = 350
private const val FADE_IN_DURATION_MS = 250
private const val FADE_IN_DELAY_MS = 60
private const val FADE_OUT_DURATION_MS = 140
private const val AXIS_DISTANCE_FRACTION = 0.30f

/** How long a screen takes to finish arriving. */
internal const val SHEET_ENTER_MILLIS: Long = AXIS_DURATION_MS.toLong()

private fun axisDistance(fullWidth: Int): Int =
    (fullWidth * AXIS_DISTANCE_FRACTION).toInt()

private fun axisEnter(fromLeading: Boolean): EnterTransition = slideInHorizontally(
    animationSpec = tween(AXIS_DURATION_MS, easing = EaseOutExpo),
    initialOffsetX = { if (fromLeading) -axisDistance(it) else axisDistance(it) },
) + fadeIn(
    animationSpec = tween(FADE_IN_DURATION_MS, delayMillis = FADE_IN_DELAY_MS, easing = LinearOutSlowInEasing),
)

private fun axisExit(toLeading: Boolean): ExitTransition = slideOutHorizontally(
    animationSpec = tween(AXIS_DURATION_MS, easing = EaseOutExpo),
    targetOffsetX = { if (toLeading) -axisDistance(it) else axisDistance(it) },
) + fadeOut(
    animationSpec = tween(FADE_OUT_DURATION_MS, easing = FastOutLinearInEasing),
)

@Composable
fun VerbaApp() {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val savedViewModel: SavedViewModel = hiltViewModel()

    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    val input by homeViewModel.input.collectAsStateWithLifecycle()
    val pair by homeViewModel.pair.collectAsStateWithLifecycle()
    val model by homeViewModel.model.collectAsStateWithLifecycle()
    val translation by homeViewModel.translation.collectAsStateWithLifecycle()
    val isSaved by homeViewModel.isCurrentSaved.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            // Forward: picker/settings arrive from the trailing edge while
            // home recedes toward the leading one. Back mirrors it exactly.
            val forward = targetState != Screen.Home
            axisEnter(fromLeading = !forward) togetherWith axisExit(toLeading = forward)
        },
        label = "screens",
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) { target ->
        when (target) {
            Screen.Home -> HomeScreen(
                input = input,
                pair = pair,
                model = model,
                translation = translation,
                isSaved = isSaved,
                onToggleSave = homeViewModel::toggleSave,
                onInputChange = homeViewModel::onInputChange,
                onClearInput = homeViewModel::clearInput,
                onSwapLanguages = homeViewModel::swapLanguages,
                onRetry = homeViewModel::retry,
                onOpenPicker = { side -> screen = Screen.Picker(side) },
                onOpenSettings = { screen = Screen.Settings },
                onOpenSaved = { screen = Screen.Saved },
            )

            is Screen.Picker -> LanguagePickerScreen(
                initialSide = target.side,
                pair = pair,
                onSelect = { side, language ->
                    homeViewModel.selectLanguage(side, language)
                    screen = Screen.Home
                },
                onClose = { screen = Screen.Home },
            )

            Screen.Settings -> SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { screen = Screen.Home },
            )

            Screen.Saved -> SavedScreen(
                viewModel = savedViewModel,
                onBack = { screen = Screen.Home },
            )
        }
    }
}
