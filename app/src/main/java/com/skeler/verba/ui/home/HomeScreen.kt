package com.skeler.verba.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.skeler.verba.R
import com.skeler.verba.model.LanguagePair
import com.skeler.verba.model.LanguageSide
import com.skeler.verba.model.VerbaModel
import com.skeler.verba.ui.theme.VerbaIcons
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The whole product on one screen: the language pair, the text, the answer.
 * The input sits small and quiet at the top; the translation owns the rest.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    input: String,
    pair: LanguagePair,
    model: VerbaModel,
    translation: TranslationUiState,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onInputChange: (String) -> Unit,
    onClearInput: () -> Unit,
    onSwapLanguages: () -> Unit,
    onRetry: () -> Unit,
    onOpenPicker: (LanguageSide) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                LanguagePairChip(
                    pair = pair,
                    onOpenPicker = onOpenPicker,
                    onSwap = onSwapLanguages,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings_open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SourceInput(
            input = input,
            isTranslating = translation is TranslationUiState.Loading,
            onInputChange = onInputChange,
            onClearInput = onClearInput,
        )

        Spacer(Modifier.height(14.dp))

        // A scribe's rule: the short lapis stroke separating question from answer.
        Box(
            Modifier
                .width(44.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )

        Spacer(Modifier.height(20.dp))

        ResultPane(
            translation = translation,
            pair = pair,
            model = model,
            isSaved = isSaved,
            onToggleSave = onToggleSave,
            onRetry = onRetry,
            onOpenSettings = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SourceInput(
    input: String,
    isTranslating: Boolean,
    onInputChange: (String) -> Unit,
    onClearInput: () -> Unit,
) {
    val inputStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            textStyle = inputStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            maxLines = 4,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 28.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (input.isEmpty()) {
                        Text(
                            text = stringResource(R.string.input_hint),
                            style = inputStyle,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    innerTextField()
                }
            },
        )

        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            when {
                isTranslating -> LoadingIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                )

                // The slot holds one quiet affordance: clear when there is
                // text, paste when there isn't.
                input.isNotEmpty() -> androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + scaleIn(initialScale = 0.6f),
                    exit = fadeOut() + scaleOut(targetScale = 0.6f),
                ) {
                    IconButton(onClick = onClearInput, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.input_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                else -> PasteButton(onPaste = onInputChange)
            }
        }
    }
}

@Composable
private fun PasteButton(onPaste: (String) -> Unit) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    IconButton(
        onClick = {
            // The clipboard is only read here, on the tap — reading it any
            // earlier would fire the system's access notice on every open.
            scope.launch {
                val clip = clipboard.getClipEntry()?.clipData ?: return@launch
                val text = (0 until clip.itemCount)
                    .joinToString("\n") { clip.getItemAt(it).coerceToText(context) }
                    .trim()
                if (text.isNotEmpty()) onPaste(text)
            }
        },
        modifier = Modifier.size(28.dp),
    ) {
        Icon(
            imageVector = VerbaIcons.Paste,
            contentDescription = stringResource(R.string.input_paste),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
    }
}
