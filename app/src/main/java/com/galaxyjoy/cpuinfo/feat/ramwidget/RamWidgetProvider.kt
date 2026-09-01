package com.galaxyjoy.cpuinfo.feat.ramwidget

import android.app.ActivityManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.domain.action.RamCleanupAction
import com.galaxyjoy.cpuinfo.domain.model.RamData
import com.galaxyjoy.cpuinfo.feat.ActHost
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Home-screen RAM widget (quick_win.md #2). Refreshed by [RamWidgetUpdateWorker] every 15 minutes
 * (WorkManager's own floor for periodic work — [AppWidgetProvider.onUpdate]'s own
 * `updatePeriodMillis` path is disabled in `ram_widget_info.xml`, since the OS enforces a coarser
 * 30-minute floor there and WorkManager is the more reliable, testable mechanism). Does NOT use
 * Hilt: [DataProviderRam]/[RamCleanupAction] have trivial framework-only constructors, so this
 * avoids wiring a `HiltWorkerFactory` into `GalaxyApp` just for two classes this simple.
 */
class RamWidgetProvider : AppWidgetProvider() {

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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CLEAN_RAM) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    performCleanAndRefresh(context)
                } catch (e: Exception) {
                    // A crash here would surface to the user as "tapped Clean RAM, app died" —
                    // an odd OEM MemoryInfo reading or a widget-host Binder hiccup must not take
                    // the whole process down.
                    Timber.e(e, "RAM widget clean-and-refresh failed")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        internal const val WORK_NAME = "ram_widget_refresh"
        const val ACTION_CLEAN_RAM = "com.galaxyjoy.cpuinfo.action.WIDGET_CLEAN_RAM"

        internal fun schedulePeriodicRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<RamWidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        internal fun cancelPeriodicRefresh(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        internal suspend fun performCleanAndRefresh(context: Context) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            RamCleanupAction(context.applicationContext, activityManager, DispatchersProvider())(Unit)
            updateAllWidgets(context)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, RamWidgetProvider::class.java))
            ids.forEach { updateWidget(context, appWidgetManager, it) }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val dataProviderRam = DataProviderRam(activityManager)
            val ramData = RamData(
                total = dataProviderRam.getTotalBytes(),
                available = dataProviderRam.getAvailableBytes(),
                availablePercentage = dataProviderRam.getAvailablePercentage(),
                threshold = dataProviderRam.getThreshold(),
            )

            val views = buildRemoteViews(context, appWidgetId, ramData)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        internal fun buildRemoteViews(context: Context, appWidgetId: Int, ramData: RamData): RemoteViews {
            val usedPercentage = (100 - ramData.availablePercentage).coerceIn(0, 100)

            val views = RemoteViews(context.packageName, R.layout.widget_ram)
            views.setTextViewText(R.id.widgetRamPercent, "$usedPercentage%")
            views.setProgressBar(R.id.widgetRamProgress, 100, usedPercentage, false)
            views.setTextViewText(
                R.id.widgetRamDetail,
                context.getString(
                    R.string.ram_widget_detail_format,
                    Utils.convertBytesToMega(ramData.available),
                    Utils.convertBytesToMega(ramData.total),
                ),
            )

            val openAppIntent = Intent(context, ActHost::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context, appWidgetId, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppPendingIntent)

            val cleanIntent = Intent(context, RamWidgetProvider::class.java).apply {
                action = ACTION_CLEAN_RAM
            }
            val cleanPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, cleanIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widgetRamCleanButton, cleanPendingIntent)

            return views
        }
    }
}
