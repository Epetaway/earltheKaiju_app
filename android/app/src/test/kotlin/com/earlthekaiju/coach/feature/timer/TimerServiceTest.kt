package com.earlthekaiju.coach.feature.timer

import android.app.Notification
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earlthekaiju.coach.e2e.E2eApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The lock-screen timer service (docs/07): it goes to the foreground with the current round and
 * Pause/Reset controls, and takes itself down when the timer is reset. The E2E app's clock is
 * frozen, so the run stays in its 10-second "get ready" interval for the whole test.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = E2eApp::class)
class TimerServiceTest {

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun runsInTheForegroundWithControlsAndStopsOnReset() {
        val timer = ApplicationProvider.getApplicationContext<E2eApp>().container.timer
        idle()
        timer.start()
        idle()

        val service = Robolectric.buildService(TimerService::class.java).create().startCommand(0, 1).get()
        idle()
        val shadow = shadowOf(service)

        val notification = shadow.lastForegroundNotification
        assertNotNull("the service must go to the foreground", notification)
        assertEquals("Sparring rounds", notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("Get ready · round 1 of 5", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(listOf("Pause", "Reset"), notification.actions.map { it.title.toString() })

        timer.reset()
        idle()
        assertTrue("reset stops the service", shadow.isStoppedBySelf)
    }
}
