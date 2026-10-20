package com.earlthekaiju.coach.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.domain.model.Belt
import com.earlthekaiju.coach.domain.model.SessionStatus

/**
 * Session status, distinguishable without color: each state has its own word, icon and treatment.
 * Scheduled = outline + clock · In progress = Spectral fill + live dot · Completed = check on Facet ·
 * Canceled = Signal text, struck through, with an X.
 */
@Composable
fun SessionStatusBadge(status: SessionStatus, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    val base = modifier.semantics(mergeDescendants = true) { contentDescription = "Status: ${status.label}" }
    when (status) {
        SessionStatus.SCHEDULED -> BadgeFrame(base.border(1.dp, colors.lineStrong, KaijuShapes.Small)) {
            Icon(KaijuIcons.Clock, null, tint = colors.inkMuted, modifier = Modifier.size(14.dp))
            BadgeText("Scheduled", colors.inkMuted)
        }
        SessionStatus.IN_PROGRESS -> BadgeFrame(base.background(colors.live, KaijuShapes.Small)) {
            LiveDot(color = colors.onLive, size = 7.dp, pulse = false)
            BadgeText("Live", colors.onLive)
        }
        SessionStatus.COMPLETED -> BadgeFrame(base.background(colors.surfaceInset, KaijuShapes.Small)) {
            Icon(KaijuIcons.Check, null, tint = colors.accent, modifier = Modifier.size(14.dp))
            BadgeText("Done", colors.ink)
        }
        SessionStatus.CANCELED -> BadgeFrame(base.border(1.dp, colors.signal, KaijuShapes.Small)) {
            Icon(KaijuIcons.Close, null, tint = colors.signal, modifier = Modifier.size(14.dp))
            BadgeText("Canceled", colors.signal, strike = true)
        }
    }
}

@Composable
private fun BadgeFrame(modifier: Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier.padding(horizontal = KaijuSpace.xs, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) { content() }
}

@Composable
private fun BadgeText(text: String, color: Color, strike: Boolean = false) {
    Text(
        text.uppercase(),
        style = Kaiju.type.eyebrow.copy(textDecoration = if (strike) TextDecoration.LineThrough else null),
        color = color,
        maxLines = 1,
    )
}

/** A small square "live" mark. Pulses slowly unless the system asks for reduced motion. */
@Composable
fun LiveDot(modifier: Modifier = Modifier, color: Color = Kaiju.colors.live, size: Dp = 10.dp, pulse: Boolean = true) {
    val animate = pulse && !Kaiju.reduceMotion
    val alpha = if (animate) {
        val transition = rememberInfiniteTransition(label = "live")
        val a by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "liveAlpha",
        )
        a
    } else {
        1f
    }
    Canvas(modifier.size(size).alpha(alpha).clearAndSetSemantics { }) {
        // A diamond: the crest's geometry at bullet size.
        val w = size.width
        val h = size.height
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w / 2f, 0f)
            lineTo(w, h / 2f)
            lineTo(w / 2f, h)
            lineTo(0f, h / 2f)
            close()
        }
        drawPath(p, color)
    }
}

/**
 * A BJJ belt drawn to scale: belt color, the kids' white/black center bar, and the rank bar
 * with stripes (white tape) or black-belt degrees. Announced as words, e.g. "Blue belt, 2 stripes".
 */
