package com.galaxyjoy.cpuinfo.feat.lastbenchwidget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic (15-minute floor, see [LastBenchWidgetProvider]) refresh of every placed "latest
 * benchmark result" widget instance. Plain [CoroutineWorker] (no Hilt), same reasoning as
 * [com.galaxyjoy.cpuinfo.feat.shieldwidget.ShieldScoreWidgetUpdateWorker].
 */
class LastBenchWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        LastBenchWidgetProvider.updateAllWidgets(applicationContext)
        return Result.success()
    }
}
