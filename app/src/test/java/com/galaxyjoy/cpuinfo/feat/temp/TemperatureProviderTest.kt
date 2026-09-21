package com.galaxyjoy.cpuinfo.feat.temp

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class TemperatureProviderTest {

    private val context: Context = mockk()
    private val provider = TemperatureProvider(context)

    @Test
    fun `divides the raw sticky-intent value by 10 to get Celsius`() {
        val stickyIntent: Intent = mockk()
        every { context.registerReceiver(null, any<IntentFilter>()) } returns stickyIntent
        every { stickyIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) } returns 365

        assertEquals(36, provider.getBatteryTemperature())
    }

    @Test
    fun `missing sticky intent defaults to 0`() {
        every { context.registerReceiver(null, any<IntentFilter>()) } returns null

        assertEquals(0, provider.getBatteryTemperature())
    }
}
