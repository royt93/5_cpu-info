package com.galaxyjoy.cpuinfo.feat.healthalert

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedule/cancel the periodic background check — same `enqueueUniquePeriodicWork` shape as
 * [com.galaxyjoy.cpuinfo.feat.shieldwidget.ShieldScoreWidgetProvider], just standalone (no
 * `AppWidgetProvider` to hang the calls off since this isn't a widget). 6-hour interval, not
 * WorkManager's 15-minute floor — this is a background health check, not a live-refreshing
 * widget, and only needs to catch a real drift over hours, not minutes. */
object HealthAlertScheduler {

    const val WORK_NAME = "health_alert_check"

    /** U26 — same toggle as [WORK_NAME] (see `FrmSettings.wireHealthAlertPref()`), just a second
     * `enqueueUniquePeriodicWork` call under its own unique name, no new Settings switch needed. */
    const val WEEKLY_DIGEST_WORK_NAME = "health_alert_weekly_digest"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<HealthAlertWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)

        val digestRequest = PeriodicWorkRequestBuilder<HealthAlertWeeklyDigestWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WEEKLY_DIGEST_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, digestRequest)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(WEEKLY_DIGEST_WORK_NAME)
    }
}
