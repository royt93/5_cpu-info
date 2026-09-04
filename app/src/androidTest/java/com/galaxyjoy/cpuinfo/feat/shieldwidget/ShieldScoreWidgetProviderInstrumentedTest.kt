package com.galaxyjoy.cpuinfo.feat.shieldwidget

import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Same "inflate the real RemoteViews through a real LayoutInflater" tier as
 * [com.galaxyjoy.cpuinfo.feat.ramwidget.RamWidgetProviderInstrumentedTest]. U27 focus: the
 * resizable large-layout branch — the compact layout itself already existed pre-U27.
 */
@RunWith(AndroidJUnit4::class)
class ShieldScoreWidgetProviderInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val themedContext = ContextThemeWrapper(appContext, R.style.AppThemeBase)

    private val result = ShieldScoreCalculator.Result(overall = 82, ramScore = 74, storageScore = 61, batteryScore = 90)

    private fun inflate(isLarge: Boolean): FrameLayout {
        val views = ShieldScoreWidgetProvider.buildRemoteViews(appContext, appWidgetId = 1, result, isLarge)
        val parent = FrameLayout(themedContext)
        val inflated = views.apply(themedContext, parent)
        parent.addView(inflated)
        return parent
    }

    @Test
    fun compactLayout_showsOverallScoreOnly_noBreakdownRows() {
        val root = inflate(isLarge = false)

        assertEquals("82", root.findViewById<TextView>(R.id.widgetShieldScorePercent).text.toString())
        assertNull(root.findViewById<View>(R.id.widgetShieldScoreRamRow))
    }

    @Test
    fun largeLayout_showsOverallScoreAndAllThreeBreakdownRows() {
        val root = inflate(isLarge = true)

        assertEquals("82", root.findViewById<TextView>(R.id.widgetShieldScorePercent).text.toString())
        val ramText = root.findViewById<TextView>(R.id.widgetShieldScoreRamRow).text.toString()
        val storageText = root.findViewById<TextView>(R.id.widgetShieldScoreStorageRow).text.toString()
        val batteryText = root.findViewById<TextView>(R.id.widgetShieldScoreBatteryRow).text.toString()

        assertEquals("${appContext.getString(R.string.ram)}: 74", ramText)
        assertEquals("${appContext.getString(R.string.storage)}: 61", storageText)
        assertEquals("${appContext.getString(R.string.battery)}: 90", batteryText)
    }
}
