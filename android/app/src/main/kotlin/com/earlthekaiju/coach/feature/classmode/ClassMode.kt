package com.earlthekaiju.coach.feature.classmode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ConfirmDialog
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.FitText
import com.earlthekaiju.coach.designsystem.component.isLargeFont
import com.earlthekaiju.coach.designsystem.component.HairlineDivider
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.LessonSegmentRow
import com.earlthekaiju.coach.designsystem.component.LiveDot
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.SegmentProgressBar
import com.earlthekaiju.coach.designsystem.component.TechniqueTag
import com.earlthekaiju.coach.designsystem.component.indexLabel
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.SessionStatus
import com.earlthekaiju.coach.domain.timer.TimerPhase
import com.earlthekaiju.coach.domain.timer.TimerStatus
import com.earlthekaiju.coach.feature.planner.NoteSheet
import com.earlthekaiju.coach.feature.timer.TimerState
import com.earlthekaiju.coach.navigation.ClassModeRoute
import com.earlthekaiju.coach.ui.Format
import com.earlthekaiju.coach.ui.LocalUse24Hour
import com.earlthekaiju.coach.ui.displayTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime

data class ClassSegmentUi(
    val title: String,
    val minutes: Int,
    val kindLabel: String,
    val techniques: List<String>,
    val notes: String,
)

data class ClassModeUiState(
    val loading: Boolean = true,
    val title: String = "",
    val programName: String? = null,
    val status: SessionStatus = SessionStatus.SCHEDULED,
    val segments: List<ClassSegmentUi> = emptyList(),
    val currentIndex: Int = 0,
    val segmentElapsedMillis: Long = 0,
    val classElapsedMillis: Long = 0,
    val presentCount: Int = 0,
    val expectedCount: Int = 0,
    val wallTime: LocalTime = LocalTime.NOON,
    val timer: TimerState? = null,
    val keepScreenOn: Boolean = true,
    val finished: Boolean = false,
) {
    val current: ClassSegmentUi? get() = segments.getOrNull(currentIndex)
    val isLast: Boolean get() = currentIndex >= segments.lastIndex
    val plannedMinutes: Int get() = segments.sumOf { it.minutes }
    /** Positive = time left in the segment; negative = over. */
    val segmentRemainingMillis: Long get() = (current?.minutes ?: 0) * 60_000L - segmentElapsedMillis
    val segmentFraction: Float
        get() = current?.let { (segmentElapsedMillis.toFloat() / (it.minutes * 60_000f).coerceAtLeast(1f)).coerceIn(0f, 1f) } ?: 0f
}

class ClassModeViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val sessionId = handle.toRoute<ClassModeRoute>().sessionId
    private val finished = MutableStateFlow(false)

    private val ticker = flow {
        while (true) {
            emit(c.clock.now())
            delay(1_000L - c.clock.now() % 1_000L + 5L)
        }
    }

    val state: StateFlow<ClassModeUiState> = combine(
        c.sessions.observeSummary(sessionId),
        c.sessions.observeSegments(sessionId),
        ticker,
        c.timer.state,
        combine(c.settings.settings, finished) { s, f -> s.keepScreenOn to f },
    ) { summary, segments, now, timer, (keepOn, done) ->
        if (summary == null) return@combine ClassModeUiState(loading = false, finished = done)
        val s = summary.session
        ClassModeUiState(
            loading = segments.isEmpty() && s.status != SessionStatus.COMPLETED,
            title = summary.displayTitle,
            programName = summary.programName,
            status = s.status,
            segments = segments.map {
                ClassSegmentUi(it.title, it.minutes, it.kind.label, it.techniqueNames.lines().filter(String::isNotBlank), it.notes)
            },
            currentIndex = s.currentSegmentIndex.coerceIn(0, (segments.size - 1).coerceAtLeast(0)),
            segmentElapsedMillis = s.currentSegmentStartedAt?.let { now - it }?.coerceAtLeast(0) ?: 0,
            classElapsedMillis = s.startedAt?.let { now - it }?.coerceAtLeast(0) ?: 0,
            presentCount = summary.presentCount,
            expectedCount = summary.expectedCount,
            wallTime = Instant.ofEpochMilli(now).atZone(c.clock.zone()).toLocalTime(),
            timer = timer.takeIf { it.snapshot.status == TimerStatus.RUNNING || it.snapshot.status == TimerStatus.PAUSED },
            keepScreenOn = keepOn,
            finished = done,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClassModeUiState())

    init {
        viewModelScope.launch { c.sessions.start(sessionId) }
    }

    fun goTo(index: Int) = viewModelScope.launch { c.sessions.goToSegment(sessionId, index) }
    fun next() = goTo(state.value.currentIndex + 1)
    fun previous() = goTo(state.value.currentIndex - 1)
    fun addNote(text: String) = viewModelScope.launch { c.sessions.addNote(sessionId, text) }
    fun finish() = viewModelScope.launch {
        c.sessions.finish(sessionId)
        finished.value = true
    }
}

