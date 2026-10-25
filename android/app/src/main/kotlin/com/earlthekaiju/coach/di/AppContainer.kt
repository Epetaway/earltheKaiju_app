package com.earlthekaiju.coach.di

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.earlthekaiju.coach.data.backup.BackupExporter
import com.earlthekaiju.coach.data.db.KaijuDatabase
import com.earlthekaiju.coach.data.prefs.SettingsRepository
import com.earlthekaiju.coach.data.repo.AttendanceRepository
import com.earlthekaiju.coach.data.repo.LessonRepository
import com.earlthekaiju.coach.data.repo.ProgramRepository
import com.earlthekaiju.coach.data.repo.SessionRepository
import com.earlthekaiju.coach.data.repo.StudentRepository
import com.earlthekaiju.coach.data.repo.TechniqueRepository
import com.earlthekaiju.coach.data.repo.TimerPresetRepository
import com.earlthekaiju.coach.data.sample.SampleData
import com.earlthekaiju.coach.domain.time.AppClock
import com.earlthekaiju.coach.domain.time.SystemAppClock
import com.earlthekaiju.coach.feature.timer.BootCounter
import com.earlthekaiju.coach.feature.timer.CuePlayer
import com.earlthekaiju.coach.feature.timer.TimerController
import com.earlthekaiju.coach.feature.timer.ToneCuePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manual dependency injection: one object that builds the app's long-lived pieces.
 * A single-instructor app with one module doesn't need a DI framework (see docs/adr/0003).
 */
class AppContainer(
    context: Context,
    val clock: AppClock = SystemAppClock,
    val database: KaijuDatabase = KaijuDatabase.build(context),
    val settings: SettingsRepository = SettingsRepository(context),
    cuePlayer: CuePlayer = ToneCuePlayer(context.applicationContext),
    bootCounter: BootCounter = BootCounter {
        Settings.Global.getInt(context.applicationContext.contentResolver, Settings.Global.BOOT_COUNT, -1)
    },
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    val programs = ProgramRepository(database, clock)
    val lessons = LessonRepository(database, clock)
    val techniques = TechniqueRepository(database, clock)
    val sessions = SessionRepository(database, clock)
    val students = StudentRepository(database, clock)
    val attendance = AttendanceRepository(database, clock)
    val timerPresets = TimerPresetRepository(database, clock, settings)
    val backup = BackupExporter(database, settings, clock)
    val sampleData = SampleData(clock, programs, lessons, techniques, sessions, students, attendance)
    val timer = TimerController(appScope, clock, settings, cuePlayer, bootCounter)

    fun onAppStart() {
        appScope.launch { timerPresets.seedDefaultsIfNeeded() }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

/** Creates a ViewModel scoped to the current navigation entry, with access to the container. */
@Composable
inline fun <reified VM : ViewModel> kaijuViewModel(
    key: String? = null,
    crossinline create: (container: AppContainer, handle: SavedStateHandle) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(key = key) { create(container, createSavedStateHandle()) }
}
