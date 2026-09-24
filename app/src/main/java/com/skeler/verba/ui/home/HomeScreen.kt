package com.skeler.verba.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
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
    voiceInput: VoiceInputState,
    voiceNotice: Int?,
    speech: SpeechState?,
    onToggleSave: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onMicDenied: () -> Unit,
    onToggleSpeak: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onClearInput: () -> Unit,
    onSwapLanguages: () -> Unit,
    onRetry: () -> Unit,
    onOpenPicker: (LanguageSide) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSaved: () -> Unit,
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
            IconButton(onClick = onOpenSaved) {
                Icon(
                    imageVector = VerbaIcons.Bookmark,
                    contentDescription = stringResource(R.string.saved_open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp),
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

        Spacer(Modifier.height(10.dp))

        ModelIndicator(model = model, onClick = onOpenSettings)

        Spacer(Modifier.height(18.dp))

        SourceInput(
            input = input,
            isTranslating = translation is TranslationUiState.Loading,
            voiceInput = voiceInput,
            onInputChange = onInputChange,
            onClearInput = onClearInput,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onMicDenied = onMicDenied,
        )

        AnimatedVisibility(visible = voiceNotice != null, enter = fadeIn(), exit = fadeOut()) {
            // Keep the last message through the fade-out rather than blanking it.
            var shown by remember { mutableStateOf(voiceNotice) }
            if (voiceNotice != null) shown = voiceNotice
            shown?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

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
            speech = speech,
            onToggleSave = onToggleSave,
            onToggleSpeak = onToggleSpeak,
            onRetry = onRetry,
            onOpenSettings = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

/**
 * A quiet line naming the engine in charge — Online or Offline. Tapping it
 * opens the settings screen where that can be changed.
 */
@Composable
private fun ModelIndicator(model: VerbaModel, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SourceInput(
    input: String,
    isTranslating: Boolean,
    voiceInput: VoiceInputState,
    onInputChange: (String) -> Unit,
    onClearInput: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onMicDenied: () -> Unit,
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

        MicButton(
            state = voiceInput,
            onStart = onStartRecording,
            onStop = onStopRecording,
            onDenied = onMicDenied,
        )
    }
}

/**
 * Dictation: tap to listen, tap again to stop and transcribe. While listening
 * the glyph turns into a lapis stop square that breathes, so it's plain the
 * mic is live.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MicButton(
    state: VoiceInputState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDenied: () -> Unit,
) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onStart() else onDenied() }

    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        when (state) {
            VoiceInputState.Transcribing -> LoadingIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
            )

            VoiceInputState.Recording -> {
                val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
                    initialValue = 1f,
                    targetValue = 0.45f,
                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                    label = "micPulse",
                )
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                ) {
                    Icon(
                        imageVector = VerbaIcons.Stop,
                        contentDescription = stringResource(R.string.voice_stop),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { alpha = pulse },
                    )
                }
            }

            VoiceInputState.Idle -> IconButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) onStart() else permission.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = VerbaIcons.Mic,
                    contentDescription = stringResource(R.string.voice_record),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
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
