package com.earlthekaiju.coach.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

data class PickerOption(val id: String?, val title: String, val subtitle: String? = null)

/** A bottom sheet listing options as large radio rows. */
@Composable
fun OptionSheet(
    title: String,
    options: List<PickerOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    footer: (@Composable () -> Unit)? = null,
) {
    val colors = Kaiju.colors
    KaijuBottomSheet(onDismiss = onDismiss, title = title) {
        Column(Modifier.selectableGroup()) {
            options.forEach { option ->
                val selected = option.id == selectedId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(option.id) })
                        .padding(vertical = KaijuSpace.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(option.title, style = Kaiju.type.titleSmall, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        option.subtitle?.let { Text(it, style = Kaiju.type.bodyS, color = colors.inkMuted, maxLines = 2) }
                    }
                    if (selected) {
                        Icon(KaijuIcons.Check, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                    } else {
                        Spacer(Modifier.size(24.dp).border(1.dp, colors.lineStrong, KaijuShapes.Small))
                    }
                }
                HairlineDivider(Modifier.fillMaxWidth())
            }
        }
        if (footer != null) {
            Spacer(Modifier.height(KaijuSpace.m))
            footer()
        }
    }
}

/** Material 3 date picker in brand colors. Dates travel as LocalDate; the picker works in UTC millis. */
@Composable
fun KaijuDatePickerDialog(initial: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val colors = Kaiju.colors
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            KaijuButton("Set date", {
                state.selectedDateMillis?.let { onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }, variant = ButtonVariant.Ghost, size = ButtonSize.Compact)
        },
        dismissButton = { KaijuButton("Cancel", onDismiss, variant = ButtonVariant.Ghost, size = ButtonSize.Compact) },
        shape = KaijuShapes.Dialog,
        colors = DatePickerDefaults.colors(containerColor = colors.surface),
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = colors.surface,
                titleContentColor = colors.inkMuted,
                headlineContentColor = colors.ink,
                weekdayContentColor = colors.inkMuted,
                dayContentColor = colors.ink,
                selectedDayContainerColor = colors.accent,
                selectedDayContentColor = colors.background,
                todayContentColor = colors.gold,
                todayDateBorderColor = colors.gold,
            ),
        )
    }
}

@Composable
fun KaijuTimePickerDialog(initial: LocalTime, is24Hour: Boolean, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val colors = Kaiju.colors
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24Hour)
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(colors.surface, KaijuShapes.Dialog)
                .border(1.dp, colors.lineStrong, KaijuShapes.Dialog)
                .padding(KaijuSpace.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Start time", style = Kaiju.type.heading, color = colors.ink, modifier = Modifier.fillMaxWidth().semantics { heading() })
            Spacer(Modifier.height(KaijuSpace.m))
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = colors.surfaceInset,
                    selectorColor = colors.accent,
                    containerColor = colors.surface,
                    timeSelectorSelectedContainerColor = colors.actionPressed,
                    timeSelectorUnselectedContainerColor = colors.surfaceInset,
                    timeSelectorSelectedContentColor = colors.ink,
                    timeSelectorUnselectedContentColor = colors.inkMuted,
                    periodSelectorSelectedContainerColor = colors.actionPressed,
                    periodSelectorSelectedContentColor = colors.ink,
                    periodSelectorUnselectedContentColor = colors.inkMuted,
                    periodSelectorBorderColor = colors.lineStrong,
                    clockDialSelectedContentColor = colors.background,
                    clockDialUnselectedContentColor = colors.ink,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
                KaijuButton("Cancel", onDismiss, Modifier.weight(1f), variant = ButtonVariant.Secondary)
                KaijuButton("Set time", { onConfirm(LocalTime.of(state.hour, state.minute)) }, Modifier.weight(1f))
            }
        }
    }
}
