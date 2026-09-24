package com.skeler.verba.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skeler.verba.BuildConfig
import com.skeler.verba.R
import com.skeler.verba.data.OfflineLanguage
import com.skeler.verba.model.ThemeMode
import com.skeler.verba.model.Voice
import com.skeler.verba.model.Voices
import com.skeler.verba.ui.axisEnter
import com.skeler.verba.ui.axisExit
import com.skeler.verba.ui.theme.VerbaIcons

/** The settings hub, plus the one detail screen it can drill into at a time. */
private sealed interface SettingsRoute {
    data object Hub : SettingsRoute
    data object Theme : SettingsRoute
    data object Model : SettingsRoute
    data object Offline : SettingsRoute
    data object Voice : SettingsRoute
}

private val SettingsRouteSaver = Saver<SettingsRoute, String>(
    save = { route ->
        when (route) {
            SettingsRoute.Hub -> "hub"
            SettingsRoute.Theme -> "theme"
            SettingsRoute.Model -> "model"
            SettingsRoute.Offline -> "offline"
            SettingsRoute.Voice -> "voice"
        }
    },
    restore = { value ->
        when (value) {
            "theme" -> SettingsRoute.Theme
            "model" -> SettingsRoute.Model
            "offline" -> SettingsRoute.Offline
            "voice" -> SettingsRoute.Voice
            else -> SettingsRoute.Hub
        }
    },
)

/**
 * Settings reads as a small app of its own: a hub of category rows —
 * Theme, Model, Voice, Offline languages — each opening into its own
 * screen with the same shared-axis push VerbaApp uses between top-level
 * screens, so drilling in feels continuous with the rest of the app rather
 * than a different navigation idiom bolted on. Only the hub carries About and
 * the version footer; a detail screen is nothing but its one concern.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val model by viewModel.model.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val voice by viewModel.voice.collectAsStateWithLifecycle()

    var route by rememberSaveable(stateSaver = SettingsRouteSaver) {
        mutableStateOf<SettingsRoute>(SettingsRoute.Hub)
    }
    // A detail screen's back goes up to the hub, not out of Settings entirely;
    // this handler only exists while it's needed, so the hub still falls
    // through to VerbaApp's own back handler.
    BackHandler(enabled = route != SettingsRoute.Hub) { route = SettingsRoute.Hub }

    val offlineTotal = viewModel.offlineLanguages.size
    val offlineDownloaded = viewModel.offlineLanguages.count {
        downloads[it.tag] == DownloadState.Present
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                val forward = targetState != SettingsRoute.Hub
                axisEnter(fromLeading = !forward) togetherWith axisExit(toLeading = forward)
            },
            label = "settingsRoute",
        ) { target ->
            when (target) {
                SettingsRoute.Hub -> SettingsHub(
                    onBack = onBack,
                    themeSummary = themeMode.copy().first,
                    modelSummary = model.name,
                    voiceSummary = voice.name,
                    offlineSummary = if (offlineDownloaded == 0) {
                        stringResource(R.string.settings_summary_offline_none, offlineTotal)
                    } else {
                        stringResource(R.string.settings_summary_offline, offlineDownloaded, offlineTotal)
                    },
                    onOpenTheme = { route = SettingsRoute.Theme },
                    onOpenModel = { route = SettingsRoute.Model },
                    onOpenVoice = { route = SettingsRoute.Voice },
                    onOpenOffline = { route = SettingsRoute.Offline },
                )

                SettingsRoute.Theme -> ThemeSettingsScreen(
                    onBack = { route = SettingsRoute.Hub },
                    themeMode = themeMode,
                    onSelect = viewModel::setThemeMode,
                )

                SettingsRoute.Model -> ModelSettingsScreen(
                    onBack = { route = SettingsRoute.Hub },
                    models = viewModel.models,
                    selected = model,
                    onSelect = viewModel::setModel,
                )

                SettingsRoute.Voice -> VoiceSettingsScreen(
                    onBack = { route = SettingsRoute.Hub },
                    voices = viewModel.voices,
                    selected = voice,
                    onSelect = viewModel::setVoice,
                )

                SettingsRoute.Offline -> OfflineSettingsScreen(
                    onBack = { route = SettingsRoute.Hub },
                    languages = viewModel.offlineLanguages,
                    downloads = downloads,
                    onDownload = viewModel::downloadLanguage,
                    onDelete = viewModel::deleteLanguage,
                )
            }
        }
    }
}

/** The header every Settings screen shares: a back arrow and a title. */
@Composable
private fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * The hub: three category rows, each a quiet lapis-tinted glyph, a title and
 * the live state of that category, and a chevron. About and the version sit
 * below, never behind a tap — they're links and a footnote, not a setting.
 */
