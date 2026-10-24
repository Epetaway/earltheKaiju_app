package com.earlthekaiju.coach.feature.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.earlthekaiju.coach.data.db.TimerPresetEntity
import com.earlthekaiju.coach.data.repo.TimerPresetDraft
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.KaijuBottomSheet
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuChip
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.NumberStepper
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.timer.TimerPhase
import com.earlthekaiju.coach.domain.timer.TimerProgram
import com.earlthekaiju.coach.domain.timer.TimerStatus
import com.earlthekaiju.coach.feature.classmode.KeepScreenOn
import com.earlthekaiju.coach.ui.Format
import com.earlthekaiju.coach.ui.LocalUse24Hour
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class TimerUiState(
    val timer: TimerState,
    val presets: List<TimerPresetEntity> = emptyList(),
    val wallTime: LocalTime = LocalTime.NOON,
    val sound: Boolean = true,
    val keepScreenOn: Boolean = true,
)

class TimerViewModel(private val c: AppContainer) : ViewModel() {
    private val wall = flow {
        while (true) {
            emit(c.clock.localDateTime().toLocalTime())
            delay(1_000L - c.clock.now() % 1_000L + 5L)
        }
    }

    val state: StateFlow<TimerUiState> = combine(c.timer.state, c.timerPresets.observeAll(), wall, c.settings.settings) { t, presets, now, s ->
        TimerUiState(timer = t, presets = presets, wallTime = now, sound = s.timerSound, keepScreenOn = s.keepScreenOn)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState(c.timer.state.value))

    fun toggle() = c.timer.toggle()
    fun reset() = c.timer.reset()
    fun select(preset: TimerPresetEntity) = c.timer.select(preset.toProgram(), preset.id, preset.name)
    fun useCustom(draft: TimerPresetDraft) = c.timer.select(draft.toProgram(), null, draft.name.ifBlank { "Custom" })
    fun savePreset(draft: TimerPresetDraft) = viewModelScope.launch {
        val id = c.timerPresets.save(draft)
        c.timer.select(draft.toProgram(), id, draft.name.ifBlank { "Custom" })
    }
    fun deletePreset(id: String) = viewModelScope.launch { c.timerPresets.delete(id) }
    fun setSound(on: Boolean) = viewModelScope.launch { c.settings.setTimerSound(on) }
}

fun TimerPresetEntity.toProgram() = TimerProgram(prepSeconds, workSeconds, restSeconds, rounds, warningSeconds)
fun TimerPresetDraft.toProgram() = TimerProgram(prepSeconds, workSeconds, restSeconds, rounds, warningSeconds)

data class TimerActions(
    val onBack: (() -> Unit)? = null,
    val onToggle: () -> Unit = {},
    val onReset: () -> Unit = {},
    val onSelect: (TimerPresetEntity) -> Unit = {},
    val onUseCustom: (TimerPresetDraft) -> Unit = {},
    val onSavePreset: (TimerPresetDraft) -> Unit = {},
    val onDeletePreset: (String) -> Unit = {},
    val onSound: (Boolean) -> Unit = {},
)

