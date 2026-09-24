package com.skeler.verba.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDirection

/**
 * The direction [text] reads in, by the script most of its letters are in.
 * Android's default looks only at the first letter, so an Arabic sentence
 * that opens with an English name gets laid out left to right and its
 * punctuation and word order come out scrambled.
 */
fun textDirectionOf(text: CharSequence): TextDirection {
    var rtl = 0
    var ltr = 0
    var i = 0
    while (i < text.length) {
        val codePoint = Character.codePointAt(text, i)
        when (Character.getDirectionality(codePoint)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> rtl++
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> ltr++
        }
        i += Character.charCount(codePoint)
    }
    return when {
        rtl > ltr -> TextDirection.Rtl
        ltr > rtl -> TextDirection.Ltr
        else -> TextDirection.Content
    }
}

/** [text] with each paragraph set in its own majority direction, for mixed-language answers. */
fun bidiText(text: String): AnnotatedString = buildAnnotatedString {
    var start = 0
    while (start <= text.length) {
        val end = text.indexOf('\n', start).let { if (it < 0) text.length else it + 1 }
        val paragraph = text.substring(start, end)
        pushStyle(ParagraphStyle(textDirection = textDirectionOf(paragraph)))
        append(paragraph)
        pop()
        if (end >= text.length) break
        start = end
    }
}