@Composable
fun BeltBar(belt: Belt, stripes: Int, modifier: Modifier = Modifier, width: Dp = 56.dp, height: Dp = 14.dp) {
    val outline = Kaiju.colors.lineStrong
    Canvas(
        modifier
            .size(width, height)
            .semantics { contentDescription = belt.describe(stripes) },
    ) {
        val w = size.width
        val h = size.height
        drawRect(Color(belt.colorArgb))
        belt.centerBarArgb?.let { bar ->
            drawRect(Color(bar), topLeft = Offset(0f, h * 0.36f), size = Size(w, h * 0.28f))
        }
        // Rank bar near the end of the belt.
        val barW = w * 0.34f
        val barX = w * 0.58f
        drawRect(Color(belt.rankBarArgb), topLeft = Offset(barX, 0f), size = Size(barW, h))
        val count = stripes.coerceIn(0, belt.maxStripes)
        if (count > 0) {
            val stripeW = (barW / (belt.maxStripes * 2f + 1f)).coerceAtLeast(1.5f)
            for (i in 0 until count) {
                val x = barX + barW - stripeW * (2 * i + 2)
                drawRect(Color(0xFFF4F1E9), topLeft = Offset(x, 0f), size = Size(stripeW, h))
            }
        }
        drawRect(outline, style = Stroke(width = 1.dp.toPx()))
    }
}

/**
 * Honest sync status. There is no sync engine in Sprint 01, so this never pretends to connect:
 * it says where the data lives and when it was last exported.
 */
@Composable
fun SyncStatusPanel(
    lastBackupLabel: String?,
    modifier: Modifier = Modifier,
) {
    val colors = Kaiju.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.surface, KaijuShapes.Card)
            .border(1.dp, colors.line, KaijuShapes.Card)
            .padding(KaijuSpace.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(KaijuIcons.Phone, null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(KaijuSpace.s))
            Text("Saved on this phone", style = Kaiju.type.titleSmall, color = colors.ink, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .border(1.dp, colors.lineStrong, KaijuShapes.Small)
                    .padding(horizontal = KaijuSpace.xs, vertical = 3.dp),
            ) { Text("OFFLINE-READY", style = Kaiju.type.eyebrow, color = colors.inkMuted) }
        }
        Spacer(Modifier.height(KaijuSpace.xs))
        Text(
            "Everything works without internet. Desktop sync isn't set up yet — it arrives with the desktop companion.",
            style = Kaiju.type.bodyS,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(KaijuSpace.s))
        HairlineDivider(Modifier.fillMaxWidth())
        Spacer(Modifier.height(KaijuSpace.s))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(KaijuIcons.Desktop, null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(KaijuSpace.s))
            Text("Paired computer", style = Kaiju.type.bodyS, color = colors.inkMuted, modifier = Modifier.weight(1f))
            Text("None", style = Kaiju.type.label, color = colors.ink)
        }
        Spacer(Modifier.height(KaijuSpace.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(KaijuIcons.Export, null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(KaijuSpace.s))
            Text("Last backup export", style = Kaiju.type.bodyS, color = colors.inkMuted, modifier = Modifier.weight(1f))
            Text(lastBackupLabel ?: "Never", style = Kaiju.type.label, color = if (lastBackupLabel == null) colors.gold else colors.ink)
        }
    }
}

/**
 * Proportional progress through a class: one block per segment, sized by its minutes.
 * Done = accent fill, current = Spectral outline with partial fill, upcoming = Facet.
 */
@Composable
fun SegmentProgressBar(
    minutes: List<Int>,
    currentIndex: Int,
    currentFraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
) {
    val colors = Kaiju.colors
    val total = minutes.sum().coerceAtLeast(1)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                contentDescription = "Segment ${(currentIndex + 1).coerceAtMost(minutes.size)} of ${minutes.size}"
            },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        minutes.forEachIndexed { index, m ->
            val weight = (m.coerceAtLeast(1)).toFloat() / total
            Canvas(Modifier.weight(weight).height(height)) {
                when {
                    index < currentIndex -> drawRect(colors.accent)
                    index == currentIndex -> {
                        drawRect(colors.surfaceInset)
                        drawRect(colors.live, size = Size(size.width * currentFraction.coerceIn(0f, 1f), size.height))
                        drawRect(colors.live, style = Stroke(width = 1.5.dp.toPx()))
                    }
                    else -> drawRect(colors.surfaceInset)
                }
            }
        }
    }
}
