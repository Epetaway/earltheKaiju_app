package com.earlthekaiju.coach.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A rectangle with 45-degree cuts on chosen corners: the app's "controlled geometric angle",
 * borrowed from the crest's triangle. Cuts mirror correctly in right-to-left layouts.
 */
@Immutable
class ChamferShape(
    private val topStart: Dp = 0.dp,
    private val topEnd: Dp = 0.dp,
    private val bottomEnd: Dp = 0.dp,
    private val bottomStart: Dp = 0.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val ltr = layoutDirection == LayoutDirection.Ltr
        val maxCut = minOf(size.width, size.height) / 2f
        fun px(d: Dp) = with(density) { d.toPx() }.coerceIn(0f, maxCut)
        val tl = px(if (ltr) topStart else topEnd)
        val tr = px(if (ltr) topEnd else topStart)
        val br = px(if (ltr) bottomEnd else bottomStart)
        val bl = px(if (ltr) bottomStart else bottomEnd)
        val path = Path().apply {
            moveTo(tl, 0f)
            lineTo(size.width - tr, 0f)
            lineTo(size.width, tr)
            lineTo(size.width, size.height - br)
            lineTo(size.width - br, size.height)
            lineTo(bl, size.height)
            lineTo(0f, size.height - bl)
            lineTo(0f, tl)
            close()
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is ChamferShape && other.topStart == topStart && other.topEnd == topEnd &&
            other.bottomEnd == bottomEnd && other.bottomStart == bottomStart

    override fun hashCode(): Int = listOf(topStart, topEnd, bottomEnd, bottomStart).hashCode()
}

/** Shape system: square by default (brand radius 0–2px), chamfers for emphasis. */
object KaijuShapes {
    /** Brand radius-1 (2dp): chips, inputs, small controls. */
    val Small = RoundedCornerShape(2.dp)
    val Square = RoundedCornerShape(0.dp)
    /** Primary and hero buttons: one cut corner, bottom-end. */
    val Action = ChamferShape(bottomEnd = 10.dp)
    val ActionHero = ChamferShape(bottomEnd = 16.dp)
    /** Cards: a cut on the top-end corner. */
    val Card = ChamferShape(topEnd = 14.dp)
    val CardFeature = ChamferShape(topEnd = 28.dp)
    /** Bottom sheets: a single cut on the top-start corner. */
    val Sheet = ChamferShape(topStart = 20.dp)
    val Dialog = ChamferShape(topEnd = 18.dp)
}

/** Spacing scale from brand tokens (space-1 … space-9) plus layout constants. */
object KaijuSpace {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val x3 = 64.dp
    val x4 = 96.dp

    /** Side margin for phone layouts. */
    val gutter = 20.dp
    /** Minimum touch target (brand guidelines: 48dp in the app). */
    val touch = 48.dp
    /** Comfortable target for mat-side controls (Class Mode, Timer). */
    val touchLarge = 64.dp
    /** Readable column width on wide screens. */
    val maxContentWidth = 640.dp
}
