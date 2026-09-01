package com.galaxyjoy.cpuinfo.data.provider

import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import com.galaxyjoy.cpuinfo.domain.model.ScreenDensityCategory
import com.galaxyjoy.cpuinfo.domain.model.ScreenSizeCategory
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataProviderScreenTest {

    private val resources: Resources = mockk()
    private val displayManager: DisplayManager = mockk()
    private val configuration = Configuration()
    private val displayMetrics = DisplayMetrics()

    private val provider = DataProviderScreen(resources, displayManager)

    private fun stubBaseResources() {
        every { resources.configuration } returns configuration
        every { resources.displayMetrics } returns displayMetrics
        every { resources.getString(any()) } returns "unknown"
    }

    @Test
    fun `sizeCategory maps SCREENLAYOUT_SIZE_NORMAL to NORMAL`() {
        stubBaseResources()
        configuration.screenLayout = Configuration.SCREENLAYOUT_SIZE_NORMAL
        every { displayManager.getDisplay(Display.DEFAULT_DISPLAY) } returns null

        assertEquals(ScreenSizeCategory.NORMAL, provider.getScreenData().sizeCategory)
    }

    @Test
    fun `sizeCategory maps unrecognised mask to UNKNOWN`() {
        stubBaseResources()
        configuration.screenLayout = 0
        every { displayManager.getDisplay(Display.DEFAULT_DISPLAY) } returns null

        assertEquals(ScreenSizeCategory.UNKNOWN, provider.getScreenData().sizeCategory)
    }

    @Test
    fun `densityCategory maps DENSITY_XHIGH to XHDPI`() {
        stubBaseResources()
        displayMetrics.densityDpi = DisplayMetrics.DENSITY_XHIGH
        every { displayManager.getDisplay(Display.DEFAULT_DISPLAY) } returns null

        assertEquals(ScreenDensityCategory.XHDPI, provider.getScreenData().densityCategory)
    }

    @Test
    fun `displayInfo is null when DisplayManager returns no default display`() {
        stubBaseResources()
        every { displayManager.getDisplay(Display.DEFAULT_DISPLAY) } returns null

        assertNull(provider.getScreenData().displayInfo)
    }

    @Test
    fun `displayInfo carries absolute metrics and realMetrics when display is present`() {
        stubBaseResources()
        val display: Display = mockk()
        every { displayManager.getDisplay(Display.DEFAULT_DISPLAY) } returns display
        every { display.getMetrics(any()) } answers {
            firstArg<DisplayMetrics>().apply {
                widthPixels = 1080
                heightPixels = 2400
            }
        }
        every { display.getRealMetrics(any()) } answers {
            firstArg<DisplayMetrics>().apply {
                widthPixels = 1080
                heightPixels = 2400
                density = 3.0f
                xdpi = 400f
                ydpi = 400f
            }
        }
        every { display.refreshRate } returns 120f
        every { display.rotation } returns 0
        every { display.supportedModes } returns emptyArray()
        every { display.hdrCapabilities } returns null
        every { display.isWideColorGamut } returns true
        every { display.cutout } returns null

        val data = provider.getScreenData()

        val info = data.displayInfo!!
        assertEquals(1080, info.absoluteWidthPx)
        assertEquals(2400, info.absoluteHeightPx)
        assertEquals(120f, info.refreshRate)
        assertEquals(400f, info.xdpi)
        assertEquals(1080, info.realMetrics!!.widthPx)
        assertEquals(360, info.realMetrics!!.dpWidth)
        // Build.VERSION.SDK_INT defaults to 0 on JVM tests (see DataProviderGpuTest) — every
        // SDK-gated field (N/O/P) short-circuits to null regardless of the mocked Display values.
        assertNull(info.hdrTypes)
        assertNull(info.isWideColorGamut)
        assertNull(info.cutoutRectCount)
    }

    @Test
    fun `realMetrics is null when getRealMetrics throws`() {
        stubBaseResources()
        val display: Display = mockk()
        every { displayManager.getDisplay(Display.DEFAULT_DISPLAY) } returns display
        every { display.getMetrics(any()) } just Runs
        every { display.getRealMetrics(any()) } throws RuntimeException("boom")
        every { display.refreshRate } returns 60f
        every { display.rotation } returns 1
        every { display.supportedModes } returns emptyArray()
        every { display.hdrCapabilities } returns null
        every { display.isWideColorGamut } returns false
        every { display.cutout } returns null

        val info = provider.getScreenData().displayInfo!!

        assertNull(info.realMetrics)
        assertEquals(0f, info.xdpi)
        assertEquals(0f, info.ydpi)
        // absolute metrics still populated — that call is unguarded, independent of getRealMetrics
        assertEquals(1, info.rotation)
    }
}
