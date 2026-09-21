package com.galaxyjoy.cpuinfo.feat.temp

import com.galaxyjoy.cpuinfo.feat.setting.FrmSettings
import com.galaxyjoy.cpuinfo.util.Prefs
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class TemperatureFormatterTest {

    private val prefs: Prefs = mockk()
    private val formatter = TemperatureFormatter(prefs)

    private fun withUnit(unit: Int) {
        every {
            prefs.get(FrmSettings.KEY_TEMPERATURE_UNIT, TemperatureFormatter.CELSIUS.toString())
        } returns unit.toString()
    }

    @Test
    fun `formats as Celsius by default`() {
        withUnit(TemperatureFormatter.CELSIUS)
        assertEquals("25°C", formatter.format(25f))
    }

    @Test
    fun `converts Celsius to Fahrenheit`() {
        withUnit(TemperatureFormatter.FAHRENHEIT)
        // 0C -> 32F
        assertEquals("32.0°F", formatter.format(0f))
        // 100C -> 212F
        assertEquals("212.0°F", formatter.format(100f))
    }

    @Test
    fun `converts Celsius to Kelvin`() {
        withUnit(TemperatureFormatter.KELVIN)
        // 0C -> 273.15K
        assertEquals("273.15°K", formatter.format(0f))
    }
}