@Composable
fun TimerDestination(onBack: (() -> Unit)? = null) {
    val vm = kaijuViewModel { c, _ -> TimerViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val running = state.timer.snapshot.status == TimerStatus.RUNNING || state.timer.snapshot.status == TimerStatus.PAUSED
    KeepScreenOn(state.keepScreenOn && running)
    TimerScreen(
        state,
        TimerActions(
            onBack = onBack,
            onToggle = vm::toggle,
            onReset = vm::reset,
            onSelect = vm::select,
            onUseCustom = vm::useCustom,
            onSavePreset = { vm.savePreset(it) },
            onDeletePreset = { vm.deletePreset(it) },
            onSound = { vm.setSound(it) },
        ),
    )
}

private data class PhaseLook(val field: Color, val label: String, val labelColor: Color, val numerals: Color)

@Composable
private fun phaseLook(state: TimerState): PhaseLook {
    val colors = Kaiju.colors
    val s = state.snapshot
    return when (s.status) {
        TimerStatus.IDLE -> PhaseLook(colors.background, "Ready", colors.inkMuted, colors.ink)
        TimerStatus.PAUSED -> PhaseLook(colors.background, "Paused", colors.gold, colors.inkMuted)
        TimerStatus.FINISHED -> PhaseLook(colors.background, "Time", colors.gold, colors.gold)
        TimerStatus.RUNNING -> when (s.phase) {
            TimerPhase.WORK -> PhaseLook(colors.phaseWork, "Train", colors.ink, colors.ink)
            TimerPhase.REST -> PhaseLook(colors.phaseRest, "Rest", colors.gold, colors.ink)
            TimerPhase.PREP -> PhaseLook(colors.surfaceInset, "Get ready", colors.ink, colors.ink)
        }
    }
}

@Composable
fun TimerScreen(state: TimerUiState, actions: TimerActions) {
    val colors = Kaiju.colors
    var editor by remember { mutableStateOf<TimerPresetDraft?>(null) }
    val look = phaseLook(state.timer)
    val field by animateColorAsState(look.field, animationSpec = tween(if (Kaiju.reduceMotion) 0 else 350), label = "phaseField")

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        val wide = maxWidth > maxHeight && maxWidth >= 560.dp
        if (wide) {
            Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                PhaseField(state, look, field, Modifier.weight(1.6f).fillMaxHeight())
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(KaijuSpace.m),
                    verticalArrangement = Arrangement.spacedBy(KaijuSpace.m),
                ) {
                    TimerHeader(state, actions)
                    TimerControls(state, actions)
                    PresetPicker(state, actions, onCustom = { editor = it })
                }
            }
        } else {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                TimerHeader(state, actions, Modifier.padding(horizontal = KaijuSpace.gutter))
                PhaseField(state, look, field, Modifier.weight(1f).fillMaxWidth())
                Column(Modifier.padding(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.m), verticalArrangement = Arrangement.spacedBy(KaijuSpace.m)) {
                    TimerControls(state, actions)
                    PresetPicker(state, actions, onCustom = { editor = it })
                }
            }
        }
    }
    editor?.let { draft ->
        PresetEditorSheet(
            initial = draft,
            onUseOnce = { actions.onUseCustom(it); editor = null },
            onSave = { actions.onSavePreset(it); editor = null },
            onDelete = draft.id?.let { id -> { actions.onDeletePreset(id); editor = null } },
            onDismiss = { editor = null },
        )
    }
}

@Composable
private fun TimerHeader(state: TimerUiState, actions: TimerActions, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    val use24h = LocalUse24Hour.current
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        actions.onBack?.let { KaijuIconButton(KaijuIcons.Back, "Back to class", it) }
        Column(Modifier.weight(1f)) {
            Eyebrow("Timer")
            Text(state.timer.presetName, style = Kaiju.type.title, color = colors.ink, maxLines = 1, modifier = Modifier.semantics { heading() })
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Format.time(state.wallTime, use24h), style = Kaiju.type.numeralM, color = colors.ink)
            Eyebrow("Clock", color = colors.inkMuted)
        }
        Spacer(Modifier.width(KaijuSpace.xs))
        KaijuIconButton(
            if (state.sound) KaijuIcons.SoundOn else KaijuIcons.SoundOff,
            if (state.sound) "Mute round sounds" else "Turn on round sounds",
            { actions.onSound(!state.sound) },
            tint = if (state.sound) colors.ink else colors.inkMuted,
        )
    }
}

