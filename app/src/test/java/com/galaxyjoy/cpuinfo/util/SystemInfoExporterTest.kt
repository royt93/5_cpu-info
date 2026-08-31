package com.galaxyjoy.cpuinfo.util

import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderGpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals

/**
 * Regression test for B26 — number formatting in the exported report must not depend on the
 * device's default locale (a German/Vietnamese/Arabic locale uses "," or Eastern digits for the
 * decimal separator, which broke re-parsing the exported JSON/TEXT report).
 */
class SystemInfoExporterTest {

    private val exporter = SystemInfoExporter(
        dataProviderCpu = mockk<DataProviderCpu>(relaxed = true),
        dataProviderRam = mockk<DataProviderRam>(relaxed = true),
        dataProviderGpu = mockk<DataProviderGpu>(relaxed = true),
        dataNativeProviderCpu = mockk<DataNativeProviderCpu>(relaxed = true),
        sensorManager = mockk<SensorManager>(relaxed = true),
        cameraManager = mockk<CameraManager>(relaxed = true),
        displayManager = mockk<DisplayManager>(relaxed = true),
        dispatchersProvider = DispatchersProvider(),
    )

    private lateinit var originalLocale: Locale

    @Before
    fun saveDefaultLocale() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun restoreDefaultLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `formatTwoDecimals uses a dot regardless of default locale`() {
        val locales = listOf(Locale.GERMANY, Locale("vi", "VN"), Locale("ar", "SA"), Locale.US)

        for (locale in locales) {
            Locale.setDefault(locale)
            assertEquals(
                "3.14",
                exporter.formatTwoDecimals(3.14159),
                "wrong decimal separator with default locale $locale",
            )
        }
    }

    @Test
    fun `formatTwoDecimals rounds to exactly two decimal places`() {
        Locale.setDefault(Locale.US)
        assertEquals("0.00", exporter.formatTwoDecimals(0.0))
        assertEquals("1.10", exporter.formatTwoDecimals(1.1))
        assertEquals("3.00", exporter.formatTwoDecimals(2.999))
    }
}
