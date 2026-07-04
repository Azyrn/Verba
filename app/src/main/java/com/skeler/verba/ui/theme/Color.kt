package com.skeler.verba.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Verba's palette: ink on paper, with lapis — the pigment medieval scribes
// saved for the words that mattered — reserved for the interactive layer.

private val Lapis = Color(0xFF2F4EC2)
private val LapisDim = Color(0xFF1A2C6E)
private val LapisWash = Color(0xFFDEE4FB)
private val LapisBright = Color(0xFFADBEF8)
private val LapisNight = Color(0xFF2C3F87)

private val Paper = Color(0xFFF7F6F1)
private val Ink = Color(0xFF1B1C1F)
private val Night = Color(0xFF131519)
private val Linen = Color(0xFFE9E7DF)

val LightScheme = lightColorScheme(
    primary = Lapis,
    onPrimary = Color.White,
    primaryContainer = LapisWash,
    onPrimaryContainer = LapisDim,
    inversePrimary = LapisBright,
    secondary = Color(0xFF5A5F6E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E6EC),
    onSecondaryContainer = Color(0xFF23262F),
    tertiary = Color(0xFF8A5A2E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2E2CF),
    onTertiaryContainer = Color(0xFF4A2E12),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Linen,
    onSurfaceVariant = Color(0xFF55565A),
    surfaceTint = Lapis,
    inverseSurface = Color(0xFF303136),
    inverseOnSurface = Color(0xFFF2F1EC),
    error = Color(0xFFA93F35),
    onError = Color.White,
    errorContainer = Color(0xFFF5DDD9),
    onErrorContainer = Color(0xFF5E1F19),
    outline = Color(0xFF787971),
    outlineVariant = Color(0xFFCBCAC2),
    scrim = Color.Black,
    surfaceBright = Paper,
    surfaceDim = Color(0xFFD8D7D0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F0EA),
    surfaceContainer = Color(0xFFECEBE4),
    surfaceContainerHigh = Color(0xFFE6E5DE),
    surfaceContainerHighest = Color(0xFFE0DFD8),
)

val DarkScheme = darkColorScheme(
    primary = LapisBright,
    onPrimary = Color(0xFF17275F),
    primaryContainer = LapisNight,
    onPrimaryContainer = LapisWash,
    inversePrimary = Color(0xFF3752B9),
    secondary = Color(0xFFC2C5CF),
    onSecondary = Color(0xFF2B2E37),
    secondaryContainer = Color(0xFF41454F),
    onSecondaryContainer = Color(0xFFDEE1EA),
    tertiary = Color(0xFFE2BE93),
    onTertiary = Color(0xFF402A10),
    tertiaryContainer = Color(0xFF5C4225),
    onTertiaryContainer = Color(0xFFF2E2CF),
    background = Night,
    onBackground = Color(0xFFE5E4E0),
    surface = Night,
    onSurface = Color(0xFFE5E4E0),
    surfaceVariant = Color(0xFF24262C),
    onSurfaceVariant = Color(0xFFA7A9AE),
    surfaceTint = LapisBright,
    inverseSurface = Color(0xFFE5E4E0),
    inverseOnSurface = Color(0xFF2E3033),
    error = Color(0xFFE7998F),
    onError = Color(0xFF4A1710),
    errorContainer = Color(0xFF7A342B),
    onErrorContainer = Color(0xFFF7DAD5),
    outline = Color(0xFF83858B),
    outlineVariant = Color(0xFF3B3E45),
    scrim = Color.Black,
    surfaceBright = Color(0xFF35383F),
    surfaceDim = Night,
    surfaceContainerLowest = Color(0xFF0D0F12),
    surfaceContainerLow = Color(0xFF191B20),
    surfaceContainer = Color(0xFF1D2025),
    surfaceContainerHigh = Color(0xFF262930),
    surfaceContainerHighest = Color(0xFF2F323A),
)

// True Black gets its own lapis: pure black swallows the dark scheme's tint,
// so the ink brightens a step and the containers sink deeper to keep contrast.
private val LapisGlow = Color(0xFFBFCDFF)
private val LapisAbyss = Color(0xFF20316E)

/** OLED variant: a brighter lapis ink on a pixels-off background. */
val BlackScheme = DarkScheme.copy(
    primary = LapisGlow,
    onPrimary = Color(0xFF0E1B4D),
    primaryContainer = LapisAbyss,
    onPrimaryContainer = Color(0xFFDCE4FF),
    inversePrimary = Color(0xFF3A55BE),
    secondary = Color(0xFFC9CDDB),
    onSecondary = Color(0xFF23262F),
    secondaryContainer = Color(0xFF34394A),
    onSecondaryContainer = Color(0xFFE2E5F1),
    surfaceTint = LapisGlow,
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF2A2D33),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0C0F),
    surfaceContainer = Color(0xFF101216),
    surfaceContainerHigh = Color(0xFF171A1F),
    surfaceContainerHighest = Color(0xFF20232A),
    surfaceVariant = Color(0xFF1C1E23),
)
