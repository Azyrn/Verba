package com.skeler.verba.ui.home

import android.content.ClipData
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.skeler.verba.R
import com.skeler.verba.model.LanguagePair
import com.skeler.verba.model.TranslationError
import com.skeler.verba.model.VerbaModel
import com.skeler.verba.ui.theme.VerbaIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The dominant region of the screen. Every state — answer, waiting, nothing,
 * failure — is typeset with the same care, because each is the product at
 * that moment.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ResultPane(
    translation: TranslationUiState,
    pair: LanguagePair,
    model: VerbaModel,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.motionScheme

    AnimatedContent(
        targetState = translation,
        transitionSpec = {
            // Arrivals rise a step and settle; departures just dissolve.
            (slideInVertically(motion.fastSpatialSpec<IntOffset>()) { it / 14 } +
                fadeIn(motion.fastEffectsSpec()))
                .togetherWith(fadeOut(motion.fastEffectsSpec()))
        },
        contentKey = { state ->
            when (state) {
                is TranslationUiState.Success -> "success:${state.text}"
                is TranslationUiState.Error -> "error:${state.error}"
                is TranslationUiState.Loading -> "loading"
                TranslationUiState.Empty -> "empty"
            }
        },
        label = "result",
        modifier = modifier,
    ) { state ->
        when (state) {
            TranslationUiState.Empty -> EmptyState(pair)
            is TranslationUiState.Loading -> LoadingState(state, pair, model)
            is TranslationUiState.Success -> Translation(
                text = state.text,
                pair = pair,
                isSaved = isSaved,
                onToggleSave = onToggleSave,
            )
            is TranslationUiState.Error -> ErrorState(
                error = state.error,
                model = model,
                onRetry = onRetry,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun Translation(
    text: String,
    pair: LanguagePair,
    dimmed: Boolean = false,
    isSaved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (dimmed) 0.38f else 1f)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = pair.target.name.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        SelectionContainer {
            Text(
                text = text,
                style = resultStyle(text.length),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (!dimmed && onToggleSave != null) {
            Spacer(Modifier.height(16.dp))
            ResultActions(text = text, isSaved = isSaved, onToggleSave = onToggleSave)
        }
        Spacer(Modifier.height(32.dp))
    }
}

private enum class ActionNotice(val label: Int) {
    COPIED(R.string.result_copied),
    SAVED(R.string.result_saved),
    REMOVED(R.string.result_removed),
}

/**
 * Copy and save, weighted alike: two quiet glyphs under the answer, each
 * acknowledged by a small lapis word in place of any toast.
 */
@Composable
private fun ResultActions(
    text: String,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    var notice by remember { mutableStateOf<ActionNotice?>(null) }
    var lastNotice by remember { mutableStateOf(ActionNotice.COPIED) }
    var noticeTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(noticeTick) {
        if (notice == null) return@LaunchedEffect
        delay(1800)
        notice = null
    }
    fun announce(value: ActionNotice) {
        notice = value
        lastNotice = value
        noticeTick++
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipData.newPlainText("Verba translation", text).toClipEntry(),
                    )
                }
                announce(ActionNotice.COPIED)
            },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = VerbaIcons.Copy,
                contentDescription = stringResource(R.string.action_copy),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(
            onClick = {
                announce(if (isSaved) ActionNotice.REMOVED else ActionNotice.SAVED)
                onToggleSave()
            },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = if (isSaved) VerbaIcons.BookmarkFilled else VerbaIcons.Bookmark,
                contentDescription = stringResource(R.string.action_save),
                tint = if (isSaved) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        AnimatedVisibility(visible = notice != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = stringResource(lastNotice.label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Book-scale type: short phrases sit at title size, running text reads like body copy. */
@Composable
private fun resultStyle(length: Int): TextStyle = when {
    length <= 70 -> MaterialTheme.typography.headlineSmall
    else -> MaterialTheme.typography.bodyLarge
}

@Composable
private fun EmptyState(pair: LanguagePair) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.result_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.result_empty_body, pair.target.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp),
        )
    }
}

@Composable
private fun LoadingState(
    state: TranslationUiState.Loading,
    pair: LanguagePair,
    model: VerbaModel,
) {
    val previous = state.previous
    if (previous != null) {
        // Keep the last answer readable but clearly stale while the next one lands.
        Translation(text = previous, pair = pair, dimmed = true)
    } else {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.result_loading_caption, model.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: TranslationError,
    model: VerbaModel,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val title = when (error) {
        TranslationError.MISSING_KEY -> stringResource(R.string.error_key_missing_title)
        TranslationError.INVALID_KEY -> stringResource(R.string.error_key_invalid_title)
        TranslationError.NETWORK -> stringResource(R.string.error_offline_title)
        TranslationError.RATE_LIMITED -> stringResource(R.string.error_rate_title)
        TranslationError.MODEL_UNAVAILABLE -> stringResource(R.string.error_model_title, model.name)
        TranslationError.EMPTY_RESPONSE -> stringResource(R.string.error_empty_title)
        TranslationError.LANGUAGE_UNSUPPORTED -> stringResource(R.string.error_lang_title)
        TranslationError.UNKNOWN -> stringResource(R.string.error_unknown_title)
    }
    val body = when (error) {
        TranslationError.MISSING_KEY -> stringResource(R.string.error_key_missing_body)
        TranslationError.INVALID_KEY -> stringResource(R.string.error_key_invalid_body)
        TranslationError.NETWORK -> stringResource(R.string.error_offline_body)
        TranslationError.RATE_LIMITED -> stringResource(R.string.error_rate_body)
        TranslationError.MODEL_UNAVAILABLE -> stringResource(R.string.error_model_body)
        TranslationError.EMPTY_RESPONSE -> stringResource(R.string.error_empty_body)
        TranslationError.LANGUAGE_UNSUPPORTED -> stringResource(R.string.error_lang_body)
        TranslationError.UNKNOWN -> stringResource(R.string.error_unknown_body)
    }
    val retryable = error != TranslationError.MISSING_KEY &&
        error != TranslationError.INVALID_KEY &&
        error != TranslationError.LANGUAGE_UNSUPPORTED

    Column(Modifier.fillMaxSize()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 340.dp),
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (retryable) {
                FilledTonalButton(onClick = onRetry) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            if (error == TranslationError.MODEL_UNAVAILABLE ||
                error == TranslationError.RATE_LIMITED ||
                error == TranslationError.LANGUAGE_UNSUPPORTED
            ) {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.action_switch_model))
                }
            }
        }
    }
}