data class ClassModeActions(
    val onLeave: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onFinish: () -> Unit = {},
    val onAttendance: () -> Unit = {},
    val onTimer: () -> Unit = {},
    val onAddNote: (String) -> Unit = {},
)

@Composable
fun ClassModeDestination(onLeave: () -> Unit, onFinished: (String) -> Unit, onAttendance: (String) -> Unit, onTimer: (String) -> Unit) {
    val vm = kaijuViewModel { c, h -> ClassModeViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onFinished(vm.sessionId) }
    KeepScreenOn(state.keepScreenOn)
    ClassModeScreen(
        state,
        ClassModeActions(
            onLeave = onLeave,
            onNext = { vm.next() },
            onPrevious = { vm.previous() },
            onFinish = { vm.finish() },
            onAttendance = { onAttendance(vm.sessionId) },
            onTimer = { onTimer(vm.sessionId) },
            onAddNote = { vm.addNote(it) },
        ),
    )
}

/** Keeps the display awake while this screen is shown (Class Mode, Timer). */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val previous = view.keepScreenOn
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = previous }
    }
}

@Composable
fun ClassModeScreen(state: ClassModeUiState, actions: ClassModeActions) {
    val colors = Kaiju.colors
    var confirmFinish by remember { mutableStateOf(false) }
    var noteSheet by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        // A thin live band across the top marks the immersive mode.
        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(3.dp).background(colors.live))
        ClassModeTopBar(state, actions.onLeave, onFinish = { confirmFinish = true })
        if (state.loading) {
            LoadingState("Starting class", Modifier.padding(KaijuSpace.gutter))
            return@Column
        }
        Column(Modifier.padding(horizontal = KaijuSpace.gutter)) {
            SegmentProgressBar(
                minutes = state.segments.map { it.minutes },
                currentIndex = state.currentIndex,
                currentFraction = state.segmentFraction,
                height = 8.dp,
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 600.dp && maxWidth > maxHeight
            if (wide) {
                Row(Modifier.fillMaxSize().padding(horizontal = KaijuSpace.gutter)) {
                    Column(Modifier.weight(1.3f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        SegmentHero(state, landscape = true)
                    }
                    Spacer(Modifier.width(KaijuSpace.l))
                    // Controls stay pinned at the bottom of the right column; only Up Next scrolls.
                    // On a real landscape phone the status and navigation bars leave ~260dp, and the
                    // controls must never fall below the fold.
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = KaijuSpace.m)) {
                            UpNext(state)
                        }
                        Spacer(Modifier.height(KaijuSpace.s))
                        state.timer?.let {
                            MiniTimer(it, actions.onTimer)
                            Spacer(Modifier.height(KaijuSpace.s))
                        }
                        ClassControls(state, actions, onFinish = { confirmFinish = true }, onNote = { noteSheet = true })
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KaijuSpace.gutter),
                    ) {
                        SegmentHero(state, landscape = false)
                        Spacer(Modifier.height(KaijuSpace.l))
                        UpNext(state)
                        Spacer(Modifier.height(KaijuSpace.m))
                    }
                    Column(Modifier.padding(horizontal = KaijuSpace.gutter)) {
                        state.timer?.let {
                            MiniTimer(it, actions.onTimer)
                            Spacer(Modifier.height(KaijuSpace.s))
                        }
                        ClassControls(state, actions, onFinish = { confirmFinish = true }, onNote = { noteSheet = true })
                    }
                }
            }
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(KaijuSpace.s))
    }
    if (confirmFinish) {
        ConfirmDialog(
            title = "Finish class?",
            body = "Marks the class complete now. Next you can review attendance and save notes.",
            confirmLabel = "Finish class",
            dismissLabel = "Keep teaching",
            onConfirm = { confirmFinish = false; actions.onFinish() },
            onDismiss = { confirmFinish = false },
        )
    }
    if (noteSheet) {
        NoteSheet(onSave = { actions.onAddNote(it); noteSheet = false }, onDismiss = { noteSheet = false }, title = "Quick note")
    }
}

