package com.galaxyjoy.cpuinfo.feat.healthalert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-device test for the one seam [HealthAlertLogicTest]/[HealthAlertSchedulerTest] can't cover
 * (pure logic + WorkManager scheduling calls, respectively) — does [HealthAlertNotifier] actually
 * post a real `Notification`? Runs against the real on-device notification manager (no mocking),
 * granting `POST_NOTIFICATIONS` via `UiAutomation` rather than adding the `androidx.test:rules`
 * dependency just for `GrantPermissionRule`.
 */
@RunWith(AndroidJUnit4::class)
class HealthAlertNotifierTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val appContext = instrumentation.targetContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)!!
    private val prefs = HealthAlertPrefs(appContext)

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(
                appContext.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    HealthAlertNotifier.CHANNEL_ID,
                    "Test channel",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        prefs.clear()
        notificationManager.cancel(HealthAlertNotifier.NOTIFICATION_ID)
    }

    @After
    fun tearDown() {
        notificationManager.cancel(HealthAlertNotifier.NOTIFICATION_ID)
        prefs.clear()
    }

    private fun isNotificationActive(): Boolean =
        notificationManager.activeNotifications.any { it.id == HealthAlertNotifier.NOTIFICATION_ID }

    /** Both notify-triggered posts and [NotificationManager.cancel] are fire-and-forget Binder
     * calls to the system notification service — [isNotificationActive] can still report the
     * stale pre-call state for a brief window right after either one. Poll instead of checking
     * once immediately. */
    private fun waitUntilNotificationActiveIs(expected: Boolean, timeoutMs: Long = 2_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (isNotificationActive() != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
    }

    @Test
    fun maybeNotify_scoreBelowAbsoluteThreshold_postsARealNotification() {
        HealthAlertNotifier.maybeNotify(
            appContext,
            prefs,
            currentScore = HealthAlertLogic.ABSOLUTE_LOW_THRESHOLD - 1,
            nowMs = System.currentTimeMillis(),
        )

        waitUntilNotificationActiveIs(true)
        assertTrue(isNotificationActive())
    }

    @Test
    fun maybeNotify_scoreHighWithNoPriorDrop_postsNoNotification() {
        HealthAlertNotifier.maybeNotify(
            appContext,
            prefs,
            currentScore = 100,
            nowMs = System.currentTimeMillis(),
        )

        assertFalse(isNotificationActive())
    }

    @Test
    fun maybeNotify_secondCallWithinCooldown_doesNotPostAgain() {
        val now = System.currentTimeMillis()
        HealthAlertNotifier.maybeNotify(appContext, prefs, currentScore = HealthAlertLogic.ABSOLUTE_LOW_THRESHOLD - 1, nowMs = now)
        waitUntilNotificationActiveIs(true)
        assertTrue(isNotificationActive())
        notificationManager.cancel(HealthAlertNotifier.NOTIFICATION_ID)
        // cancel() above is async (Binder call to the system notification service) — wait for it
        // to actually land, otherwise the assertFalse below can see the pre-cancel notification
        // still listed and misreport this as maybeNotify() having wrongly re-posted. The cooldown
        // logic itself (HealthAlertLogic.shouldAlert) is pure and separately proven correct by
        // HealthAlertLogicTest, so a failure here was always this test's race, not app logic.
        waitUntilNotificationActiveIs(false)

        HealthAlertNotifier.maybeNotify(
            appContext,
            prefs,
            currentScore = HealthAlertLogic.ABSOLUTE_LOW_THRESHOLD - 1,
            nowMs = now + 1_000L,
        )

        assertFalse(isNotificationActive())
    }
}
