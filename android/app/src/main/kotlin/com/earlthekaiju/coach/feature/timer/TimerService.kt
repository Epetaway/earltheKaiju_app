package com.earlthekaiju.coach.feature.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.earlthekaiju.coach.KaijuCoachApp
import com.earlthekaiju.coach.MainActivity
import com.earlthekaiju.coach.R
import com.earlthekaiju.coach.domain.timer.TimerPhase
import com.earlthekaiju.coach.domain.timer.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Keeps a running timer alive while the phone is locked or the app is in the background
 * (docs/07, Sprint 02).
 *
 * The service owns no timer logic. The app-wide [TimerController] still counts and fires cues;
 * this service only:
 *  - runs as a foreground service, so Android doesn't freeze or kill the process mid-round;
 *  - holds a partial wake lock while the timer is *running* (not paused), so the CPU keeps
 *    waking for each second and the bells ring on time with the screen off;
 *  - shows an ongoing notification (lock screen too) with the round, a live countdown and
 *    Pause/Resume and Reset buttons.
 *
 * It starts when a run becomes active ([follow]) and stops itself when the timer is reset or
 * a few seconds after it finishes, once the final bells have played.
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val timer: TimerController get() = (application as KaijuCoachApp).container.timer

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Every start must promote the service to the foreground promptly, even if the timer was
        // reset in the meantime; the observer below then stops it straight away.
        ensureChannel(this)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(this, timer.state.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        when (intent?.action) {
            ACTION_PAUSE_RESUME -> when (timer.state.value.snapshot.status) {
                TimerStatus.RUNNING -> timer.pause()
                TimerStatus.PAUSED -> timer.resume()
                else -> Unit
            }
            ACTION_RESET -> timer.reset()
        }
        if (observing == null) observing = scope.launch { observe() }
        return START_NOT_STICKY
    }

    private suspend fun observe() {
        // Only rebuild the notification when something it shows changes: status, the interval,
        // or (while paused) the frozen remaining second. The countdown itself is drawn by the
        // notification's own chronometer, not by posting every second.
        timer.state
            .map { s -> NotificationKey(s.snapshot.status, s.snapshot.intervalIndex, s.presetName, if (s.snapshot.status == TimerStatus.PAUSED) s.snapshot.remainingSeconds else -1L) to s }
            .distinctUntilChanged { a, b -> a.first == b.first }
            // collectLatest: pressing Start again during the finish grace period cancels the stop.
            .collectLatest { (key, state) ->
                when (key.status) {
                    TimerStatus.RUNNING -> { holdWakeLock(state.snapshot.totalMillis - state.snapshot.elapsedMillis); post(state) }
                    TimerStatus.PAUSED -> { releaseWakeLock(); post(state) }
                    TimerStatus.FINISHED -> {
                        post(state)
                        // Let the three finishing bells play out before letting the CPU sleep.
                        delay(FINISH_GRACE_MILLIS)
                        stop()
                    }
                    TimerStatus.IDLE -> stop()
                }
            }
    }

    private data class NotificationKey(val status: TimerStatus, val interval: Int, val name: String, val pausedSecond: Long)

    // Permission is checked on the line above; lint can't see through the helper.
    @SuppressLint("MissingPermission")
    private fun post(state: TimerState) {
        if (!canNotify(this)) return
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(this, state))
    }

    private fun holdWakeLock(remainingMillis: Long) {
        val lock = wakeLock ?: getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KaijuCoach:timer")
            ?.apply { setReferenceCounted(false) }
            ?.also { wakeLock = it }
            ?: return
        // Re-acquiring replaces the timeout: always "until the program ends, plus a margin".
        lock.acquire((remainingMillis + WAKE_LOCK_MARGIN_MILLIS).coerceIn(WAKE_LOCK_MARGIN_MILLIS, MAX_WAKE_LOCK_MILLIS))
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    private fun stop() {
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        observing = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "timer"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_PAUSE_RESUME = "com.earlthekaiju.coach.timer.PAUSE_RESUME"
        private const val ACTION_RESET = "com.earlthekaiju.coach.timer.RESET"
        private const val FINISH_GRACE_MILLIS = 5_000L
        private const val WAKE_LOCK_MARGIN_MILLIS = 60_000L
        private const val MAX_WAKE_LOCK_MILLIS = 4 * 60 * 60 * 1000L

        /**
         * Starts the service whenever a run becomes active (Start pressed, or a run restored after
         * the process was killed). Called once from the Application.
         */
        fun follow(context: Context, controller: TimerController, scope: CoroutineScope) {
            val app = context.applicationContext
            scope.launch {
                controller.state
                    .map { it.snapshot.status == TimerStatus.RUNNING || it.snapshot.status == TimerStatus.PAUSED }
                    .distinctUntilChanged()
                    .collect { active ->
                        if (active) {
                            // Android 12+ refuses to start a foreground service from the background
                            // (for example a run restored while the app wasn't open). The timer
                            // still counts correctly; the service starts the next time it's opened.
                            runCatching { ContextCompat.startForegroundService(app, Intent(app, TimerService::class.java)) }
                        }
                    }
            }
        }

        fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Training timer", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows the running round timer, with pause and reset, while the phone is locked."
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
        }

        private fun buildNotification(context: Context, state: TimerState): Notification {
            val s = state.snapshot
            val text = when (s.status) {
                TimerStatus.PAUSED -> "Paused · ${clock(s.remainingSeconds)} left · ${phaseText(state)}"
                TimerStatus.FINISHED -> "Finished · ${s.rounds} rounds"
                else -> phaseText(state)
            }
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setColor(0xFF0B7A62.toInt())
                .setContentTitle(state.presetName)
                .setContentText(text)
                .setContentIntent(open)
                .setOngoing(s.status == TimerStatus.RUNNING || s.status == TimerStatus.PAUSED)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

            if (s.status == TimerStatus.RUNNING) {
                // The system draws the countdown to the end of this interval, second by second.
                builder.setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setShowWhen(true)
                    .setWhen(System.currentTimeMillis() + s.remainingMillis)
            } else {
                builder.setShowWhen(false)
            }

            if (s.status == TimerStatus.RUNNING || s.status == TimerStatus.PAUSED) {
                val label = if (s.status == TimerStatus.RUNNING) "Pause" else "Resume"
                builder.addAction(R.drawable.ic_stat_timer, label, serviceIntent(context, ACTION_PAUSE_RESUME, 1))
                builder.addAction(R.drawable.ic_stat_timer, "Reset", serviceIntent(context, ACTION_RESET, 2))
            }
            return builder.build()
        }

        private fun serviceIntent(context: Context, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getService(
                context, requestCode,
                Intent(context, TimerService::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun phaseText(state: TimerState): String {
            val s = state.snapshot
            return when (s.phase) {
                TimerPhase.PREP -> "Get ready · round 1 of ${s.rounds}"
                TimerPhase.WORK -> "Round ${s.round} of ${s.rounds} · Train"
                TimerPhase.REST -> "Rest · round ${(s.round + 1).coerceAtMost(s.rounds)} next"
            }
        }

        private fun clock(seconds: Long): String = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
    }
}