/** The hero: a full-bleed field whose color, label and pattern all change with the phase. */
@Composable
private fun PhaseField(state: TimerUiState, look: PhaseLook, field: Color, modifier: Modifier) {
    val colors = Kaiju.colors
    val s = state.timer.snapshot
    val paused = s.status == TimerStatus.PAUSED
    Box(modifier.background(field).clipToBounds()) {
        if (paused) PauseHatch(colors.surfaceInset)
        if (s.status == TimerStatus.RUNNING && s.phase == TimerPhase.REST) {
            Box(Modifier.fillMaxWidth().height(6.dp).background(colors.gold))
        }
        if (s.inWarning && s.status == TimerStatus.RUNNING) {
            Box(Modifier.fillMaxSize().border(6.dp, colors.gold))
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = KaijuSpace.m, vertical = KaijuSpace.m)) {
            val text = Format.clock(s.remainingSeconds)
            val widthEm = text.count { it.isDigit() } * 0.52f + text.count { it == ':' } * 0.30f + 0.06f
            val density = LocalDensity.current
            val byWidth = maxWidth * 0.94f / widthEm
            val byHeight = maxHeight * 0.52f
            val sizeDp: Dp = minOf(byWidth, byHeight).coerceAtLeast(48.dp)
            val fontSize = with(density) { sizeDp.toSp() }
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    look.label.uppercase(),
                    style = Kaiju.type.displayM,
                    color = look.labelColor,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        heading()
                    },
                )
                Text(
                    if (s.status == TimerStatus.IDLE) "${s.rounds} ROUNDS" else "ROUND ${s.round} OF ${s.rounds}",
                    style = Kaiju.type.eyebrow.copy(fontSize = 15.sp),
                    color = if (field == colors.phaseWork) colors.ink else colors.inkMuted,
                )
                Text(
                    text,
                    style = Kaiju.type.numeralHero.copy(fontSize = fontSize, lineHeight = fontSize),
                    color = look.numerals,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.semantics {
                        contentDescription = "${s.remainingSeconds / 60} minutes ${s.remainingSeconds % 60} seconds"
                    },
                )
                ProgressLine(s.intervalProgress, if (field == colors.phaseWork) colors.ink else colors.accent, Modifier.fillMaxWidth(0.7f))
                Spacer(Modifier.height(KaijuSpace.s))
                val next = s.next
                val nextText = when {
                    s.status == TimerStatus.FINISHED -> "All rounds complete"
                    s.inWarning -> "Last ${state.timer.program.warningSeconds} seconds"
                    next == null -> "Final round"
                    else -> "Next · ${next.phase.label} ${Format.clock(next.durationMillis / 1000)}"
                }
                // Gold on Deep Emerald is only 3.8:1, so warning text stays Ivory; the gold border carries the cue.
                Eyebrow(nextText, color = if (field == colors.phaseWork) colors.ink else if (s.inWarning) colors.gold else colors.inkMuted)
            }
        }
    }
}

@Composable
private fun ProgressLine(progress: Float, color: Color, modifier: Modifier) {
    val track = Kaiju.colors.line
    Canvas(modifier.height(4.dp).clearAndSetSemantics { }) {
        drawRect(track)
        drawRect(color, size = size.copy(width = size.width * progress.coerceIn(0f, 1f)))
    }
}

/** Diagonal hatching: "paused" reads at a glance, without relying on color. */
@Composable
private fun PauseHatch(color: Color) {
    Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
        val step = 28.dp.toPx()
        val stroke = 6.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = stroke)
            x += step
        }
    }
}

/** Big transport controls: reset on the side, the one main action dominant. */
@Composable
fun TimerControls(state: TimerUiState, actions: TimerActions, modifier: Modifier = Modifier) {
    val s = state.timer.snapshot
    val (label, icon) = when (s.status) {
        TimerStatus.IDLE -> "Start" to KaijuIcons.Play
        TimerStatus.RUNNING -> "Pause" to KaijuIcons.Pause
        TimerStatus.PAUSED -> "Resume" to KaijuIcons.Play
        TimerStatus.FINISHED -> "Start again" to KaijuIcons.Reset
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s), verticalAlignment = Alignment.CenterVertically) {
        KaijuIconButton(
            KaijuIcons.Reset,
            "Reset timer",
            actions.onReset,
            enabled = s.status != TimerStatus.IDLE,
            size = KaijuSpace.touchLarge,
            iconSize = 28.dp,
            filled = true,
        )
        KaijuButton(
            label,
            actions.onToggle,
            Modifier.weight(1f),
            size = ButtonSize.Hero,
            icon = icon,
            variant = if (s.status == TimerStatus.RUNNING) ButtonVariant.Secondary else ButtonVariant.Primary,
        )
    }
}

