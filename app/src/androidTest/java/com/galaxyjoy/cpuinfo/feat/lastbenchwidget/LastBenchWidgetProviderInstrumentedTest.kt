package com.galaxyjoy.cpuinfo.feat.lastbenchwidget

import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Same "inflate the real RemoteViews through a real LayoutInflater" tier as
 * [com.galaxyjoy.cpuinfo.feat.ramwidget.RamWidgetProviderInstrumentedTest] — `RemoteViews.apply()`
 * needs a real Android rendering pipeline, unavailable under the JVM unit-test stub.
 */
@RunWith(AndroidJUnit4::class)
class LastBenchWidgetProviderInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val themedContext = ContextThemeWrapper(appContext, R.style.AppThemeBase)

    private fun inflate(latest: LastBenchPicker.Latest?): FrameLayout {
        val views = LastBenchWidgetProvider.buildRemoteViews(appContext, appWidgetId = 1, latest)
        val parent = FrameLayout(themedContext)
        val inflated = views.apply(themedContext, parent)
        parent.addView(inflated)
        return parent
    }

    @Test
    fun buildRemoteViews_withNullLatest_showsEmptyState() {
        val root = inflate(null)

        val label = root.findViewById<TextView>(R.id.widgetLastBenchLabel).text.toString()
        val value = root.findViewById<TextView>(R.id.widgetLastBenchValue).text.toString()

        assertEquals(appContext.getString(R.string.last_bench_widget_empty), label)
        assertEquals("", value)
    }

    @Test
    fun buildRemoteViews_withThrottleLatest_rendersMhzValue() {
        val throttle = ThrottleResultPrefs.SavedResult(
            timestampMs = 1_000L, peakFreqMhz = 2800, sustainedFreqMhz = 2600,
            throttlePercent = 7, maxTempC = 40, opsPerSecond = 5_000_000L,
        )
        val latest = LastBenchPicker.pick(throttle, null, null, null)

        val root = inflate(latest)

        val label = root.findViewById<TextView>(R.id.widgetLastBenchLabel).text.toString()
        val value = root.findViewById<TextView>(R.id.widgetLastBenchValue).text.toString()
        assertEquals(appContext.getString(R.string.all_bench_row_throttle), label)
        assertTrue("value should mention 2600 MHz: $value", value.contains("2600"))
    }

    @Test
    fun buildRemoteViews_withGpuLatest_rendersFpsValue() {
        val gpu = GpuBenchResultPrefs.SavedResult(timestampMs = 4_000L, avgFps = 55.5)
        val latest = LastBenchPicker.pick(null, null, null, gpu)

        val root = inflate(latest)

        val value = root.findViewById<TextView>(R.id.widgetLastBenchValue).text.toString()
        assertTrue("value should mention 55.5 FPS: $value", value.contains("55.5"))
    }

    @Test
    fun updateWidget_endToEnd_withRealResultPrefs_picksLatestByTimestamp() {
        val throttlePrefs = ThrottleResultPrefs(appContext)
        val storagePrefs = StorageBenchResultPrefs(appContext)
        val ramPrefs = RamBenchResultPrefs(appContext)
        val gpuPrefs = GpuBenchResultPrefs(appContext)

        val latest = LastBenchPicker.pick(
            throttle = throttlePrefs.getLastResult(),
            storage = storagePrefs.getLastResult(),
            ram = ramPrefs.getLastResult(),
            gpu = gpuPrefs.getLastResult(),
        )

        // No assertion on which kind wins (depends on whatever real benchmarks ran on this
        // device before this test) — just confirms the real end-to-end read + RemoteViews build
        // does not crash, mirroring RamWidgetProviderInstrumentedTest's real-DataProviderRam test.
        val root = inflate(latest)
        assertTrue(root.findViewById<TextView>(R.id.widgetLastBenchLabel).text.isNotEmpty())
    }

    /** U27 — large layout shows all 4 benchmark types at once, each row hidden if that type has
     * never been run, instead of just the single newest one the compact layout shows. */
    @Test
    fun largeLayout_showsAllFourTypes_hidingOnesNeverRun() {
        val throttle = ThrottleResultPrefs.SavedResult(
            timestampMs = 1_000L, peakFreqMhz = 2800, sustainedFreqMhz = 2600,
            throttlePercent = 7, maxTempC = 40, opsPerSecond = 5_000_000L,
        )
        val gpu = GpuBenchResultPrefs.SavedResult(timestampMs = 4_000L, avgFps = 55.5)
        val latest = LastBenchPicker.pick(throttle, null, null, gpu)

        val views = LastBenchWidgetProvider.buildRemoteViews(
            appContext, appWidgetId = 1, latest, isLarge = true,
            throttle = throttle, storage = null, ram = null, gpu = gpu,
        )
        val parent = FrameLayout(themedContext)
        val root = views.apply(themedContext, parent).also { parent.addView(it) }

        val throttleRow = root.findViewById<TextView>(R.id.widgetLastBenchThrottleRow)
        val storageRow = root.findViewById<View>(R.id.widgetLastBenchStorageRow)
        val ramRow = root.findViewById<View>(R.id.widgetLastBenchRamRow)
        val gpuRow = root.findViewById<TextView>(R.id.widgetLastBenchGpuRow)

        assertTrue("throttle row should mention 2600: ${throttleRow.text}", throttleRow.text.contains("2600"))
        assertEquals(View.GONE, storageRow.visibility)
        assertEquals(View.GONE, ramRow.visibility)
        assertTrue("gpu row should mention 55.5: ${gpuRow.text}", gpuRow.text.contains("55.5"))
    }
}
