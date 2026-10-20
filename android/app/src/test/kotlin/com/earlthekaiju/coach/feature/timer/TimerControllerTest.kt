package com.earlthekaiju.coach.feature.timer

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.earlthekaiju.coach.data.prefs.SettingsRepository
import com.earlthekaiju.coach.domain.time.AppClock
import com.earlthekaiju.coach.domain.timer.TimerCue
import com.earlthekaiju.coach.domain.timer.TimerPhase
import com.earlthekaiju.coach.domain.timer.TimerProgram
import com.earlthekaiju.coach.domain.timer.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TimerControllerTest {
    @get:Rule val tmp = TemporaryFolder()

    private class Clock(var elapsed: Long) : AppClock {
        override fun now(): Long = 1_792_521_000_000L + elapsed
        override fun elapsedRealtime(): Long = elapsed
        override fun zone(): ZoneId = ZoneId.of("UTC")
    }

    private class RecordingCues : CuePlayer {
        val played = mutableListOf<TimerCue>()
        override fun play(cue: TimerCue, sound: Boolean, vibration: Boolean) { played += cue }
    }

    private val program = TimerProgram(prepSeconds = 0, workSeconds = 60, restSeconds = 10, rounds = 2, warningSeconds = 0)

    private fun TestScope.newController(file: File, clock: Clock, cues: CuePlayer = RecordingCues(), boot: Int = 7): Pair<TimerController, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val controller = TimerController(scope, clock, SettingsRepository(store), cues) { boot }
        return controller to scope
    }

    @Test
    fun `a running timer is restored after the process dies`() = runTest {
        val file = File(tmp.root, "a.preferences_pb")
        val clock = Clock(elapsed = 100_000)
        val (first, scope1) = newController(file, clock)
        advanceUntilIdle()
        first.select(program, presetId = null, presetName = "Test")
        first.start()
        runCurrent()
        clock.elapsed += 30_000
        runCurrent()
        scope1.cancel() // process death
        runCurrent()

        clock.elapsed += 15_000 // time keeps passing while the app is gone
        val (second, scope2) = newController(file, clock)
        runCurrent()
        val snap = second.state.value.snapshot
        assertEquals(TimerStatus.RUNNING, snap.status)
        assertEquals(TimerPhase.WORK, snap.phase)
        assertEquals(15L, snap.remainingSeconds) // 60 s round, 45 s elapsed
        scope2.cancel()
    }

    @Test
    fun `a saved timer is discarded after a reboot`() = runTest {
        val file = File(tmp.root, "b.preferences_pb")
        val clock = Clock(elapsed = 100_000)
        val (first, scope1) = newController(file, clock, boot = 1)
        advanceUntilIdle()
        first.select(program, null, "Test")
        first.start()
        runCurrent()
        scope1.cancel()
        runCurrent()

        clock.elapsed = 5_000 // elapsedRealtime restarts after a reboot
        val (second, scope2) = newController(file, clock, boot = 2)
        runCurrent()
        assertEquals(TimerStatus.IDLE, second.state.value.snapshot.status)
        assertEquals(program, second.state.value.program) // the chosen program is kept
        scope2.cancel()
    }

    @Test
    fun `round start cue plays when work begins`() = runTest {
        val file = File(tmp.root, "c.preferences_pb")
        val clock = Clock(elapsed = 0)
        val cues = RecordingCues()
        val (c, scope) = newController(file, clock, cues)
        advanceUntilIdle()
        c.select(program, null, "Test")
        c.start()
        runCurrent()
        clock.elapsed = 59_500
        testScheduler.advanceTimeBy(59_500)
        runCurrent()
        clock.elapsed = 60_200 // into the rest
        testScheduler.advanceTimeBy(700)
        runCurrent()
        assertTrue("cues: ${cues.played}", TimerCue.ROUND_END in cues.played)
        scope.cancel()
    }
}
