package com.earlthekaiju.coach

import android.app.Application
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.feature.timer.TimerService

open class KaijuCoachApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        container.onAppStart()
        followTimer()
    }

    /** Keeps a running timer alive with the screen locked (see [TimerService]). */
    protected open fun followTimer() {
        TimerService.follow(this, container.timer, container.appScope)
    }

    /** Overridden in end-to-end tests to use a fixed clock and an in-memory database. */
    protected open fun createContainer(): AppContainer = AppContainer(this)
}
