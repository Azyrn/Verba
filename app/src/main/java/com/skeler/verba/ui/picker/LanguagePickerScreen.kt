package com.skeler.verba.ui.picker

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skeler.verba.R
import com.skeler.verba.model.Language
import com.skeler.verba.model.LanguagePair
import com.skeler.verba.model.LanguageSide
import com.skeler.verba.model.Languages
import com.skeler.verba.ui.SHEET_ENTER_MILLIS
import kotlinx.coroutines.delay

/**
 * The searchable picker, arriving as a full-height sheet. One list, two
 * sides: the From/To toggle at the top decides which half a tap replaces.
 */
@Composable
fun LanguagePickerScreen(
    initialSide: LanguageSide,
    pair: LanguagePair,
    onSelect: (LanguageSide, Language) -> Unit,
    onClose: () -> Unit,
) {
    var side by rememberSaveable { mutableStateOf(initialSide) }
    var query by rememberSaveable { mutableStateOf("") }

    val results = remember(query) { Languages.search(query) }
    val current = if (side == LanguageSide.SOURCE) pair.source else pair.target

    // Focusing the search field immediately drags the IME animation and its
    // relayout into the middle of the sheet's slide — profiled at >50% janky
    // frames. Let the sheet land first, then summon the keyboard.
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(SHEET_ENTER_MILLIS + 50)
        searchFocus.requestFocus()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding(),
            ) {
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    SideToggle(
                        pair = pair,
                        side = side,
                        onSideChange = { side = it },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(8.dp))
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.picker_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    focusRequester = searchFocus,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                Spacer(Modifier.height(8.dp))

                if (results.isEmpty()) {
                    NoMatches(query = query)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (side == LanguageSide.SOURCE && query.isBlank()) {
                            item(key = Language.AUTO_CODE) {
                                LanguageRow(
                                    title = Language.Auto.nativeName,
                                    subtitle = stringResource(R.string.picker_detect_subtitle),
                                    selected = current.isAuto,
                                    onClick = { onSelect(side, Language.Auto) },
                                )
                            }
                        }
                        items(results, key = { it.code }) { language ->
                            LanguageRow(
                                title = language.nativeName,
                                subtitle = language.name,
                                selected = language == current,
                                onClick = { onSelect(side, language) },
                            )
                        }
                    }
                }
            }
        }
}

/**
 * From/To selector. The lapis underline springs across to whichever side is
 * being edited; each side shows its current language beneath its role.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SideToggle(
    pair: LanguagePair,
    side: LanguageSide,
    onSideChange: (LanguageSide) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.motionScheme
    BoxWithConstraints(modifier) {
        val tabWidth = maxWidth / 2
        val indicatorOffset by animateFloatAsState(
            targetValue = if (side == LanguageSide.SOURCE) 0f else 1f,
            animationSpec = motion.fastSpatialSpec(),
            label = "sideIndicator",
        )
        Column {
            Row {
                SideTab(
                    role = stringResource(R.string.picker_from),
                    language = pair.source.name,
                    active = side == LanguageSide.SOURCE,
                    onClick = { onSideChange(LanguageSide.SOURCE) },
                    modifier = Modifier.weight(1f),
                )
                SideTab(
                    role = stringResource(R.string.picker_to),
                    language = pair.target.name,
                    active = side == LanguageSide.TARGET,
                    onClick = { onSideChange(LanguageSide.TARGET) },
                    modifier = Modifier.weight(1f),
                )
            }
            Box(
                Modifier
                    .offset(x = tabWidth * indicatorOffset)
                    .size(width = tabWidth, height = 2.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun SideTab(
    role: String,
    language: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = role.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = language,
            style = MaterialTheme.typography.titleMedium,
            color = if (active) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.picker_search_hint),
                            style = textStyle,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.picker_search_clear),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun LanguageRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.picker_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NoMatches(query: String) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
        Text(
            text = stringResource(R.string.picker_no_match_title, query.trim()),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.picker_no_match_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
