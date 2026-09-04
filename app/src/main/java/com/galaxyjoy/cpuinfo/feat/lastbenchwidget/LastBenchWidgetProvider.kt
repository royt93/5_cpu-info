package com.galaxyjoy.cpuinfo.feat.lastbenchwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.ActHost
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoContainerState
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import com.galaxyjoy.cpuinfo.util.WidgetSizeClass
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Home-screen "latest benchmark result" widget (U21) — same shape as
 * [com.galaxyjoy.cpuinfo.feat.shieldwidget.ShieldScoreWidgetProvider], refreshed every 15 minutes.
 * Does NOT use Hilt: the 4 `*ResultPrefs` classes are `@Inject`-annotated for Hilt's own graph, but
 * their constructors are plain public `(Context)` — directly instantiable here exactly like
 * [ShieldScoreWidgetProvider] already does for `DataProviderRam`/`BatteryStatusProvider`.
 */
class LastBenchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    /** U27 — same reasoning as [com.galaxyjoy.cpuinfo.feat.shieldwidget.ShieldScoreWidgetProvider]'s
     * override: react to the user drag-resizing the widget, not just initial placement. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
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
        internal const val WORK_NAME = "last_bench_widget_refresh"

        /** Read by [ActHost] to jump straight to the matching benchmark tab in
         * [com.galaxyjoy.cpuinfo.feat.infor.FrmInfoContainer]'s ViewPager2. */
        const val EXTRA_OPEN_BENCH_TAB = "com.galaxyjoy.cpuinfo.extra.OPEN_BENCH_TAB"

        internal fun schedulePeriodicRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<LastBenchWidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        internal fun cancelPeriodicRefresh(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, LastBenchWidgetProvider::class.java))
            ids.forEach { updateWidget(context, appWidgetManager, it) }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val appContext = context.applicationContext
            val throttle = ThrottleResultPrefs(appContext).getLastResult()
            val storage = StorageBenchResultPrefs(appContext).getLastResult()
            val ram = RamBenchResultPrefs(appContext).getLastResult()
            val gpu = GpuBenchResultPrefs(appContext).getLastResult()
            val latest = LastBenchPicker.pick(throttle, storage, ram, gpu)

            val minHeightDp = appWidgetManager.getAppWidgetOptions(appWidgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val views = buildRemoteViews(
                context, appWidgetId, latest, WidgetSizeClass.isLarge(minHeightDp), throttle, storage, ram, gpu,
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        internal fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            latest: LastBenchPicker.Latest?,
            isLarge: Boolean = false,
            throttle: ThrottleResultPrefs.SavedResult? = null,
            storage: StorageBenchResultPrefs.SavedResult? = null,
            ram: RamBenchResultPrefs.SavedResult? = null,
            gpu: GpuBenchResultPrefs.SavedResult? = null,
        ): RemoteViews {
            val layoutRes = if (isLarge) R.layout.widget_last_bench_large else R.layout.widget_last_bench
            val views = RemoteViews(context.packageName, layoutRes)

            if (isLarge) {
                bindRow(views, context, R.id.widgetLastBenchThrottleRow, LastBenchPicker.Kind.THROTTLE, throttle, storage, ram, gpu)
                bindRow(views, context, R.id.widgetLastBenchStorageRow, LastBenchPicker.Kind.STORAGE, throttle, storage, ram, gpu)
                bindRow(views, context, R.id.widgetLastBenchRamRow, LastBenchPicker.Kind.RAM, throttle, storage, ram, gpu)
                bindRow(views, context, R.id.widgetLastBenchGpuRow, LastBenchPicker.Kind.GPU, throttle, storage, ram, gpu)
            } else if (latest == null) {
                views.setTextViewText(R.id.widgetLastBenchLabel, context.getString(R.string.last_bench_widget_empty))
                views.setTextViewText(R.id.widgetLastBenchValue, "")
            } else {
                views.setTextViewText(R.id.widgetLastBenchLabel, context.getString(labelRes(latest.kind)))
                views.setTextViewText(R.id.widgetLastBenchValue, valueText(context, latest))
            }

            val openAppIntent = Intent(context, ActHost::class.java).apply {
                putExtra(EXTRA_OPEN_BENCH_TAB, latest?.let { tabPositionFor(it.kind) } ?: -1)
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, appWidgetId, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widgetLastBenchRoot, openAppPendingIntent)

            return views
        }

        /** U27 — large layout shows all 4 benchmark types at once (not just the newest one, see
         * the compact layout's [labelRes]/[valueText] usage above), each on its own row, hidden if
         * that type has never been run. Reuses [labelRes]/[valueText] instead of duplicating the
         * per-kind formatting by building the same [LastBenchPicker.Latest] shape each of those
         * already expects. */
        private fun bindRow(
            views: RemoteViews,
            context: Context,
            rowId: Int,
            kind: LastBenchPicker.Kind,
            throttle: ThrottleResultPrefs.SavedResult?,
            storage: StorageBenchResultPrefs.SavedResult?,
            ram: RamBenchResultPrefs.SavedResult?,
            gpu: GpuBenchResultPrefs.SavedResult?,
        ) {
            val latestForKind = latestForKind(kind, throttle, storage, ram, gpu)
            if (latestForKind == null) {
                views.setViewVisibility(rowId, View.GONE)
                return
            }
            views.setViewVisibility(rowId, View.VISIBLE)
            val text = "${context.getString(labelRes(kind))}: ${valueText(context, latestForKind)}"
            views.setTextViewText(rowId, text)
        }

        private fun latestForKind(
            kind: LastBenchPicker.Kind,
            throttle: ThrottleResultPrefs.SavedResult?,
            storage: StorageBenchResultPrefs.SavedResult?,
            ram: RamBenchResultPrefs.SavedResult?,
            gpu: GpuBenchResultPrefs.SavedResult?,
        ): LastBenchPicker.Latest? = when (kind) {
            LastBenchPicker.Kind.THROTTLE -> throttle?.let { LastBenchPicker.Latest(kind, it.timestampMs, throttle = it) }
            LastBenchPicker.Kind.STORAGE -> storage?.let { LastBenchPicker.Latest(kind, it.timestampMs, storage = it) }
            LastBenchPicker.Kind.RAM -> ram?.let { LastBenchPicker.Latest(kind, it.timestampMs, ram = it) }
            LastBenchPicker.Kind.GPU -> gpu?.let { LastBenchPicker.Latest(kind, it.timestampMs, gpu = it) }
        }

        internal fun tabPositionFor(kind: LastBenchPicker.Kind): Int = when (kind) {
            LastBenchPicker.Kind.THROTTLE -> AdtInfoContainerState.THROTTLE_POS
            LastBenchPicker.Kind.STORAGE -> AdtInfoContainerState.STORAGE_BENCH_POS
            LastBenchPicker.Kind.RAM -> AdtInfoContainerState.RAM_BENCH_POS
            LastBenchPicker.Kind.GPU -> AdtInfoContainerState.GPU_BENCH_POS
        }

        private fun labelRes(kind: LastBenchPicker.Kind): Int = when (kind) {
            LastBenchPicker.Kind.THROTTLE -> R.string.all_bench_row_throttle
            LastBenchPicker.Kind.STORAGE -> R.string.all_bench_row_storage
            LastBenchPicker.Kind.RAM -> R.string.all_bench_row_ram
            LastBenchPicker.Kind.GPU -> R.string.all_bench_row_gpu
        }

        private fun valueText(context: Context, latest: LastBenchPicker.Latest): String = when (latest.kind) {
            LastBenchPicker.Kind.THROTTLE -> context.getString(
                R.string.last_bench_widget_value_mhz, latest.throttle?.sustainedFreqMhz ?: 0L,
            )
            LastBenchPicker.Kind.STORAGE -> context.getString(
                R.string.last_bench_widget_value_read_write_mb,
                formatDecimal(latest.storage?.seqWriteMbPerSec ?: 0.0),
                formatDecimal(latest.storage?.seqReadMbPerSec ?: 0.0),
            )
            LastBenchPicker.Kind.RAM -> context.getString(
                R.string.last_bench_widget_value_read_write_mb,
                formatDecimal(latest.ram?.writeMbPerSec ?: 0.0),
                formatDecimal(latest.ram?.readMbPerSec ?: 0.0),
            )
            LastBenchPicker.Kind.GPU -> context.getString(
                R.string.last_bench_widget_value_fps, formatDecimal(latest.gpu?.avgFps ?: 0.0),
            )
        }

        /** Always renders with a "." decimal point, regardless of the device's default locale —
         * same reasoning as [com.galaxyjoy.cpuinfo.feat.allbench.AllBenchScreen]'s `formatDecimal`. */
        private fun formatDecimal(value: Double): String = "%.1f".format(Locale.US, value)
    }
}
