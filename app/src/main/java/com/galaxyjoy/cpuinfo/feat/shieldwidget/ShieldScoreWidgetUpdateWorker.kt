package com.galaxyjoy.cpuinfo.feat.shieldwidget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic (15-minute floor, see [ShieldScoreWidgetProvider]) refresh of every placed Shield
 * Score widget instance. Plain [CoroutineWorker] (no Hilt), same reasoning as
 * [com.galaxyjoy.cpuinfo.feat.ramwidget.RamWidgetUpdateWorker].
 */
class ShieldScoreWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        ShieldScoreWidgetProvider.updateAllWidgets(applicationContext)
        return Result.success()
    }
}
