package com.galaxyjoy.cpuinfo.feat.benchreminder

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
 * [com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertWeeklyDigestNotifierTest], for U30.
 */
@RunWith(AndroidJUnit4::class)
class BenchReminderNotifierTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val appContext = instrumentation.targetContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)!!

    private val dayMs = 24 * 60 * 60 * 1000L
    private val intervalMs = BenchReminderLogic.REMINDER_INTERVAL_DAYS * dayMs

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
                    BenchReminderNotifier.CHANNEL_ID,
                    "Test channel",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        notificationManager.cancel(BenchReminderNotifier.NOTIFICATION_ID)
    }

    @After
    fun tearDown() {
        notificationManager.cancel(BenchReminderNotifier.NOTIFICATION_ID)
    }

    private fun isNotificationActive(): Boolean =
        notificationManager.activeNotifications.any { it.id == BenchReminderNotifier.NOTIFICATION_ID }

    @Test
    fun maybeNotify_neverBenchmarked_postsNoNotification() {
        BenchReminderNotifier.maybeNotify(appContext, lastBenchTimestampMs = null, nowMs = intervalMs * 2)

        assertFalse(isNotificationActive())
    }

    @Test
    fun maybeNotify_recentlyBenchmarked_postsNoNotification() {
        val now = intervalMs * 2
        BenchReminderNotifier.maybeNotify(appContext, lastBenchTimestampMs = now - dayMs, nowMs = now)

        assertFalse(isNotificationActive())
    }

    @Test
    fun maybeNotify_pastTheInterval_postsARealNotification() {
        val now = intervalMs * 2
        BenchReminderNotifier.maybeNotify(appContext, lastBenchTimestampMs = now - intervalMs, nowMs = now)

        assertTrue(isNotificationActive())
    }

    @Test
    fun notificationId_doesNotCollideWithHealthAlertIds() {
        assertFalse(
            BenchReminderNotifier.NOTIFICATION_ID ==
                com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertNotifier.NOTIFICATION_ID,
        )
        assertFalse(
            BenchReminderNotifier.NOTIFICATION_ID ==
                com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertWeeklyDigestNotifier.NOTIFICATION_ID,
        )
    }
}
