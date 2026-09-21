package com.galaxyjoy.cpuinfo.feat.ramtile

import org.junit.Test
import kotlin.test.assertEquals

class ServiceRamTileTest {

    @Test
    fun `classify buckets used percentage at the 50 and 75 thresholds`() {
        assertEquals(ServiceRamTile.RAMLoad.Low, ServiceRamTile.classify(0))
        assertEquals(ServiceRamTile.RAMLoad.Low, ServiceRamTile.classify(49))
        assertEquals(ServiceRamTile.RAMLoad.Medium, ServiceRamTile.classify(50))
        assertEquals(ServiceRamTile.RAMLoad.Medium, ServiceRamTile.classify(74))
        assertEquals(ServiceRamTile.RAMLoad.High, ServiceRamTile.classify(75))
        assertEquals(ServiceRamTile.RAMLoad.High, ServiceRamTile.classify(100))
    }
}
