package com.earlthekaiju.coach.domain.timer

/**
 * The BJJ round timer as pure functions of a monotonic clock.
 *
 * A run stores only anchors (when it started, how long it has been paused). Every value on screen
 * is derived from `now - anchors`, so the countdown is exact no matter how often (or rarely) the
 * UI redraws, survives the app being backgrounded or killed, and never drifts the way a
 * "subtract one every second" counter does.
 */
data class TimerProgram(
    val prepSeconds: Int,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val warningSeconds: Int = 0,
) {
    init {
        require(workSeconds > 0) { "workSeconds must be positive" }
        require(rounds > 0) { "rounds must be positive" }
        require(prepSeconds >= 0 && restSeconds >= 0 && warningSeconds >= 0) { "durations cannot be negative" }
    }

    /** Prep (optional), then work/rest pairs; no rest after the final round. */
    val intervals: List<TimerInterval> = buildList {
        var start = 0L
        fun add(phase: TimerPhase, round: Int, seconds: Int) {
            if (seconds <= 0) return
            add(TimerInterval(phase, round, start, seconds * 1000L))
            start += seconds * 1000L
        }
        add(TimerPhase.PREP, 1, prepSeconds)
        for (round in 1..rounds) {
            add(TimerPhase.WORK, round, workSeconds)
            if (round < rounds) add(TimerPhase.REST, round, restSeconds)
        }
    }

    val totalMillis: Long = intervals.last().endMillis

    companion object {
        val Default = TimerProgram(prepSeconds = 10, workSeconds = 5 * 60, restSeconds = 60, rounds = 5, warningSeconds = 30)
    }
}

enum class TimerPhase(val label: String) { PREP("Get ready"), WORK("Train"), REST("Rest") }

enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

data class TimerInterval(val phase: TimerPhase, val round: Int, val startMillis: Long, val durationMillis: Long) {
    val endMillis: Long get() = startMillis + durationMillis
}

/** Persistable state of one timer run. Times are [com.earlthekaiju.coach.domain.time.AppClock.elapsedRealtime] values. */
data class TimerRun(
    val program: TimerProgram,
    val startedAtElapsed: Long,
    val pausedAccumMillis: Long = 0L,
    val pausedAtElapsed: Long? = null,
) {
    val isPaused: Boolean get() = pausedAtElapsed != null
}

data class TimerSnapshot(
    val status: TimerStatus,
    val phase: TimerPhase,
    val round: Int,
    val rounds: Int,
    val intervalIndex: Int,
    val remainingMillis: Long,
    val intervalDurationMillis: Long,
    val elapsedMillis: Long,
    val totalMillis: Long,
    val inWarning: Boolean,
    val next: TimerInterval?,
) {
    /** Whole seconds shown on screen: 4.3s left shows 5, 4.0s shows 4, so 0 means "done". */
    val remainingSeconds: Long get() = (remainingMillis + 999) / 1000

    /** 0..1 through the current interval. */
    val intervalProgress: Float
        get() = if (intervalDurationMillis <= 0) 1f else 1f - remainingMillis.toFloat() / intervalDurationMillis
}

object TimerEngine {
    fun start(program: TimerProgram, nowElapsed: Long): TimerRun = TimerRun(program, startedAtElapsed = nowElapsed)

    fun pause(run: TimerRun, nowElapsed: Long): TimerRun =
        if (run.isPaused) run else run.copy(pausedAtElapsed = nowElapsed)

    fun resume(run: TimerRun, nowElapsed: Long): TimerRun {
        val pausedAt = run.pausedAtElapsed ?: return run
        val pausedFor = (nowElapsed - pausedAt).coerceAtLeast(0L)
        return run.copy(pausedAccumMillis = run.pausedAccumMillis + pausedFor, pausedAtElapsed = null)
    }

    /** Milliseconds of actual training time elapsed (pauses excluded). */
    fun activeMillis(run: TimerRun, nowElapsed: Long): Long {
        val reference = run.pausedAtElapsed ?: nowElapsed
        return (reference - run.startedAtElapsed - run.pausedAccumMillis).coerceAtLeast(0L)
    }

