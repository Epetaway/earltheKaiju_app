package com.earlthekaiju.coach

import android.app.Application
import com.earlthekaiju.coach.di.AppContainer

class KaijuCoachApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.onAppStart()
    }
}
