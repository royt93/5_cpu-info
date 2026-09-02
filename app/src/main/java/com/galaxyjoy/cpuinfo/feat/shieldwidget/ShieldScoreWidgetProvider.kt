package com.galaxyjoy.cpuinfo.feat.shieldwidget

import android.app.ActivityManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.VisibleForTesting
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.feat.ActHost
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreCalculator
import com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreProvider
import java.util.concurrent.TimeUnit

/**
 * Home-screen "device health score" widget (U13) — same shape as
 * [com.galaxyjoy.cpuinfo.feat.ramwidget.RamWidgetProvider], refreshed by
 * [ShieldScoreWidgetUpdateWorker] every 15 minutes (WorkManager's own floor). Does NOT use Hilt:
 * [DataProviderRam]/[BatteryStatusProvider] have trivial framework-only constructors, matching the
 * RAM widget's reasoning for skipping a `HiltWorkerFactory`.
 */
class ShieldScoreWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePeriodicRefresh(context)
    }

    override fun onDisabled(context: Context) {
        cancelPeriodicRefresh(context)
        super.onDisabled(context)
    }

    companion object {
        internal const val WORK_NAME = "shield_score_widget_refresh"

        /** Read by [ActHost] to open [com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreBottomSheet]
         * right after launch, instead of just landing on the default tab like a plain app-icon tap. */
        const val EXTRA_OPEN_SHIELD_SCORE = "com.galaxyjoy.cpuinfo.extra.OPEN_SHIELD_SCORE"

        internal fun schedulePeriodicRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<ShieldScoreWidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        internal fun cancelPeriodicRefresh(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, ShieldScoreWidgetProvider::class.java))
            ids.forEach { updateWidget(context, appWidgetManager, it) }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val provider = ShieldScoreProvider(
                dataProviderRam = DataProviderRam(activityManager),
                batteryStatusProvider = BatteryStatusProvider(context.applicationContext),
            )
            val result = provider.compute()

            val views = buildRemoteViews(context, appWidgetId, result)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        internal fun buildRemoteViews(context: Context, appWidgetId: Int, result: ShieldScoreCalculator.Result): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_shield_score)
            views.setTextViewText(
                R.id.widgetShieldScorePercent,
                context.getString(R.string.shield_score_widget_score_format, result.overall),
            )
            // Color-code the number itself (RemoteViews.setTextColor takes a plain int, unlike
            // ProgressBar's tint setters which need a ColorStateList — not reflectable via
            // RemoteViews.setInt) rather than the progress bar fill.
            views.setTextColor(R.id.widgetShieldScorePercent, scoreColor(result.overall))
            views.setProgressBar(R.id.widgetShieldScoreProgress, 100, result.overall, false)

            val openAppIntent = Intent(context, ActHost::class.java).apply {
                putExtra(EXTRA_OPEN_SHIELD_SCORE, true)
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, appWidgetId, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widgetShieldScoreRoot, openAppPendingIntent)

            return views
        }

        /** Same 3-band thresholds/colors as [com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreBottomSheet]'s
         * `scoreColor()` — kept in sync manually since RemoteViews can't share a Compose `Color`.
         * Plain Int literals (not `android.graphics.Color.parseColor`) so this stays pure/JVM-testable —
         * `Color.parseColor` is a real Android framework call that returns 0 under this project's
         * `unitTests.isReturnDefaultValues = true` JVM test setup, not the real parsed value. */
        @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
        internal fun scoreColor(score: Int): Int = when {
            score >= 80 -> 0xFF4CAF50.toInt()
            score >= 50 -> 0xFFFFA726.toInt()
            else -> 0xFFE53935.toInt()
        }
    }
}
