package com.skeler.verba.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.skeler.verba.R

/**
 * One voice: Inter everywhere. The translation still gets the display tiers —
 * scale does the talking, not a second typeface.
 */
val Inter = FontFamily(
    Font(R.font.inter_regular, weight = FontWeight.Normal),
    Font(R.font.inter_medium, weight = FontWeight.Medium),
)

val VerbaTypography = Typography(
    // Display tiers: the translated result, stepped down as text grows.
    // Inter wants negative tracking at these sizes.
    displayLarge = TextStyle(
        fontFamily = Inter,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Inter,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Inter,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
    ),
)
