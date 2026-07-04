package com.skeler.verba.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.skeler.verba.R
import com.skeler.verba.model.LanguagePair
import com.skeler.verba.model.LanguageSide
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * The single language control: "English ⇄ Spanish" in one pill. Tapping a
 * label opens the picker on that side; the middle glyph (or a long-press
 * anywhere) swaps direction — the two labels physically trade places.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LanguagePairChip(
    pair: LanguagePair,
    onOpenPicker: (LanguageSide) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swapEnabled = !pair.source.isAuto
    val swapProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val motion = MaterialTheme.motionScheme
    val swapSpec = remember(motion) { motion.fastSpatialSpec<Float>() }

    fun performSwap() {
        if (!swapEnabled || swapProgress.isRunning) return
        scope.launch {
            swapProgress.animateTo(1f, swapSpec)
            onSwap()
            swapProgress.snapTo(0f)
        }
    }

    val chipDescription = stringResource(
        R.string.pair_chip_description, pair.source.name, pair.target.name,
    )
    val swapLabel = stringResource(R.string.swap_languages)

    // Animatable.value is state-backed, so the tint tracks the swap frame by frame.
    val glyphColor by animateColorAsState(
        targetValue = if (swapProgress.value > 0f) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motion.fastEffectsSpec(),
        label = "swapGlyphColor",
    )
    Surface(
        modifier = modifier.semantics { contentDescription = chipDescription },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier
                .combinedClickable(
                    onClick = { onOpenPicker(LanguageSide.TARGET) },
                    onLongClick = ::performSwap,
                    onLongClickLabel = swapLabel,
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            TradingLabels(
                sourceName = pair.source.name,
                targetName = pair.target.name,
                progress = { swapProgress.value },
                onSourceClick = { onOpenPicker(LanguageSide.SOURCE) },
                onTargetClick = { onOpenPicker(LanguageSide.TARGET) },
                swapIcon = {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = swapEnabled, onClick = ::performSwap),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SwapGlyph,
                            contentDescription = swapLabel,
                            tint = if (swapEnabled) glyphColor
                            else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer { rotationZ = 180f * swapProgress.value },
                        )
                    }
                },
            )
        }
    }
}

/**
 * Lays out [source] [icon] [target] and, driven by [progress], slides the two
 * labels along shallow opposing arcs into each other's slots — the swap reads
 * as the words trading places, not the screen reloading.
 */
@Composable
private fun TradingLabels(
    sourceName: String,
    targetName: String,
    progress: () -> Float,
    onSourceClick: () -> Unit,
    onTargetClick: () -> Unit,
    swapIcon: @Composable () -> Unit,
) {
    Layout(
        content = {
            ChipLabel(text = sourceName, onClick = onSourceClick)
            swapIcon()
            ChipLabel(text = targetName, onClick = onTargetClick)
        },
    ) { measurables, constraints ->
        val iconPlaceable = measurables[1].measure(Constraints())
        val gap = 6.dp.roundToPx()
        val labelBudget = ((constraints.maxWidth - iconPlaceable.width) / 2 - gap)
            .coerceAtLeast(0)
        val labelConstraints = Constraints(maxWidth = labelBudget)
        val sourcePlaceable = measurables[0].measure(labelConstraints)
        val targetPlaceable = measurables[2].measure(labelConstraints)

        val width = sourcePlaceable.width + targetPlaceable.width + iconPlaceable.width + gap * 2
        val height = maxOf(sourcePlaceable.height, targetPlaceable.height, iconPlaceable.height)

        layout(width, height) {
            val t = progress()
            val arc = (6.dp.toPx() * sin(PI * t.toDouble())).toFloat().roundToInt()

            val sourceStart = 0
            val sourceEnd = width - sourcePlaceable.width
            val targetStart = width - targetPlaceable.width
            val targetEnd = 0

            fun lerp(from: Int, to: Int): Int = (from + (to - from) * t).roundToInt()

            sourcePlaceable.placeRelative(
                x = lerp(sourceStart, sourceEnd),
                y = (height - sourcePlaceable.height) / 2 - arc,
            )
            targetPlaceable.placeRelative(
                x = lerp(targetStart, targetEnd),
                y = (height - targetPlaceable.height) / 2 + arc,
            )
            iconPlaceable.placeRelative(
                x = (width - iconPlaceable.width) / 2,
                y = (height - iconPlaceable.height) / 2,
            )
        }
    }
}

@Composable
private fun ChipLabel(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** Two opposing arrows, point-symmetric so a 180° spin lands on itself. */
private val SwapGlyph: ImageVector by lazy {
    ImageVector.Builder(
        name = "SwapGlyph",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // Top arrow, pointing right.
            moveTo(4f, 8f)
            lineTo(17f, 8f)
            moveTo(13.5f, 4.5f)
            lineTo(17f, 8f)
            lineTo(13.5f, 11.5f)
            // Bottom arrow, pointing left.
            moveTo(20f, 16f)
            lineTo(7f, 16f)
            moveTo(10.5f, 12.5f)
            lineTo(7f, 16f)
            lineTo(10.5f, 19.5f)
        }
    }.build()
}
