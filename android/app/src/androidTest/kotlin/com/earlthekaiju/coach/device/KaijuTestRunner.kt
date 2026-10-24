package com.earlthekaiju.coach.device

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.test.runner.AndroidJUnitRunner
import com.earlthekaiju.coach.KaijuCoachApp
import com.earlthekaiju.coach.data.db.KaijuDatabase
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.domain.time.AppClock
import java.time.LocalDateTime
import java.time.ZoneId

/** Instrumented tests run against [DeviceTestApp] instead of the real Application. */
class KaijuTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, DeviceTestApp::class.java.name, context)
}

/**
 * Wall-clock time starts at Wednesday, October 21, 2026, 6:20 PM in New Jersey and then runs
 * at real speed, so classes and timers behave as on the mat but every run starts the same way.
 */
class ShiftedClock(start: LocalDateTime, private val zone: ZoneId) : AppClock {
    private val startWall = start.atZone(zone).toInstant().toEpochMilli()
    private val startElapsed = SystemClock.elapsedRealtime()
    override fun now(): Long = startWall + (SystemClock.elapsedRealtime() - startElapsed)
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun zone(): ZoneId = zone
}

/**
 * The real app with an in-memory database and the shifted clock. Everything else is the
 * production setup: DataStore on disk (fresh per install), real tone and vibration cues.
 */
class DeviceTestApp : KaijuCoachApp() {
    override fun createContainer(): AppContainer = AppContainer(
        context = this,
        clock = ShiftedClock(LocalDateTime.of(2026, 10, 21, 18, 20), ZoneId.of("America/New_York")),
        database = KaijuDatabase.inMemory(this),
    )
}
