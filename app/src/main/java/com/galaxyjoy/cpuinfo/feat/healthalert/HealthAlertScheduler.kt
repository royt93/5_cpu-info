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

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<HealthAlertWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
