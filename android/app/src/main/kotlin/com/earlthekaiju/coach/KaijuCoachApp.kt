package com.earlthekaiju.coach

import android.app.Application
import com.earlthekaiju.coach.di.AppContainer

open class KaijuCoachApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        container.onAppStart()
    }

    /** Overridden in end-to-end tests to use a fixed clock and an in-memory database. */
    protected open fun createContainer(): AppContainer = AppContainer(this)
}
