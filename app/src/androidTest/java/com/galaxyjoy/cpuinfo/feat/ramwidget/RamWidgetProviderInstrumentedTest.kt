package com.galaxyjoy.cpuinfo.feat.ramwidget

import android.app.ActivityManager
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.domain.model.RamData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [RemoteViews][android.widget.RemoteViews] can only be meaningfully verified by actually
 * inflating it through the real Android rendering pipeline (`RemoteViews.apply()` needs a real
 * `LayoutInflater`/`Context`, unavailable under the JVM unit-test stub) — same class of
 * limitation as `Uri.Builder`/`GradientDrawable` elsewhere in this codebase. Covers both:
 * "widget test" (does the RemoteViews inflate into the expected View tree for known input) and
 * "integration test" (does it work end-to-end against a real on-device [DataProviderRam]).
 */
@RunWith(AndroidJUnit4::class)
class RamWidgetProviderInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val themedContext = ContextThemeWrapper(appContext, R.style.AppThemeBase)

    private fun inflate(ramData: RamData): FrameLayout {
        val views = RamWidgetProvider.buildRemoteViews(appContext, appWidgetId = 1, ramData)
        val parent = FrameLayout(themedContext)
        val inflated = views.apply(themedContext, parent)
        parent.addView(inflated)
        return parent
    }

    @Test
    fun buildRemoteViews_rendersPercentageAndDetailText_forKnownRamData() {
        val ramData = RamData(
            total = 8L * 1024 * 1024 * 1024,
            available = 2L * 1024 * 1024 * 1024,
            availablePercentage = 25,
            threshold = 256L * 1024 * 1024,
        )

        val root = inflate(ramData)

        val percentText = root.findViewById<TextView>(R.id.widgetRamPercent).text.toString()
        val detailText = root.findViewById<TextView>(R.id.widgetRamDetail).text.toString()
        val progress = root.findViewById<ProgressBar>(R.id.widgetRamProgress).progress

        assertEquals("75%", percentText)
        assertEquals(75, progress)
        assertTrue("detail text should mention the available MB: $detailText", detailText.contains("2048"))
        assertTrue("detail text should mention the total MB: $detailText", detailText.contains("8192"))
    }

    @Test
    fun buildRemoteViews_coercesOutOfRangePercentage_insteadOfCrashingProgressBar() {
        // availablePercentage > 100 is not expected from a real device, but the widget must not
        // crash if ActivityManager.MemoryInfo ever reports something odd on some OEM ROM.
        val ramData = RamData(total = 100L, available = 200L, availablePercentage = 200, threshold = 0L)

        val root = inflate(ramData)

        val progress = root.findViewById<ProgressBar>(R.id.widgetRamProgress).progress
        assertEquals(0, progress)
    }

    @Test
    fun updateWidget_endToEnd_withRealDataProviderRam_producesConsistentReadout() {
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val dataProviderRam = DataProviderRam(activityManager)
        val ramData = RamData(
            total = dataProviderRam.getTotalBytes(),
            available = dataProviderRam.getAvailableBytes(),
            availablePercentage = dataProviderRam.getAvailablePercentage(),
            threshold = dataProviderRam.getThreshold(),
        )

        val root = inflate(ramData)

        val expectedUsedPercentage = (100 - ramData.availablePercentage).coerceIn(0, 100)
        val percentText = root.findViewById<TextView>(R.id.widgetRamPercent).text.toString()
        val progress = root.findViewById<ProgressBar>(R.id.widgetRamProgress).progress

        assertEquals("$expectedUsedPercentage%", percentText)
        assertEquals(expectedUsedPercentage, progress)
    }
}
