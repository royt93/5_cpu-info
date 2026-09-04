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
 * Same "does it actually post a real `Notification`" real-device tier as
 * [HealthAlertNotifierTest], for the U26 weekly digest sibling.
 */
@RunWith(AndroidJUnit4::class)
class HealthAlertWeeklyDigestNotifierTest {

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
        notificationManager.cancel(HealthAlertWeeklyDigestNotifier.NOTIFICATION_ID)
    }

    @After
    fun tearDown() {
        notificationManager.cancel(HealthAlertWeeklyDigestNotifier.NOTIFICATION_ID)
        prefs.clear()
    }

    private fun isNotificationActive(): Boolean =
        notificationManager.activeNotifications.any { it.id == HealthAlertWeeklyDigestNotifier.NOTIFICATION_ID }

    @Test
    fun maybeNotify_noBaselineYet_postsNoNotification() {
        HealthAlertWeeklyDigestNotifier.maybeNotify(appContext, prefs, currentScore = 80)

        assertFalse(isNotificationActive())
    }

    @Test
    fun maybeNotify_withBaseline_postsARealNotification() {
        prefs.saveWeeklyBaselineScore(70)

        HealthAlertWeeklyDigestNotifier.maybeNotify(appContext, prefs, currentScore = 80)

        assertTrue(isNotificationActive())
    }

    @Test
    fun maybeNotify_scoreUnchangedFromBaseline_stillPostsANotification() {
        // Unlike HealthAlertNotifier's drop-only alert, the digest always reports on a schedule
        // once a baseline exists — "no change" is itself a real answer to "how was my week".
        prefs.saveWeeklyBaselineScore(80)

        HealthAlertWeeklyDigestNotifier.maybeNotify(appContext, prefs, currentScore = 80)

        assertTrue(isNotificationActive())
    }

    @Test
    fun notificationId_doesNotCollideWithTheDropAlerts() {
        assertFalse(HealthAlertNotifier.NOTIFICATION_ID == HealthAlertWeeklyDigestNotifier.NOTIFICATION_ID)
    }
}
