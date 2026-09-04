package com.galaxyjoy.cpuinfo.feat.benchreminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs

/**
 * U30 — periodic (see [BenchReminderScheduler]) check of "how long since any benchmark last ran".
 * Plain [CoroutineWorker] (no Hilt) — same reasoning as
 * [com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertWorker]; `*ResultPrefs` constructors are
 * plain `(Context)` despite being `@Inject`-annotated elsewhere (same fact U25/U28 already rely
 * on).
 */
class BenchReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val lastBenchTimestampMs = BenchReminderLogic.latestOf(
            ThrottleResultPrefs(context).getLastResult()?.timestampMs,
            StorageBenchResultPrefs(context).getLastResult()?.timestampMs,
            RamBenchResultPrefs(context).getLastResult()?.timestampMs,
            GpuBenchResultPrefs(context).getLastResult()?.timestampMs,
        )
        BenchReminderNotifier.maybeNotify(context, lastBenchTimestampMs, System.currentTimeMillis())
        return Result.success()
    }
}
