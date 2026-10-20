package com.earlthekaiju.coach.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.earlthekaiju.coach.domain.time.newId
import com.earlthekaiju.coach.domain.timer.TimerProgram
import com.earlthekaiju.coach.domain.timer.TimerRun
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.kaijuSettings: DataStore<Preferences> by preferencesDataStore(name = "kaiju_settings")

data class AppSettings(
    val timerSound: Boolean = true,
    val timerVibration: Boolean = true,
    val keepScreenOn: Boolean = true,
    val lastBackupAt: Long? = null,
)

/** A timer run saved across process death, valid only within the same device boot. */
data class PersistedTimer(
    val run: TimerRun?,
    val program: TimerProgram,
    val presetId: String?,
    val presetName: String,
    val bootCount: Int,
)

/**
 * Small user preferences and app state in DataStore. Records (students, classes…) live in Room;
 * this holds toggles, the device id for future sync, and the running timer's anchors.
 */
class SettingsRepository(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.kaijuSettings)

    private object Keys {
        val timerSound = booleanPreferencesKey("timer_sound")
        val timerVibration = booleanPreferencesKey("timer_vibration")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val lastBackupAt = longPreferencesKey("last_backup_at")
        val deviceId = stringPreferencesKey("device_id")
        val presetsSeeded = booleanPreferencesKey("presets_seeded")

        val timerProgram = stringPreferencesKey("timer_program")
        val timerPresetId = stringPreferencesKey("timer_preset_id")
        val timerPresetName = stringPreferencesKey("timer_preset_name")
        val timerStartedAt = longPreferencesKey("timer_started_elapsed")
        val timerPausedAccum = longPreferencesKey("timer_paused_accum")
        val timerPausedAt = longPreferencesKey("timer_paused_at")
        val timerBootCount = intPreferencesKey("timer_boot_count")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            timerSound = p[Keys.timerSound] ?: true,
            timerVibration = p[Keys.timerVibration] ?: true,
            keepScreenOn = p[Keys.keepScreenOn] ?: true,
            lastBackupAt = p[Keys.lastBackupAt],
        )
    }

    suspend fun setTimerSound(enabled: Boolean) = store.edit { it[Keys.timerSound] = enabled }
    suspend fun setTimerVibration(enabled: Boolean) = store.edit { it[Keys.timerVibration] = enabled }
    suspend fun setKeepScreenOn(enabled: Boolean) = store.edit { it[Keys.keepScreenOn] = enabled }
    suspend fun setLastBackupAt(epochMillis: Long) = store.edit { it[Keys.lastBackupAt] = epochMillis }

    /** A random id for this install; the future desktop sync uses it to name the phone. */
    suspend fun deviceId(): String {
        store.data.first()[Keys.deviceId]?.let { return it }
        val id = newId()
        store.edit { if (it[Keys.deviceId] == null) it[Keys.deviceId] = id }
        return store.data.first()[Keys.deviceId] ?: id
    }

    suspend fun presetsSeeded(): Boolean = store.data.first()[Keys.presetsSeeded] ?: false
    suspend fun markPresetsSeeded() = store.edit { it[Keys.presetsSeeded] = true }

    suspend fun saveTimer(state: PersistedTimer) = store.edit { p ->
        p[Keys.timerProgram] = encode(state.program)
        state.presetId?.let { p[Keys.timerPresetId] = it } ?: p.remove(Keys.timerPresetId)
        p[Keys.timerPresetName] = state.presetName
        p[Keys.timerBootCount] = state.bootCount
        val run = state.run
        if (run == null) {
            p.remove(Keys.timerStartedAt); p.remove(Keys.timerPausedAccum); p.remove(Keys.timerPausedAt)
        } else {
            p[Keys.timerStartedAt] = run.startedAtElapsed
            p[Keys.timerPausedAccum] = run.pausedAccumMillis
            run.pausedAtElapsed?.let { p[Keys.timerPausedAt] = it } ?: p.remove(Keys.timerPausedAt)
        }
    }

    suspend fun loadTimer(): PersistedTimer? {
        val p = store.data.first()
        val program = p[Keys.timerProgram]?.let(::decode) ?: return null
        val started = p[Keys.timerStartedAt]
        val run = started?.let {
            TimerRun(
                program = program,
                startedAtElapsed = it,
                pausedAccumMillis = p[Keys.timerPausedAccum] ?: 0L,
                pausedAtElapsed = p[Keys.timerPausedAt],
            )
        }
        return PersistedTimer(
            run = run,
            program = program,
            presetId = p[Keys.timerPresetId],
            presetName = p[Keys.timerPresetName] ?: "Custom",
            bootCount = p[Keys.timerBootCount] ?: -1,
        )
    }

    private fun encode(p: TimerProgram) = listOf(p.prepSeconds, p.workSeconds, p.restSeconds, p.rounds, p.warningSeconds).joinToString(",")

    private fun decode(text: String): TimerProgram? = runCatching {
        val v = text.split(",").map(String::toInt)
        TimerProgram(prepSeconds = v[0], workSeconds = v[1], restSeconds = v[2], rounds = v[3], warningSeconds = v[4])
    }.getOrNull()
}
