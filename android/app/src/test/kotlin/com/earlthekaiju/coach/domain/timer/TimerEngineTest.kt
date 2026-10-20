package com.earlthekaiju.coach.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {
    private val program = TimerProgram(prepSeconds = 10, workSeconds = 300, restSeconds = 60, rounds = 3, warningSeconds = 30)
    private val t0 = 1_000_000L

    @Test
    fun `intervals are prep then work and rest with no rest after the last round`() {
        val phases = program.intervals.map { it.phase to it.round }
        assertEquals(
            listOf(
                TimerPhase.PREP to 1, TimerPhase.WORK to 1, TimerPhase.REST to 1,
                TimerPhase.WORK to 2, TimerPhase.REST to 2, TimerPhase.WORK to 3,
            ),
            phases,
        )
        assertEquals((10 + 300 * 3 + 60 * 2) * 1000L, program.totalMillis)
    }

    @Test
    fun `no prep and no rest gives back-to-back work rounds`() {
        val p = TimerProgram(prepSeconds = 0, workSeconds = 60, restSeconds = 0, rounds = 3)
        assertEquals(List(3) { TimerPhase.WORK }, p.intervals.map { it.phase })
        assertEquals(180_000L, p.totalMillis)
    }

    @Test
    fun `remaining time is derived from the clock, not counted down`() {
        val run = TimerEngine.start(program, t0)
        // 4.3 s into the 10 s prep: 5.7 s remain, displayed as 6.
        val s = TimerEngine.snapshot(run, t0 + 4_300)
        assertEquals(TimerPhase.PREP, s.phase)
        assertEquals(5_700L, s.remainingMillis)
        assertEquals(6L, s.remainingSeconds)
        // Jumping far ahead without any intermediate ticks is still exact.
        val later = TimerEngine.snapshot(run, t0 + 10_000 + 300_000 + 15_000)
        assertEquals(TimerPhase.REST, later.phase)
        assertEquals(1, later.round)
        assertEquals(45_000L, later.remainingMillis)
    }

    @Test
    fun `display shows whole seconds rounded up so zero means done`() {
        val run = TimerEngine.start(program, t0)
        assertEquals(10L, TimerEngine.snapshot(run, t0).remainingSeconds)
        assertEquals(10L, TimerEngine.snapshot(run, t0 + 1).remainingSeconds)
        assertEquals(9L, TimerEngine.snapshot(run, t0 + 1_000).remainingSeconds)
    }

    @Test
    fun `pause freezes the countdown and resume continues exactly`() {
        var run = TimerEngine.start(program, t0)
        run = TimerEngine.pause(run, t0 + 20_000)
        val paused = TimerEngine.snapshot(run, t0 + 80_000)
        assertEquals(TimerStatus.PAUSED, paused.status)
        assertEquals(TimerPhase.WORK, paused.phase)
        assertEquals(290_000L, paused.remainingMillis)

        run = TimerEngine.resume(run, t0 + 80_000)
        val resumed = TimerEngine.snapshot(run, t0 + 81_000)
        assertEquals(TimerStatus.RUNNING, resumed.status)
        assertEquals(289_000L, resumed.remainingMillis)
    }

    @Test
    fun `pausing twice or resuming while running changes nothing`() {
        val run = TimerEngine.start(program, t0)
        assertEquals(run, TimerEngine.resume(run, t0 + 5_000))
        val paused = TimerEngine.pause(run, t0 + 1_000)
        assertEquals(paused, TimerEngine.pause(paused, t0 + 9_000))
    }

    @Test
    fun `finishes after the last round and reports zero`() {
        val run = TimerEngine.start(program, t0)
        val s = TimerEngine.snapshot(run, t0 + program.totalMillis + 5_000)
        assertEquals(TimerStatus.FINISHED, s.status)
        assertEquals(0L, s.remainingMillis)
        assertNull(s.next)
    }

    @Test
    fun `warning applies only to the end of work rounds`() {
        val run = TimerEngine.start(program, t0)
        val workStart = t0 + 10_000
        assertFalse(TimerEngine.snapshot(run, workStart + 269_000).inWarning)
        assertTrue(TimerEngine.snapshot(run, workStart + 270_000).inWarning)
        assertFalse(TimerEngine.snapshot(run, workStart + 300_000 + 50_000).inWarning) // rest
    }

    @Test
    fun `next tick lands on the next displayed second`() {
        val run = TimerEngine.start(program, t0)
        assertEquals(300L, TimerEngine.millisUntilNextSecond(TimerEngine.snapshot(run, t0 + 4_700)))
        assertEquals(1_000L, TimerEngine.millisUntilNextSecond(TimerEngine.snapshot(run, t0 + 5_000)))
    }

    @Test
    fun `cues fire once at each transition in order`() {
        val run = TimerEngine.start(program, 0)
        var previous: TimerSnapshot? = null
        val cues = mutableListOf<Pair<TimerCue, Long>>()
        var t = 0L
        while (t <= program.totalMillis + 1_000) {
            val s = TimerEngine.snapshot(run, t)
            TimerCues.detect(previous, s, program)?.let { cues += it to t / 1000 }
            previous = s
            t += 250
        }
        val kinds = cues.map { it.first }
        assertEquals(3, kinds.count { it == TimerCue.ROUND_START })
        assertEquals(2, kinds.count { it == TimerCue.ROUND_END })
        assertEquals(3, kinds.count { it == TimerCue.WARNING })
        assertEquals(1, kinds.count { it == TimerCue.FINISHED })
        assertEquals(TimerCue.ROUND_START to 10L, cues.first { it.first == TimerCue.ROUND_START })
        assertEquals(TimerCue.FINISHED, kinds.last())
        // 3-2-1 count-in before each round start: prep + two rests.
        assertEquals(9, kinds.count { it == TimerCue.COUNTDOWN_TICK })
    }

    @Test
    fun `a late transition after the app was frozen does not ring`() {
        val run = TimerEngine.start(program, 0)
        val before = TimerEngine.snapshot(run, 5_000)
        // Process frozen for a minute: the next look at the clock is well past the round start.
        val after = TimerEngine.snapshot(run, 65_000)
        assertNull(TimerCues.detect(before, after, program))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a program needs at least one round`() {
        TimerProgram(prepSeconds = 0, workSeconds = 60, restSeconds = 0, rounds = 0)
    }
}