@Composable
private fun SettingsHub(
    onBack: () -> Unit,
    themeSummary: String,
    modelSummary: String,
    voiceSummary: String,
    offlineSummary: String,
    onOpenTheme: () -> Unit,
    onOpenModel: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenOffline: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        SettingsTopBar(title = stringResource(R.string.settings_title), onBack = onBack)
        Spacer(Modifier.height(20.dp))

        SettingsCard {
            HubRow(
                icon = VerbaIcons.Brightness,
                title = stringResource(R.string.settings_section_theme),
                value = themeSummary,
                onClick = onOpenTheme,
            )
            RowDivider()
            HubRow(
                icon = VerbaIcons.Sparkle,
                title = stringResource(R.string.settings_section_model),
                value = modelSummary,
                onClick = onOpenModel,
            )
            RowDivider()
            HubRow(
                icon = VerbaIcons.VolumeUp,
                title = stringResource(R.string.settings_section_voice),
                value = voiceSummary,
                onClick = onOpenVoice,
            )
            RowDivider()
            HubRow(
                icon = VerbaIcons.Download,
                title = stringResource(R.string.settings_section_offline),
                value = offlineSummary,
                onClick = onOpenOffline,
            )
        }

        Spacer(Modifier.height(24.dp))

        val context = LocalContext.current
        fun open(url: String) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        SectionLabel(stringResource(R.string.settings_section_about))
        SettingsCard {
            AboutRow(
                icon = painterResource(R.drawable.ic_github),
                title = stringResource(R.string.about_source_title),
                subtitle = stringResource(R.string.about_source_subtitle),
                onClick = { open(context.getString(R.string.about_source_url)) },
            )
            RowDivider()
            AboutRow(
                icon = painterResource(R.drawable.ic_telegram),
                title = stringResource(R.string.about_telegram_title),
                subtitle = stringResource(R.string.about_telegram_subtitle),
                onClick = { open(context.getString(R.string.about_telegram_url)) },
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(
                R.string.about_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ThemeSettingsScreen(
    onBack: () -> Unit,
    themeMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        SettingsTopBar(title = stringResource(R.string.settings_section_theme), onBack = onBack)
        Spacer(Modifier.height(20.dp))
        SettingsCard {
            ThemeMode.entries.forEachIndexed { index, mode ->
                if (index > 0) RowDivider()
                val (title, subtitle) = mode.copy()
                ChoiceRow(
                    title = title,
                    subtitle = subtitle,
                    selected = themeMode == mode,
                    onClick = { onSelect(mode) },
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModelSettingsScreen(
    onBack: () -> Unit,
    models: List<com.skeler.verba.model.VerbaModel>,
    selected: com.skeler.verba.model.VerbaModel,
    onSelect: (com.skeler.verba.model.VerbaModel) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        SettingsTopBar(title = stringResource(R.string.settings_section_model), onBack = onBack)
        Spacer(Modifier.height(20.dp))
        SettingsCard {
            models.forEachIndexed { index, candidate ->
                if (index > 0) RowDivider()
                ChoiceRow(
                    title = candidate.name,
                    subtitle = stringResource(candidate.description),
                    selected = selected.id == candidate.id,
                    onClick = { onSelect(candidate) },
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** Read-aloud voices; every one speaks every language, so it's purely a matter of taste. */
@Composable
private fun VoiceSettingsScreen(
    onBack: () -> Unit,
    voices: List<Voice>,
    selected: Voice,
    onSelect: (Voice) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        SettingsTopBar(title = stringResource(R.string.settings_section_voice), onBack = onBack)
        Spacer(Modifier.height(20.dp))
        SettingsCard {
            voices.forEachIndexed { index, candidate ->
                if (index > 0) RowDivider()
                val kind = stringResource(
                    if (candidate.female) R.string.voice_female else R.string.voice_male,
                )
                ChoiceRow(
                    title = candidate.name,
                    subtitle = if (candidate == Voices.default) {
                        stringResource(R.string.voice_default_suffix, kind)
                    } else {
                        kind
                    },
                    selected = selected.id == candidate.id,
                    onClick = { onSelect(candidate) },
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OfflineSettingsScreen(
    onBack: () -> Unit,
    languages: List<OfflineLanguage>,
    downloads: Map<String, DownloadState>,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        SettingsTopBar(title = stringResource(R.string.settings_section_offline), onBack = onBack)
        Spacer(Modifier.height(20.dp))
        SettingsCard {
            languages.forEachIndexed { index, offline ->
                if (index > 0) RowDivider()
                OfflineLanguageRow(
                    offline = offline,
                    state = downloads[offline.tag] ?: DownloadState.Absent,
                    onDownload = { onDownload(offline.tag) },
                    onDelete = { onDelete(offline.tag) },
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * One hub category row: a soft lapis-tinted glyph tile on the left — the
 * house style —
 * the category name, its live value beneath, and a static disclosure chevron.
 */
@Composable
private fun HubRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(12.dp))
        Icon(
            imageVector = VerbaIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * One ABOUT row: a brand glyph in a soft rounded tile, a title and subtitle, and
 * an open-in-new mark on the right. The whole row opens its link on tap.
 */
@Composable
private fun AboutRow(
    icon: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(12.dp))
        Icon(
            painter = painterResource(R.drawable.ic_open_external),
            contentDescription = stringResource(R.string.about_open_link),
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * One language the offline engine can carry, with its model's state on the
 * right: a Download affordance when absent, a spinner mid-transfer, a check and
 * a remove button once it's on disk. The native name leads — the same order the
 * language picker uses — with the English exonym beneath it when it differs.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OfflineLanguageRow(
    offline: OfflineLanguage,
    state: DownloadState,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val present = state == DownloadState.Present
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = offline.language.nativeName,
                style = MaterialTheme.typography.titleMedium,
                color = if (present) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            if (offline.language.nativeName != offline.language.name) {
                Text(
                    text = offline.language.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(16.dp))
        when (state) {
            DownloadState.Busy -> LoadingIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            DownloadState.Present -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.offline_remove),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            DownloadState.Absent -> Text(
                text = stringResource(R.string.offline_download),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDownload)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** The title/subtitle copy for a theme mode, kept beside its enum for one glance. */
@Composable
private fun ThemeMode.copy(): Pair<String, String> = when (this) {
    ThemeMode.SYSTEM ->
        stringResource(R.string.theme_system) to stringResource(R.string.theme_system_subtitle)
    ThemeMode.LIGHT ->
        stringResource(R.string.theme_light) to stringResource(R.string.theme_light_subtitle)
    ThemeMode.DARK ->
        stringResource(R.string.theme_dark) to stringResource(R.string.theme_dark_subtitle)
    ThemeMode.TRUE_BLACK ->
        stringResource(R.string.theme_black) to stringResource(R.string.theme_black_subtitle)
}

/**
 * The one container shape in Settings: a soft-cornered surface with a hairline
 * border, the same card the language chip and search field wear. Rows stack
 * inside it, hairline-ruled apart by [RowDivider].
 */
@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(22.dp),
            ),
        content = content,
    )
}

/** A hairline between rows, inset to start under the text, not the card edge. */
@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}

/**
 * One selectable row of a settings card. The active row lifts onto a faint lapis
 * wash, brightens its title to lapis, and grows a check on the right — the whole
 * change crossfades in on the theme's effects spec, no bounce, no layout jump.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val motion = MaterialTheme.motionScheme
    val wash by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        else Color.Transparent,
        animationSpec = motion.defaultEffectsSpec(),
        label = "rowWash",
    )
    val titleColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = motion.defaultEffectsSpec(),
        label = "rowTitle",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .background(wash)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(12.dp))
        // A fixed slot so the row never reflows; the check scales and fades in.
        val checkScale by animateFloatAsState(
            targetValue = if (selected) 1f else 0.5f,
            animationSpec = motion.fastSpatialSpec(),
            label = "checkScale",
        )
        val checkAlpha by animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            animationSpec = motion.fastEffectsSpec(),
            label = "checkAlpha",
        )
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = checkScale
                    scaleY = checkScale
                    alpha = checkAlpha
                },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 4.dp, bottom = 10.dp),
    )
}
