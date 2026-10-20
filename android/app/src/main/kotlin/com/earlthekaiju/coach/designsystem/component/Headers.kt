package com.earlthekaiju.coach.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace

/** Small, wide-tracked uppercase label. Gold by default (9.2:1 on Shadow). */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Kaiju.colors.gold,
    style: TextStyle = Kaiju.type.eyebrow,
) {
    Text(text = text.uppercase(), style = style, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Two-digit editorial index: 1 → "01". */
fun indexLabel(index: Int): String = index.toString().padStart(2, '0')

/**
 * Editorial section header: `01  NEXT CLASS ─────────── [action]`.
 * The gold index and the hairline give every screen the same rhythm.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    index: Int? = null,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = KaijuSpace.xs)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (index != null) {
            Text(indexLabel(index), style = Kaiju.type.index, color = Kaiju.colors.gold)
            Spacer(Modifier.width(KaijuSpace.s))
        }
        Text(
            text = title.uppercase(),
            style = Kaiju.type.eyebrow,
            color = Kaiju.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(KaijuSpace.s))
        HairlineDivider(Modifier.weight(1f))
        if (action != null) {
            Spacer(Modifier.width(KaijuSpace.xs))
            action()
        }
    }
}

/**
 * Large editorial header for the five top-level screens: a gold eyebrow over a display title,
 * with an optional trailing slot (settings, add). Scrolls with the content on purpose.
 */
@Composable
fun ScreenHeader(
    title: String,
    eyebrow: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    titleStyle: TextStyle = Kaiju.type.displayL,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = KaijuSpace.m, bottom = KaijuSpace.s),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Eyebrow(eyebrow)
            Spacer(Modifier.height(KaijuSpace.xs))
            Text(
                text = title.uppercase(),
                style = titleStyle,
                color = Kaiju.colors.ink,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier, color: Color = Kaiju.colors.line, thickness: Dp = 1.dp) {
    Box(modifier.height(thickness).background(color))
}

/** Hairline with a gold diamond in the middle (after the brand kit's diamond divider). */
@Composable
fun DiamondDivider(modifier: Modifier = Modifier) {
    val line = Kaiju.colors.line
    val gold = Kaiju.colors.gold
    Row(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HairlineDivider(Modifier.weight(1f), color = line)
        Canvas(Modifier.padding(horizontal = KaijuSpace.xs).size(8.dp)) {
            val path = Path().apply {
                moveTo(size.width / 2f, 0f); lineTo(size.width, size.height / 2f)
                lineTo(size.width / 2f, size.height); lineTo(0f, size.height / 2f); close()
            }
            drawPath(path, gold)
        }
        HairlineDivider(Modifier.weight(1f), color = line)
    }
}

/**
 * Gold rule with an emerald keystone, echoing the rule under KAIJU in the practical lockup.
 * Decorative only.
 */
@Composable
fun KeystoneRule(modifier: Modifier = Modifier, width: Dp = 120.dp) {
    val gold = Kaiju.colors.gold
    val emerald = Kaiju.colors.action
    Canvas(modifier.size(width = width, height = 12.dp).clearAndSetSemantics { }) {
        val ruleH = 3.dp.toPx()
        drawRect(gold, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(size.width, ruleH))
        val kw = 14.dp.toPx()
        val cx = size.width * 0.18f
        val path = Path().apply {
            moveTo(cx - kw / 2f, ruleH); lineTo(cx + kw / 2f, ruleH); lineTo(cx, ruleH + kw * 0.6f); close()
        }
        drawPath(path, emerald)
    }
}

/**
 * Text rotated 90° to run up the left margin: a Japanese-editorial vertical label.
 * Purely decorative ([description] is read instead, if given).
 */
@Composable
fun VerticalLabel(text: String, modifier: Modifier = Modifier, color: Color = Kaiju.colors.inkMuted, description: String? = null) {
    Box(
        modifier = modifier
            .semantics { if (description != null) contentDescription = description }
            .verticalRotated(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = Kaiju.type.eyebrow, color = color, maxLines = 1, softWrap = false)
    }
}
