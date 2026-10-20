package com.earlthekaiju.coach.feature.timer

import com.earlthekaiju.coach.data.prefs.AppSettings
import com.earlthekaiju.coach.data.prefs.PersistedTimer
import com.earlthekaiju.coach.data.prefs.SettingsRepository
import com.earlthekaiju.coach.domain.time.AppClock
import com.earlthekaiju.coach.domain.timer.TimerCue
import com.earlthekaiju.coach.domain.timer.TimerCues
import com.earlthekaiju.coach.domain.timer.TimerEngine
import com.earlthekaiju.coach.domain.timer.TimerProgram
import com.earlthekaiju.coach.domain.timer.TimerRun
import com.earlthekaiju.coach.domain.timer.TimerSnapshot
import com.earlthekaiju.coach.domain.timer.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Plays round cues. The Android implementation synthesizes tones and vibrates; tests use a fake. */
interface CuePlayer {
    fun play(cue: TimerCue, sound: Boolean, vibration: Boolean)
}

/** Identifies the current device boot, so a saved timer is discarded after a reboot. */
fun interface BootCounter {
    fun current(): Int
}

data class TimerState(
    val snapshot: TimerSnapshot,
    val program: TimerProgram,
    val presetId: String?,
    val presetName: String,
)

/**
 * App-wide training timer. Lives as long as the process, so the timer keeps counting while the
 * instructor moves between Timer, Class Mode and Attendance, and cues keep firing.
 *
 * Only the run's anchors are stored (in memory and DataStore). If Android stops the process,
 * the next launch restores the same run and the countdown is still correct, because it is
 * recomputed from the monotonic clock rather than counted down.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerController(
    private val scope: CoroutineScope,
    private val clock: AppClock,
    private val settingsRepository: SettingsRepository,
    private val cuePlayer: CuePlayer,
    private val bootCounter: BootCounter,
) {
    private data class Selection(val program: TimerProgram, val presetId: String?, val presetName: String)

    private val selection = MutableStateFlow(Selection(TimerProgram.Default, null, "Sparring rounds"))
    private val run = MutableStateFlow<TimerRun?>(null)
    private var settings = AppSettings()

    private val restored = MutableStateFlow(false)
    val isRestored: StateFlow<Boolean> = restored.asStateFlow()

    private val snapshots: Flow<TimerSnapshot> = combine(run, selection) { r, s -> r to s }
        .flatMapLatest { (r, s) ->
            if (r == null) {
                flowOf(TimerEngine.idle(s.program))
            } else {
                flow {
                    while (true) {
                        val snap = TimerEngine.snapshot(r, clock.elapsedRealtime())
                        emit(snap)
                        if (snap.status != TimerStatus.RUNNING) break
                        delay(TimerEngine.millisUntilNextSecond(snap))
                    }
                }
            }
        }

    val state: StateFlow<TimerState> = combine(snapshots, selection) { snap, sel ->
        TimerState(snap, sel.program, sel.presetId, sel.presetName)
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        TimerState(TimerEngine.idle(TimerProgram.Default), TimerProgram.Default, null, "Sparring rounds"),
    )

    init {
        scope.launch { settingsRepository.settings.collect { settings = it } }
        scope.launch { restore() }
        scope.launch {
            var previous: TimerSnapshot? = null
            state.collect { s ->
                val cue = TimerCues.detect(previous, s.snapshot, s.program)
                if (cue != null) cuePlayer.play(cue, settings.timerSound, settings.timerVibration)
                previous = s.snapshot
            }
        }
    }

    private suspend fun restore() {
        val saved = settingsRepository.loadTimer()
        if (saved != null) {
            selection.value = Selection(saved.program, saved.presetId, saved.presetName)
            if (saved.run != null && saved.bootCount == bootCounter.current() && run.value == null) {
                run.value = saved.run
            }
        }
        restored.value = true
    }

    /** Choose what the timer will run. Ignored while a run is active; reset first. */
    fun select(program: TimerProgram, presetId: String?, presetName: String) {
        val current = state.value.snapshot.status
        if (current == TimerStatus.RUNNING || current == TimerStatus.PAUSED) return
        run.value = null
        selection.value = Selection(program, presetId, presetName)
        persist()
    }

    fun start() {
        run.value = TimerEngine.start(selection.value.program, clock.elapsedRealtime())
        persist()
    }

    fun pause() {
        run.value = run.value?.let { TimerEngine.pause(it, clock.elapsedRealtime()) }
        persist()
    }

    fun resume() {
        run.value = run.value?.let { TimerEngine.resume(it, clock.elapsedRealtime()) }
        persist()
    }

    fun reset() {
        run.value = null
        persist()
    }

    /** Main action: start when idle/finished, pause when running, resume when paused. */
    fun toggle() {
        when (state.value.snapshot.status) {
            TimerStatus.IDLE, TimerStatus.FINISHED -> start()
            TimerStatus.RUNNING -> pause()
            TimerStatus.PAUSED -> resume()
        }
    }

    private fun persist() {
        val sel = selection.value
        val r = run.value
        scope.launch {
            settingsRepository.saveTimer(PersistedTimer(r, sel.program, sel.presetId, sel.presetName, bootCounter.current()))
        }
    }
}
