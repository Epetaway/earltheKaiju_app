package com.earlthekaiju.coach.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace

enum class ButtonVariant { Primary, Secondary, Ghost, Danger }

enum class ButtonSize(val height: Dp) {
    /** Standard 52dp: forms, sheets, dialogs. */
    Regular(52.dp),
    /** 48dp: compact rows and toolbars (still meets the touch minimum). */
    Compact(48.dp),
    /** 64dp: the one dominant action on a screen (Start Class, timer controls). */
    Hero(64.dp),
}

/**
 * The app's button. Primary = Emerald fill with Ivory label (4.7:1) and one chamfered corner,
 * pressed = Deep Emerald (7.1:1). Secondary = outline, Ghost = accent text, Danger = Signal outline.
 * Keyboard and switch-access focus draws the brand focus ring (2dp Spectral Emerald, 2dp offset).
 */
@Composable
fun KaijuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Regular,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = Kaiju.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape: Shape = when {
        variant == ButtonVariant.Ghost -> KaijuShapes.Small
        size == ButtonSize.Hero -> KaijuShapes.ActionHero
        else -> KaijuShapes.Action
    }
    val container = when (variant) {
        ButtonVariant.Primary -> if (pressed) colors.actionPressed else colors.action
        ButtonVariant.Secondary -> if (pressed) colors.surfaceInset else Color.Transparent
        ButtonVariant.Ghost -> if (pressed) colors.surfaceInset else Color.Transparent
        ButtonVariant.Danger -> if (pressed) colors.surfaceInset else Color.Transparent
    }
    val content = when (variant) {
        ButtonVariant.Primary -> colors.onAction
        ButtonVariant.Secondary -> colors.ink
        ButtonVariant.Ghost -> colors.accent
        ButtonVariant.Danger -> colors.signal
    }
    val border: BorderStroke? = when (variant) {
        ButtonVariant.Secondary -> BorderStroke(1.dp, colors.lineStrong)
        ButtonVariant.Danger -> BorderStroke(1.dp, colors.signal)
        else -> null
    }
    val alpha = if (enabled) 1f else 0.38f
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = size.height, minWidth = KaijuSpace.touch)
            .focusRing(interaction, shape)
            .clip(shape)
            .background(if (enabled) container else colors.surfaceInset.copy(alpha = 0.6f))
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = content),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = if (variant == ButtonVariant.Ghost) KaijuSpace.s else KaijuSpace.l),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content.copy(alpha = alpha), modifier = Modifier.size(if (size == ButtonSize.Hero) 26.dp else 20.dp))
                Spacer(Modifier.width(KaijuSpace.xs + 2.dp))
            }
            Text(
                text = text,
                style = if (size == ButtonSize.Hero) Kaiju.type.title else Kaiju.type.labelL,
                color = content.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A 48dp icon-only button. [contentDescription] is required: every icon action is announced. */
@Composable
fun KaijuIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Kaiju.colors.ink,
    enabled: Boolean = true,
    size: Dp = KaijuSpace.touch,
    iconSize: Dp = 24.dp,
    filled: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = Kaiju.colors
    Box(
        modifier = modifier
            .size(size)
            .focusRing(interaction, KaijuShapes.Small)
            .clip(KaijuShapes.Small)
            .then(if (filled) Modifier.background(colors.surfaceInset) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = tint),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) tint else tint.copy(alpha = 0.38f), modifier = Modifier.size(iconSize))
    }
}

/** Brand focus ring: drawn only while the element has keyboard / switch-access focus. */
@Composable
fun Modifier.focusRing(interaction: MutableInteractionSource, shape: Shape): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    val ring = Kaiju.colors.live
    return if (focused) this.border(2.dp, ring, shape) else this
}