    fun idle(program: TimerProgram): TimerSnapshot {
        val first = program.intervals.first()
        return TimerSnapshot(
            status = TimerStatus.IDLE,
            phase = first.phase,
            round = first.round,
            rounds = program.rounds,
            intervalIndex = 0,
            remainingMillis = first.durationMillis,
            intervalDurationMillis = first.durationMillis,
            elapsedMillis = 0L,
            totalMillis = program.totalMillis,
            inWarning = false,
            next = program.intervals.getOrNull(1),
        )
    }

    fun snapshot(run: TimerRun, nowElapsed: Long): TimerSnapshot {
        val program = run.program
        val active = activeMillis(run, nowElapsed)
        val intervals = program.intervals
        if (active >= program.totalMillis) {
            val last = intervals.last()
            return TimerSnapshot(
                status = TimerStatus.FINISHED,
                phase = last.phase,
                round = last.round,
                rounds = program.rounds,
                intervalIndex = intervals.lastIndex,
                remainingMillis = 0L,
                intervalDurationMillis = last.durationMillis,
                elapsedMillis = program.totalMillis,
                totalMillis = program.totalMillis,
                inWarning = false,
                next = null,
            )
        }
        val index = intervals.indexOfFirst { active < it.endMillis }
        val current = intervals[index]
        val remaining = current.endMillis - active
        val warning = current.phase == TimerPhase.WORK &&
            program.warningSeconds > 0 &&
            program.warningSeconds * 1000L < current.durationMillis &&
            remaining <= program.warningSeconds * 1000L
        return TimerSnapshot(
            status = if (run.isPaused) TimerStatus.PAUSED else TimerStatus.RUNNING,
            phase = current.phase,
            round = current.round,
            rounds = program.rounds,
            intervalIndex = index,
            remainingMillis = remaining,
            intervalDurationMillis = current.durationMillis,
            elapsedMillis = active,
            totalMillis = program.totalMillis,
            inWarning = warning,
            next = intervals.getOrNull(index + 1),
        )
    }

    /**
     * How long until the displayed second changes, so the UI redraws exactly on second boundaries.
     * Accuracy never depends on this value; it only decides when to look at the clock again.
     */
    fun millisUntilNextSecond(snapshot: TimerSnapshot): Long =
        ((snapshot.remainingMillis - 1).mod(1000L) + 1L).coerceIn(16L, 1000L)
}

enum class TimerCue { COUNTDOWN_TICK, ROUND_START, WARNING, ROUND_END, FINISHED }

object TimerCues {
    /**
     * The cue to play when the timer moves from [previous] to [current], or null.
     * A cue is dropped if its moment passed more than [toleranceMillis] ago (e.g. the app was frozen
     * in the background), so a late bell never rings out of sync with the mat.
     */
    fun detect(previous: TimerSnapshot?, current: TimerSnapshot, program: TimerProgram, toleranceMillis: Long = 1500L): TimerCue? {
        if (previous == null || previous.status == TimerStatus.IDLE) return null
        if (current.status == TimerStatus.PAUSED || previous.status == TimerStatus.FINISHED) return null

        if (current.status == TimerStatus.FINISHED) {
            return TimerCue.FINISHED.takeIf { current.elapsedMillis - program.totalMillis <= toleranceMillis }
        }
        if (current.intervalIndex != previous.intervalIndex) {
            val startedAt = program.intervals[current.intervalIndex].startMillis
            val late = current.elapsedMillis - startedAt > toleranceMillis
            if (late) return null
            return when (current.phase) {
                TimerPhase.WORK -> TimerCue.ROUND_START
                TimerPhase.REST -> TimerCue.ROUND_END
                TimerPhase.PREP -> null
            }
        }
        if (current.inWarning && !previous.inWarning) return TimerCue.WARNING
        val countingIn = current.phase == TimerPhase.PREP || current.phase == TimerPhase.REST
        if (countingIn && current.remainingSeconds in 1..3 && current.remainingSeconds != previous.remainingSeconds) {
            return TimerCue.COUNTDOWN_TICK
        }
        return null
    }
}