@Composable
private fun ClassModeTopBar(state: ClassModeUiState, onLeave: () -> Unit, onFinish: () -> Unit) {
    val colors = Kaiju.colors
    val use24h = LocalUse24Hour.current
    val large = isLargeFont()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = KaijuSpace.xxs, end = KaijuSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KaijuIconButton(KaijuIcons.Close, "Leave Class Mode (the class keeps running)", onLeave)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(size = 9.dp)
                Spacer(Modifier.width(KaijuSpace.xs))
                Eyebrow(if (large) "Live" else "Live · Class mode", color = colors.live)
            }
            // Large text: shrink the name to one line rather than breaking a word across two.
            if (large) {
                FitText(state.title, style = Kaiju.type.titleSmall, color = colors.ink, minSize = 14.sp)
            } else {
                Text(state.title, style = Kaiju.type.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = KaijuSpace.xs)) {
            Text(Format.time(state.wallTime, use24h), style = Kaiju.type.numeralM.copy(fontSize = 22.sp), color = colors.ink)
            Text(
                // Large text: drop the wide-tracked "CLASS" label so the class name keeps its room.
                (if (large) "" else "CLASS ") + Format.clock(state.classElapsedMillis / 1000),
                style = Kaiju.type.eyebrow,
                color = colors.inkMuted,
                modifier = Modifier.semantics { contentDescription = "Class running for ${state.classElapsedMillis / 60_000} minutes" },
            )
        }
        KaijuButton("Finish", onFinish, variant = ButtonVariant.Ghost, size = ButtonSize.Compact)
    }
}

@Composable
private fun SegmentHero(state: ClassModeUiState, landscape: Boolean) {
    val colors = Kaiju.colors
    val seg = state.current ?: return
    val remaining = state.segmentRemainingMillis
    val over = remaining < 0
    Column(Modifier.fillMaxWidth().padding(top = if (landscape) KaijuSpace.s else KaijuSpace.l)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(indexLabel(state.currentIndex + 1), style = if (landscape) Kaiju.type.displayM else Kaiju.type.displayL, color = colors.live)
            Text(
                " / ${indexLabel(state.segments.size)}",
                style = Kaiju.type.title,
                color = colors.inkMuted,
                modifier = Modifier.padding(bottom = if (landscape) 2.dp else 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Eyebrow(seg.kindLabel, color = colors.accent, modifier = Modifier.padding(bottom = if (landscape) 4.dp else 10.dp))
        }
        Text(
            seg.title.uppercase(),
            style = Kaiju.type.displayM,
            maxLines = if (landscape) 2 else 3,
            overflow = TextOverflow.Ellipsis,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(if (landscape) KaijuSpace.xs else KaijuSpace.m))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = if (over) "${-remaining / 60_000} minutes over" else "${(remaining + 59_999) / 60_000} minutes left in segment"
            },
        ) {
            Text(
                (if (over) "+" else "") + Format.clock(if (over) -remaining / 1000 else (remaining + 999) / 1000),
                style = Kaiju.type.numeralHero.copy(fontSize = if (landscape) 96.sp else 104.sp, lineHeight = if (landscape) 96.sp else 104.sp),
                color = if (over) colors.gold else colors.ink,
            )
            Spacer(Modifier.width(KaijuSpace.s))
            Column(Modifier.padding(bottom = 14.dp)) {
                Eyebrow(if (over) "Over" else "Left", color = if (over) colors.gold else colors.inkMuted)
                Text("of ${seg.minutes} min", style = Kaiju.type.caption, color = colors.inkMuted)
            }
        }
        if (seg.techniques.isNotEmpty()) {
            Spacer(Modifier.height(KaijuSpace.s))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs), verticalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
                seg.techniques.forEach { TechniqueTag(it) }
            }
        }
        if (seg.notes.isNotBlank()) {
            Spacer(Modifier.height(KaijuSpace.m))
            Row {
                Box(Modifier.width(3.dp).height(IntrinsicLineHeight).background(colors.gold))
                Spacer(Modifier.width(KaijuSpace.s))
                Text(seg.notes, style = Kaiju.type.bodyL, color = colors.ink)
            }
        }
    }
}

