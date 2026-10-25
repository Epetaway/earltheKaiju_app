package com.earlthekaiju.coach.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace

/**
 * Text field with its label above (never a vanishing placeholder-as-label).
 * Raised fill, 1dp Line Strong border (3.6:1), 2dp accent border when focused,
 * Signal border plus an icon and a message on error.
 */
@Composable
fun KaijuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Kaiju.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val isError = errorText != null
    val borderColor = when {
        isError -> colors.signal
        focused -> colors.accent
        else -> colors.lineStrong
    }
    Column(modifier) {
        Text(label, style = Kaiju.type.label, color = colors.ink)
        Spacer(Modifier.height(KaijuSpace.xs))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            textStyle = Kaiju.type.body.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.live),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (isError) error(errorText.orEmpty())
                },
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .background(colors.surface, KaijuShapes.Small)
                        .border(if (focused || isError) 2.dp else 1.dp, borderColor, KaijuShapes.Small)
                        .padding(horizontal = KaijuSpace.m, vertical = KaijuSpace.s),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(KaijuSpace.s))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = Kaiju.type.body, color = colors.inkMuted, maxLines = if (singleLine) 1 else 3)
                        }
                        inner()
                    }
                    if (trailing != null) {
                        Spacer(Modifier.width(KaijuSpace.xs))
                        trailing()
                    }
                }
            },
        )
        when {
            isError -> Row(Modifier.padding(top = KaijuSpace.xs), verticalAlignment = Alignment.CenterVertically) {
                Icon(KaijuIcons.Warning, null, tint = colors.signal, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(KaijuSpace.xs))
                Text(errorText.orEmpty(), style = Kaiju.type.bodyS, color = colors.signal)
            }
            supportingText != null -> Text(
                supportingText,
                style = Kaiju.type.bodyS,
                color = colors.inkMuted,
                modifier = Modifier.padding(top = KaijuSpace.xs),
            )
        }
    }
}

/**
 * A labeled number control with 48dp minus/plus buttons and a large tabular value.
 * Used for minutes, seconds and rounds so values can be set without the keyboard.
 */
@Composable
fun NumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..999,
    step: Int = 1,
    valueText: (Int) -> String = { it.toString() },
    unitLabel: String? = null,
) {
    val colors = Kaiju.colors
    val text = valueText(value)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Kaiju.type.label, color = colors.ink)
            if (unitLabel != null) Text(unitLabel, style = Kaiju.type.caption, color = colors.inkMuted)
        }
        KaijuIconButton(
            icon = KaijuIcons.Minus,
            contentDescription = "Decrease $label",
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = value > range.first,
            filled = true,
        )
        Text(
            text = text,
            style = Kaiju.type.numeralM,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 76.dp)
                .padding(horizontal = KaijuSpace.xs)
                .semantics { stateDescription = "$label: $text" },
        )
        KaijuIconButton(
            icon = KaijuIcons.Add,
            contentDescription = "Increase $label",
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = value < range.last,
            filled = true,
        )
    }
}

/** A read-only field that opens a picker (date, time, program, lesson). */
@Composable
fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = KaijuIcons.ChevronDown,
    placeholder: String = "Choose",
) {
    val colors = Kaiju.colors
    Column(modifier) {
        Text(label, style = Kaiju.type.label, color = colors.ink)
        Spacer(Modifier.height(KaijuSpace.xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(colors.surface, KaijuShapes.Small)
                .border(1.dp, colors.lineStrong, KaijuShapes.Small)
                .clickable(role = Role.Button, onClickLabel = "Change $label", onClick = onClick)
                .padding(horizontal = KaijuSpace.m, vertical = KaijuSpace.s)
                .semantics(mergeDescendants = true) { contentDescription = "$label: ${value.ifEmpty { placeholder }}" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value.ifEmpty { placeholder },
                style = Kaiju.type.body,
                color = if (value.isEmpty()) colors.inkMuted else colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(icon, null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
        }
    }
}

/** Settings row with a switch; the whole row toggles and is announced as one control. */
@Composable
fun KaijuSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    val colors = Kaiju.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = KaijuSpace.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = colors.inkMuted, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(KaijuSpace.m))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Kaiju.type.titleSmall, color = colors.ink)
            if (subtitle != null) Text(subtitle, style = Kaiju.type.bodyS, color = colors.inkMuted)
        }
        Spacer(Modifier.width(KaijuSpace.m))
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics { },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.ink,
                checkedTrackColor = colors.action,
                checkedBorderColor = colors.action,
                uncheckedThumbColor = colors.inkMuted,
                uncheckedTrackColor = colors.surfaceInset,
                uncheckedBorderColor = colors.lineStrong,
            ),
        )
    }
}

/** Selectable chip. Selected = Ivory text on Facet with an accent border and a check icon. */
@Composable
fun KaijuChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = Kaiju.colors
    Row(
        modifier = modifier
            .heightIn(min = KaijuSpace.touch)
            .selectable(selected = selected, onClick = onClick, role = Role.Checkbox)
            .padding(vertical = 4.dp)
            .background(if (selected) colors.surfaceInset else colors.background, KaijuShapes.Small)
            .border(1.dp, if (selected) colors.accent else colors.lineStrong, KaijuShapes.Small)
            .padding(horizontal = KaijuSpace.s, vertical = KaijuSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val lead = if (selected) KaijuIcons.Check else icon
        if (lead != null) {
            Icon(lead, null, tint = if (selected) colors.accent else colors.inkMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(KaijuSpace.xxs + 2.dp))
        }
        Text(label, style = Kaiju.type.label, color = if (selected) colors.ink else colors.inkMuted, maxLines = 1)
    }
}

@Composable
fun <T> ChipGroup(
    options: List<T>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
        options.forEach { option ->
            KaijuChip(label = label(option), selected = option in selected, onClick = { onToggle(option) })
        }
    }
}

/**
 * Two to four mutually exclusive options. The selected segment is Facet with Ivory text and an
 * accent underline; others are muted. 48dp tall.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kaiju.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.lineStrong, KaijuShapes.Small)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = KaijuSpace.touch)
                    .background(if (selected) colors.surfaceInset else colors.background)
                    .selectable(
                        selected = selected,
                        onClick = { onSelect(index) },
                        role = Role.Tab,
                        interactionSource = null,
                        indication = ripple(color = colors.accent),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.height(3.dp))
                Text(
                    option.uppercase(),
                    style = Kaiju.type.eyebrow,
                    color = if (selected) colors.ink else colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = KaijuSpace.xs, vertical = KaijuSpace.s),
                )
                Box(Modifier.fillMaxWidth().height(3.dp).background(if (selected) colors.accent else colors.background))
            }
        }
    }
}
