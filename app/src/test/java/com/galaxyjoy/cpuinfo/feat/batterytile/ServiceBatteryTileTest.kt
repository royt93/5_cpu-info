package com.galaxyjoy.cpuinfo.feat.batterytile

import org.junit.Test
import kotlin.test.assertEquals

class ServiceBatteryTileTest {

    @Test
    fun `classify buckets level at the 20 and 50 thresholds`() {
        assertEquals(ServiceBatteryTile.BatteryLevel.Low, ServiceBatteryTile.classify(0))
        assertEquals(ServiceBatteryTile.BatteryLevel.Low, ServiceBatteryTile.classify(20))
        assertEquals(ServiceBatteryTile.BatteryLevel.Medium, ServiceBatteryTile.classify(21))
        assertEquals(ServiceBatteryTile.BatteryLevel.Medium, ServiceBatteryTile.classify(50))
        assertEquals(ServiceBatteryTile.BatteryLevel.High, ServiceBatteryTile.classify(51))
        assertEquals(ServiceBatteryTile.BatteryLevel.High, ServiceBatteryTile.classify(100))
    }
}