private val IntrinsicLineHeight = 28.dp

@Composable
private fun UpNext(state: ClassModeUiState) {
    val colors = Kaiju.colors
    val upcoming = state.segments.drop(state.currentIndex + 1)
    Column {
        Eyebrow(if (upcoming.isEmpty()) "Last segment" else "Up next", color = colors.inkMuted)
        Spacer(Modifier.height(KaijuSpace.xxs))
        if (upcoming.isEmpty()) {
            Text("Finish when you're ready — attendance and notes come next.", style = Kaiju.type.bodyS, color = colors.inkMuted)
        }
        upcoming.forEachIndexed { i, seg ->
            LessonSegmentRow(
                index = state.currentIndex + 2 + i,
                title = seg.title,
                minutes = seg.minutes,
                compact = i > 0,
                kindLabel = seg.kindLabel.takeIf { i == 0 },
            )
            if (i < upcoming.lastIndex) HairlineDivider(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ClassControls(state: ClassModeUiState, actions: ClassModeActions, onFinish: () -> Unit, onNote: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s), verticalAlignment = Alignment.CenterVertically) {
            KaijuIconButton(
                KaijuIcons.SkipPrevious,
                "Previous segment",
                actions.onPrevious,
                enabled = state.currentIndex > 0,
                size = KaijuSpace.touchLarge,
                iconSize = 28.dp,
                filled = true,
            )
            if (state.isLast) {
                KaijuButton("Finish class", onFinish, Modifier.weight(1f), size = ButtonSize.Hero, icon = KaijuIcons.Flag)
            } else {
                KaijuButton("Next segment", actions.onNext, Modifier.weight(1f), size = ButtonSize.Hero, icon = KaijuIcons.SkipNext)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
            QuickAction("Attendance", "${state.presentCount}/${state.expectedCount}", KaijuIcons.Attendance, actions.onAttendance, Modifier.weight(1f))
            QuickAction("Timer", null, KaijuIcons.Timer, actions.onTimer, Modifier.weight(1f))
            QuickAction("Note", null, KaijuIcons.Note, onNote, Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickAction(label: String, value: String?, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    val colors = Kaiju.colors
    Column(
        modifier
            .heightIn(min = 64.dp)
            .border(1.dp, colors.lineStrong, KaijuShapes.Action)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(vertical = KaijuSpace.xs)
            .semantics(mergeDescendants = true) { contentDescription = if (value != null) "$label, $value" else label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.ink, modifier = Modifier.size(22.dp))
            if (value != null) {
                Spacer(Modifier.width(6.dp))
                Text(value, style = Kaiju.type.numeralM.copy(fontSize = 20.sp, lineHeight = 22.sp), color = colors.ink)
            }
        }
        FitText(label.uppercase(), style = Kaiju.type.eyebrow, color = colors.inkMuted, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

/** A compact strip showing the running round timer; tap to open the full timer. */
@Composable
fun MiniTimer(timer: TimerState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    val s = timer.snapshot
    val paused = s.status == TimerStatus.PAUSED
    val work = s.phase == TimerPhase.WORK && !paused
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (work) colors.phaseWork else colors.surfaceInset, KaijuShapes.Small)
            .clickable(role = Role.Button, onClickLabel = "Open timer", onClick = onClick)
            .padding(horizontal = KaijuSpace.m)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(KaijuIcons.Timer, null, tint = colors.ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(KaijuSpace.s))
        Text(
            (if (paused) "PAUSED" else s.phase.label.uppercase()) + " · ROUND ${s.round}/${s.rounds}",
            style = Kaiju.type.eyebrow,
            color = if (s.phase == TimerPhase.REST && !paused) colors.gold else colors.ink,
            modifier = Modifier.weight(1f),
        )
        Text(Format.clock(s.remainingSeconds), style = Kaiju.type.numeralM, color = colors.ink)
    }
}
