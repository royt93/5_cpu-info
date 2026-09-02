package com.galaxyjoy.cpuinfo.feat.devicecard

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [DeviceCardRenderer] draws with plain `android.graphics.Canvas`/`Paint`, which JVM unit tests
 * can't exercise under this project's `unitTests.isReturnDefaultValues = true` setup — same reason
 * [com.galaxyjoy.cpuinfo.feat.vipreport.VipDiagnosticReportRepositoryInstrumentedTest] runs on a
 * real device instead. Verifies the bitmap is the right size and that it actually drew content
 * (not a blank canvas), rather than pixel-matching an exact layout.
 */
@RunWith(AndroidJUnit4::class)
class DeviceCardRendererTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val sampleData = DeviceCardData(
        deviceModel = "Test Device Model XL Ultra Pro",
        chipName = "Test Chipset Octa-Core 3.2GHz",
        coreCount = 8,
        ramTotalBytes = 8L * 1024 * 1024 * 1024,
        storageTotalBytes = 128L * 1024 * 1024 * 1024,
        screenResolution = "1080×2400",
        refreshRateHz = 120,
        androidVersion = "Android 15 (API 35)",
        shieldScore = 82,
    )

    @Test
    fun render_producesBitmapOfTheFixedCardSize() {
        val bitmap = DeviceCardRenderer.render(appContext, sampleData)

        assertEquals(DeviceCardRenderer.CARD_WIDTH_PX, bitmap.width)
        assertEquals(DeviceCardRenderer.CARD_HEIGHT_PX, bitmap.height)
    }

    @Test
    fun render_actuallyDrawsContent_notABlankCanvas() {
        val bitmap = DeviceCardRenderer.render(appContext, sampleData)

        val distinctColors = mutableSetOf<Int>()
        var y = 0
        while (y < bitmap.height && distinctColors.size < 2) {
            var x = 0
            while (x < bitmap.width && distinctColors.size < 2) {
                distinctColors.add(bitmap.getPixel(x, y))
                x += 4
            }
            y += 4
        }
        assertTrue("card should have more than one color drawn on it", distinctColors.size >= 2)
    }

    @Test
    fun render_withNullShieldScore_doesNotCrash() {
        val bitmap = DeviceCardRenderer.render(appContext, sampleData.copy(shieldScore = null))

        assertEquals(DeviceCardRenderer.CARD_WIDTH_PX, bitmap.width)
    }

    @Test
    fun render_withVeryLongDeviceModelAndChipName_doesNotCrash() {
        val longData = sampleData.copy(
            deviceModel = "A".repeat(200),
            chipName = "B".repeat(200),
        )

        val bitmap = DeviceCardRenderer.render(appContext, longData)

        assertEquals(DeviceCardRenderer.CARD_WIDTH_PX, bitmap.width)
        assertEquals(DeviceCardRenderer.CARD_HEIGHT_PX, bitmap.height)
    }
}
