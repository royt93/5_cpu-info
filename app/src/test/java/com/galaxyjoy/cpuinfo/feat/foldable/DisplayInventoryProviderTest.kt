package com.galaxyjoy.cpuinfo.feat.foldable

import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class DisplayStateLabelTest {

    @Test
    fun `maps every known Display state constant to a label`() {
        assertEquals("ON", displayStateLabel(Display.STATE_ON))
        assertEquals("OFF", displayStateLabel(Display.STATE_OFF))
        assertEquals("DOZE", displayStateLabel(Display.STATE_DOZE))
        assertEquals("DOZE_SUSPEND", displayStateLabel(Display.STATE_DOZE_SUSPEND))
        assertEquals("ON_SUSPEND", displayStateLabel(Display.STATE_ON_SUSPEND))
        assertEquals("VR", displayStateLabel(Display.STATE_VR))
    }

    @Test
    fun `unknown state constant falls back to UNKNOWN instead of throwing`() {
        assertEquals("UNKNOWN", displayStateLabel(-1))
    }
}

class DisplayInventoryProviderTest {

    private val displayManager: DisplayManager = mockk()

    @Test
    fun `maps each Display into a DisplayInfo with the default flag set correctly`() {
        val defaultDisplay: Display = mockk()
        every { defaultDisplay.displayId } returns Display.DEFAULT_DISPLAY
        every { defaultDisplay.name } returns "Built-in Screen"
        every { defaultDisplay.state } returns Display.STATE_ON
        every { defaultDisplay.getRealMetrics(any()) } answers {
            firstArg<DisplayMetrics>().apply {
                widthPixels = 1080
                heightPixels = 2436
                densityDpi = 420
            }
        }

        val secondaryDisplay: Display = mockk()
        every { secondaryDisplay.displayId } returns Display.DEFAULT_DISPLAY + 1
        every { secondaryDisplay.name } returns "USB-C DP"
        every { secondaryDisplay.state } returns Display.STATE_OFF
        every { secondaryDisplay.getRealMetrics(any()) } answers {
            firstArg<DisplayMetrics>().apply {
                widthPixels = 1920
                heightPixels = 1080
                densityDpi = 160
            }
        }

        every { displayManager.displays } returns arrayOf(defaultDisplay, secondaryDisplay)

        val result = DisplayInventoryProvider(displayManager).listDisplays()

        assertEquals(2, result.size)
        assertEquals(DisplayInfo(Display.DEFAULT_DISPLAY, "Built-in Screen", true, 1080, 2436, 420, "ON"), result[0])
        assertEquals(
            DisplayInfo(Display.DEFAULT_DISPLAY + 1, "USB-C DP", false, 1920, 1080, 160, "OFF"),
            result[1],
        )
    }
}
