package com.galaxyjoy.cpuinfo.feat.healthalert

import android.app.ActivityManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreProvider

/**
 * U26 — periodic (7d, see [HealthAlertScheduler]) "how did your device change this week" digest,
 * sibling to [HealthAlertWorker]'s 6-hourly drop check rather than a change to it — the two run on
 * independent schedules and independent state ([HealthAlertPrefs]'s weekly-baseline key vs its
 * last-score key), so a shared worker class would just be an `if` branch away from confusing which
 * timer triggered which behavior. Plain [CoroutineWorker] (no Hilt) — same reasoning as
 * [HealthAlertWorker].
 */
class HealthAlertWeeklyDigestWorker(
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
        HealthAlertWeeklyDigestNotifier.maybeNotify(applicationContext, prefs, currentScore)
        prefs.saveWeeklyBaselineScore(currentScore)

        return Result.success()
    }
}
