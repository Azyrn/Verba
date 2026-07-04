package com.skeler.verba.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import com.skeler.verba.model.ThemeMode

/** Single source of truth for whether a [ThemeMode] renders dark right now. */
@Composable
fun ThemeMode.resolvesToDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.TRUE_BLACK -> true
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VerbaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.resolvesToDark()
    val colorScheme = when {
        !darkTheme -> LightScheme
        themeMode == ThemeMode.TRUE_BLACK -> BlackScheme
        else -> DarkScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = VerbaTypography,
        content = content,
    )
}
