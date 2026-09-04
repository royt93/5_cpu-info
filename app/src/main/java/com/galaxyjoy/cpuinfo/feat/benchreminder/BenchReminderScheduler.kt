package com.galaxyjoy.cpuinfo.feat.benchreminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** U30 — same `enqueueUniquePeriodicWork` shape as
 * [com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertScheduler]. Period matches
 * [BenchReminderLogic.REMINDER_INTERVAL_DAYS] so the worker's own periodic tick lines up with the
 * threshold it evaluates — the threshold check inside [BenchReminderWorker] stays the real source
 * of truth (defensively correct even if WorkManager fires early after a reboot/backoff). */
object BenchReminderScheduler {

    const val WORK_NAME = "bench_reminder_check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<BenchReminderWorker>(
            BenchReminderLogic.REMINDER_INTERVAL_DAYS.toLong(),
            TimeUnit.DAYS,
        ).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
