package com.skeler.verba.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The handful of glyphs Verba needs beyond material-icons-core, inlined so the
 * multi-megabyte extended set stays out of the APK. Standard Material path
 * data, 24dp grid, tinted by [androidx.compose.material3.Icon] as usual.
 */
object VerbaIcons {

    val Copy: ImageVector by lazy {
        icon(
            "Verba.Copy",
            "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 " +
                "1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z",
        )
    }

    val Paste: ImageVector by lazy {
        icon(
            "Verba.Paste",
            "M19 2h-4.18C14.4.84 13.3 0 12 0c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 " +
                ".9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 " +
                "0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm7 18H5V4h2v3h10V4h2v16z",
        )
    }

    val Bookmark: ImageVector by lazy {
        icon(
            "Verba.Bookmark",
            "M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15l-5-2.18L7 18V5h10v13z",
        )
    }

    val BookmarkFilled: ImageVector by lazy {
        icon(
            "Verba.BookmarkFilled",
            "M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z",
        )
    }

    val ChevronRight: ImageVector by lazy {
        icon(
            "Verba.ChevronRight",
            "M8.59 16.59 13.17 12 8.59 7.41 10 6l6 6-6 6z",
        )
    }

    val Delete: ImageVector by lazy {
        icon(
            "Verba.Delete",
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 " +
                "1H5v2h14V4z",
        )
    }

    /** Settings hub row glyphs: appearance, model, keys, offline languages. */
    val Brightness: ImageVector by lazy {
        icon(
            "Verba.Brightness",
            "M12 18V6c3.31 0 6 2.69 6 6s-2.69 6-6 6zm0-16C6.48 2 2 6.48 2 " +
                "12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z",
        )
    }

    val Sparkle: ImageVector by lazy {
        icon(
            "Verba.Sparkle",
            "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zM11.5 " +
                "9.5 9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 " +
                "2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z",
        )
    }

    val Download: ImageVector by lazy {
        icon(
            "Verba.Download",
            "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z",
        )
    }

    /** Voice: dictate into the input, and read the translation aloud. */
    val Mic: ImageVector by lazy {
        icon(
            "Verba.Mic",
            "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 " +
                "1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 " +
                "6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z",
        )
    }

    val Stop: ImageVector by lazy {
        icon("Verba.Stop", "M6 6h12v12H6z")
    }

    val VolumeUp: ImageVector by lazy {
        icon(
            "Verba.VolumeUp",
            "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 " +
                "2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 " +
                "6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z",
        )
    }

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
            .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
            .build()
}
