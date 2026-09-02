package com.galaxyjoy.cpuinfo.feat.healthalert

import android.app.ActivityManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreProvider

/**
 * Periodic (6h, see [HealthAlertScheduler]) background check: recompute the Shield Score via the
 * existing [ShieldScoreProvider] (no new scoring logic) and hand off the decide-and-post step to
 * [HealthAlertNotifier]. Plain [CoroutineWorker] (no Hilt) — same reasoning as
 * [com.galaxyjoy.cpuinfo.feat.shieldwidget.ShieldScoreWidgetUpdateWorker].
 */
class HealthAlertWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val provider = ShieldScoreProvider(
            dataProviderRam = DataProviderRam(activityManager),
            batteryStatusProvider = BatteryStatusProvider(applicationContext),
        )
        val currentScore = provider.compute().overall

        val prefs = HealthAlertPrefs(applicationContext)
        val now = System.currentTimeMillis()
        HealthAlertNotifier.maybeNotify(applicationContext, prefs, currentScore, now)
        prefs.saveScore(currentScore)

        return Result.success()
    }
}