@Composable
private fun PresetPicker(state: TimerUiState, actions: TimerActions, onCustom: (TimerPresetDraft) -> Unit) {
    val colors = Kaiju.colors
    val locked = state.timer.snapshot.status == TimerStatus.RUNNING || state.timer.snapshot.status == TimerStatus.PAUSED
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Presets", color = colors.inkMuted, modifier = Modifier.weight(1f))
            if (locked) Text("Reset to change", style = Kaiju.type.caption, color = colors.inkMuted)
        }
        Spacer(Modifier.height(KaijuSpace.xxs))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            state.presets.forEach { p ->
                KaijuChip(
                    "${p.name} · ${p.rounds}×${Format.secondsLabel(p.workSeconds)}",
                    selected = state.timer.presetId == p.id,
                    onClick = { if (!locked) actions.onSelect(p) },
                )
            }
            KaijuChip(
                "Custom…",
                selected = state.timer.presetId == null && state.timer.snapshot.status == TimerStatus.IDLE,
                icon = KaijuIcons.Edit,
                onClick = {
                    if (!locked) {
                        val current = state.timer.program
                        val preset = state.presets.firstOrNull { it.id == state.timer.presetId }
                        onCustom(
                            TimerPresetDraft(
                                id = preset?.id,
                                name = preset?.name ?: "",
                                prepSeconds = current.prepSeconds,
                                workSeconds = current.workSeconds,
                                restSeconds = current.restSeconds,
                                rounds = current.rounds,
                                warningSeconds = current.warningSeconds,
                            ),
                        )
                    }
                },
            )
        }
        Text(
            "${state.timer.program.rounds} × ${Format.secondsLabel(state.timer.program.workSeconds)} train · " +
                "${Format.secondsLabel(state.timer.program.restSeconds)} rest · ${Format.clock(state.timer.program.totalMillis / 1000)} total",
            style = Kaiju.type.caption,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = KaijuSpace.xxs),
        )
    }
}

@Composable
private fun PresetEditorSheet(
    initial: TimerPresetDraft,
    onUseOnce: (TimerPresetDraft) -> Unit,
    onSave: (TimerPresetDraft) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var d by remember { mutableStateOf(initial) }
    KaijuBottomSheet(onDismiss = onDismiss, title = if (initial.id == null) "Custom timer" else "Edit preset", eyebrow = "Timer") {
        KaijuTextField(
            value = d.name,
            onValueChange = { d = d.copy(name = it) },
            label = "Preset name",
            placeholder = "e.g. Competition rounds",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        Spacer(Modifier.height(KaijuSpace.s))
        NumberStepper("Rounds", d.rounds, { d = d.copy(rounds = it) }, range = 1..40)
        NumberStepper("Round length", d.workSeconds, { d = d.copy(workSeconds = it) }, range = 15..3600, step = 15, valueText = Format::secondsLabel, unitLabel = "min:sec, steps of 15s")
        NumberStepper("Rest", d.restSeconds, { d = d.copy(restSeconds = it) }, range = 0..600, step = 5, valueText = Format::secondsLabel, unitLabel = "between rounds")
        NumberStepper("Get ready", d.prepSeconds, { d = d.copy(prepSeconds = it) }, range = 0..60, step = 5, valueText = Format::secondsLabel, unitLabel = "before round 1")
        NumberStepper("Warning", d.warningSeconds, { d = d.copy(warningSeconds = it) }, range = 0..120, step = 5, valueText = { if (it == 0) "Off" else Format.secondsLabel(it) }, unitLabel = "before a round ends")
        Spacer(Modifier.height(KaijuSpace.m))
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
            KaijuButton("Use once", { onUseOnce(d) }, Modifier.weight(1f), variant = ButtonVariant.Secondary)
            KaijuButton(if (initial.id == null) "Save preset" else "Save", { onSave(d) }, Modifier.weight(1f), icon = KaijuIcons.Check)
        }
        if (onDelete != null) {
            Spacer(Modifier.height(KaijuSpace.s))
            KaijuButton("Delete preset", onDelete, Modifier.fillMaxWidth(), variant = ButtonVariant.Danger, icon = KaijuIcons.Delete)
        }
    }
}
