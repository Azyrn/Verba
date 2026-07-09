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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skeler.verba.BuildConfig
import com.skeler.verba.R
import com.skeler.verba.data.CredentialCheck
import com.skeler.verba.data.OfflineLanguage
import com.skeler.verba.model.Provider
import com.skeler.verba.model.ThemeMode
import com.skeler.verba.ui.axisEnter
import com.skeler.verba.ui.axisExit
import com.skeler.verba.ui.theme.VerbaIcons

/** The settings hub, plus the one detail screen it can drill into at a time. */
private sealed interface SettingsRoute {
    data object Hub : SettingsRoute
    data object Theme : SettingsRoute
    data object Model : SettingsRoute
    data object Keys : SettingsRoute
    data object Offline : SettingsRoute
}

private val SettingsRouteSaver = Saver<SettingsRoute, String>(
    save = { route ->
        when (route) {
            SettingsRoute.Hub -> "hub"
            SettingsRoute.Theme -> "theme"
            SettingsRoute.Model -> "model"
            SettingsRoute.Keys -> "keys"
            SettingsRoute.Offline -> "offline"
        }
    },
    restore = { value ->
        when (value) {
            "theme" -> SettingsRoute.Theme
            "model" -> SettingsRoute.Model
            "keys" -> SettingsRoute.Keys
            "offline" -> SettingsRoute.Offline
            else -> SettingsRoute.Hub
        }
    },
)

/**
 * Settings reads as a small app of its own: a hub of four category rows —
 * Appearance, Model, API keys, Offline languages — each opening into its own
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
    val models by viewModel.models.collectAsStateWithLifecycle()
    val apiKeys by viewModel.apiKeys.collectAsStateWithLifecycle()
    val customModels by viewModel.customModels.collectAsStateWithLifecycle()
    val keyRows by viewModel.keyRows.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    var route by rememberSaveable(stateSaver = SettingsRouteSaver) {
        mutableStateOf<SettingsRoute>(SettingsRoute.Hub)
    }
    // A detail screen's back goes up to the hub, not out of Settings entirely;
    // this handler only exists while it's needed, so the hub still falls
    // through to VerbaApp's own back handler.
    BackHandler(enabled = route != SettingsRoute.Hub) { route = SettingsRoute.Hub }

    val connectedKeys = Provider.entries.count {
        !it.onDevice && apiKeys[it] != null && customModels[it] != null
    }
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
                    keysSummary = if (connectedKeys == 0) {
                        stringResource(R.string.settings_summary_keys_none)
                    } else {
                        stringResource(R.string.settings_summary_keys, connectedKeys)
                    },
                    offlineSummary = if (offlineDownloaded == 0) {
                        stringResource(R.string.settings_summary_offline_none, offlineTotal)
                    } else {
                        stringResource(R.string.settings_summary_offline, offlineDownloaded, offlineTotal)
                    },
                    onOpenTheme = { route = SettingsRoute.Theme },
                    onOpenModel = { route = SettingsRoute.Model },
                    onOpenKeys = { route = SettingsRoute.Keys },
                    onOpenOffline = { route = SettingsRoute.Offline },
                )

                SettingsRoute.Theme -> ThemeSettingsScreen(
                    onBack = { route = SettingsRoute.Hub },
                    themeMode = themeMode,
                    onSelect = viewModel::setThemeMode,
                )

                SettingsRoute.Model -> ModelSettingsScreen(
                    onBack = { route = SettingsRoute.Hub },
                    models = models,
                    selected = model,
                    onSelect = viewModel::setModel,
                )

                SettingsRoute.Keys -> KeysSettingsScreen(
                    onBack = { route = SettingsRoute.Hub },
                    apiKeys = apiKeys,
                    customModels = customModels,
                    keyRows = keyRows,
                    onTest = viewModel::test,
                    onRemove = viewModel::removeKey,
                    onClearError = viewModel::dismissKeyError,
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
 * The hub: four category rows, each a quiet lapis-tinted glyph, a title and
 * the live state of that category, and a chevron. About and the version sit
 * below, never behind a tap — they're links and a footnote, not a setting.
 */
