package com.galaxyjoy.cpuinfo.feat.ramwidget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic (15-minute floor, see [RamWidgetProvider]) refresh of every placed RAM widget
 * instance. Plain [CoroutineWorker] (no Hilt) — [RamWidgetProvider.updateAllWidgets] only needs
 * a [Context], which WorkManager's default factory already provides via reflection.
 */
class RamWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        RamWidgetProvider.updateAllWidgets(applicationContext)
        return Result.success()
    }
}