@Composable
private fun SettingsHub(
    onBack: () -> Unit,
    themeSummary: String,
    modelSummary: String,
    keysSummary: String,
    offlineSummary: String,
    onOpenTheme: () -> Unit,
    onOpenModel: () -> Unit,
    onOpenKeys: () -> Unit,
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
                icon = VerbaIcons.Key,
                title = stringResource(R.string.settings_section_keys),
                value = keysSummary,
                onClick = onOpenKeys,
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
        Text(
            text = stringResource(R.string.model_free_tier_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(16.dp))
        SettingsCard(Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())) {
            models.forEachIndexed { index, candidate ->
                if (index > 0) RowDivider()
                ChoiceRow(
                    title = candidate.name,
                    subtitle = candidate.description?.let { stringResource(it) }
                        ?: stringResource(
                            R.string.model_custom_subtitle,
                            candidate.provider.displayName,
                        ),
                    selected = selected.id == candidate.id,
                    onClick = { onSelect(candidate) },
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun KeysSettingsScreen(
    onBack: () -> Unit,
    apiKeys: Map<Provider, String>,
    customModels: Map<Provider, String>,
    keyRows: Map<Provider, KeyRowState>,
    onTest: (Provider, String, String) -> Unit,
    onRemove: (Provider) -> Unit,
    onClearError: (Provider) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        SettingsTopBar(title = stringResource(R.string.settings_section_keys), onBack = onBack)
        Spacer(Modifier.height(20.dp))
        SettingsCard {
            // One open at a time: tapping a provider closes whichever was open.
            var expandedProvider by rememberSaveable { mutableStateOf<String?>(null) }
            // On-device engines carry no key, so they never appear as a key row.
            Provider.entries.filter { !it.onDevice }.forEachIndexed { index, provider ->
                if (index > 0) RowDivider()
                ProviderKeyRow(
                    provider = provider,
                    expanded = expandedProvider == provider.name,
                    onExpand = { expandedProvider = provider.name },
                    onCollapse = { if (expandedProvider == provider.name) expandedProvider = null },
                    savedKey = apiKeys[provider],
                    savedModel = customModels[provider],
                    state = keyRows[provider] ?: KeyRowState.Idle,
                    onTest = { key, model -> onTest(provider, key, model) },
                    onRemove = { onRemove(provider) },
                    onClearError = { onClearError(provider) },
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
 * house style, distinct from the per-provider brand tiles inside API keys —
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

/**
 * The flat color and real wordmark that stand in for each provider's logo —
 * traced into [VerbaIcons] from lobehub's MIT-licensed icon set, not an
 * invented monogram. The hue is fixed regardless of theme, the way a real
 * logo would be.
 */
private data class ProviderBrand(val color: Color, val icon: ImageVector)

private val Provider.brand: ProviderBrand
    get() = when (this) {
        Provider.OPENROUTER -> ProviderBrand(Color(0xFF6467F2), VerbaIcons.LogoOpenRouter)
        Provider.OPENAI -> ProviderBrand(Color(0xFF10A37F), VerbaIcons.LogoOpenAi)
        Provider.ANTHROPIC -> ProviderBrand(Color(0xFFCC785C), VerbaIcons.LogoAnthropic)
        Provider.GOOGLE -> ProviderBrand(Color(0xFF4285F4), VerbaIcons.LogoGoogle)
        Provider.MISTRAL -> ProviderBrand(Color(0xFFFA6400), VerbaIcons.LogoMistral)
        Provider.DEEPSEEK -> ProviderBrand(Color(0xFF4D6BFE), VerbaIcons.LogoDeepSeek)
        Provider.CEREBRAS -> ProviderBrand(Color(0xFFF15A29), VerbaIcons.LogoCerebras)
        Provider.MLKIT -> ProviderBrand(Color(0xFF757575), VerbaIcons.LogoOpenRouter)
    }

/** A provider's brand tile: its real logo on a soft wash of its own color. */
@Composable
private fun ProviderBrandTile(provider: Provider) {
    val brand = provider.brand
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(brand.color.copy(alpha = 0.16f)),
    ) {
        Icon(
            imageVector = brand.icon,
            contentDescription = null,
            tint = brand.color,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * One provider, one uniform flow for all of them: a key and a model id, tested
 * together against the live API. It rests as a quiet connected line — the model
 * it answers with, and the last four of the key — and opens on tap into the two
 * fields plus Test. Nothing is stored until that exact pair verifies, so a
 * connected row is a row you can actually translate on.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProviderKeyRow(
    provider: Provider,
    expanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    savedKey: String?,
    savedModel: String?,
    state: KeyRowState,
    onTest: (key: String, model: String) -> Unit,
    onRemove: () -> Unit,
    onClearError: () -> Unit,
) {
    val connected = savedKey != null && savedModel != null
    // Re-keying on [connected] resets the form the moment a test succeeds: the
    // key draft clears, the model draft adopts what stuck.
    var keyDraft by rememberSaveable(connected) { mutableStateOf("") }
    var modelDraft by rememberSaveable(connected) { mutableStateOf(savedModel.orEmpty()) }
    val testing = state == KeyRowState.Testing
    // A row that has just connected folds itself shut.
    LaunchedEffect(connected) { if (connected) onCollapse() }

    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())
            .clickable(enabled = !expanded) { onExpand() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProviderBrandTile(provider)
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (connected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
                if (!expanded) {
                    Text(
                        text = when {
                            connected -> stringResource(
                                R.string.keys_verified_subtitle,
                                savedModel.orEmpty(),
                                savedKey.orEmpty().takeLast(4),
                            )
                            // Both ship a free bundled model backed by a shared key;
                            // a personal key here buys a quota of the user's own.
                            provider == Provider.OPENROUTER || provider == Provider.GOOGLE ->
                                stringResource(R.string.keys_add_subtitle_shared_free)
                            else -> stringResource(R.string.keys_add_hint, provider.displayName)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            when {
                connected -> IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.keys_remove),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                !expanded -> Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (expanded) {
            val keyFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { keyFocus.requestFocus() }

            Spacer(Modifier.height(16.dp))
            CredentialField(
                label = stringResource(R.string.keys_field_key),
                value = keyDraft,
                hint = provider.keyHint,
                imeAction = ImeAction.Next,
                enabled = !testing,
                focusRequester = keyFocus,
                onValueChange = {
                    keyDraft = it
                    onClearError()
                },
                onImeAction = {},
            )
            Spacer(Modifier.height(18.dp))
            CredentialField(
                label = stringResource(R.string.keys_field_model),
                value = modelDraft,
                hint = stringResource(R.string.keys_model_hint, provider.probeModel),
                imeAction = ImeAction.Done,
                enabled = !testing,
                onValueChange = {
                    modelDraft = it
                    onClearError()
                },
                onImeAction = { onTest(keyDraft, modelDraft) },
            )

            if (state is KeyRowState.Failed) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage(state.check, provider, modelDraft),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                if (testing) {
                    LoadingIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    val ready = keyDraft.isNotBlank() && modelDraft.isNotBlank()
                    Text(
                        text = stringResource(R.string.keys_test),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (ready) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = ready) { onTest(keyDraft, modelDraft) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** Maps a failed test to the one honest line that names what actually went wrong. */
@Composable
private fun errorMessage(check: CredentialCheck, provider: Provider, model: String): String =
    when (check) {
        CredentialCheck.INVALID_KEY ->
            stringResource(R.string.keys_err_key, provider.displayName)
        CredentialCheck.MODEL_NOT_FOUND ->
            stringResource(R.string.keys_err_model, provider.displayName, model.trim())
        CredentialCheck.RATE_LIMITED ->
            stringResource(R.string.keys_err_rate, provider.displayName)
        CredentialCheck.UNREACHABLE ->
            stringResource(R.string.keys_err_network, provider.displayName)
        CredentialCheck.VALID, CredentialCheck.UNKNOWN ->
            stringResource(R.string.keys_err_unknown, provider.displayName)
    }

/**
 * One labelled, borderless input on a hairline rule — the same typing surface
 * as the rest of Verba, dressed with a small caps label and an underline so two
 * of them stack legibly. Used for both the key and the model id.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CredentialField(
    label: String,
    value: String,
    hint: String,
    imeAction: ImeAction,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onImeAction: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            enabled = enabled,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(text = hint, style = textStyle, color = MaterialTheme.colorScheme.outline)
                    }
                    innerTextField()
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
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
