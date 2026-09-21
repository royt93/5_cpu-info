package com.galaxyjoy.cpuinfo.feat.cputile

import org.junit.Test
import kotlin.test.assertEquals

class ServiceCpuTileTest {

    @Test
    fun `classify buckets avg freq into thirds of the min-max range`() {
        // range 1000..4000, thirds = 1000
        assertEquals(ServiceCpuTile.CPULoad.Low, ServiceCpuTile.classify(1000, 1000, 4000))
        assertEquals(ServiceCpuTile.CPULoad.Medium, ServiceCpuTile.classify(2500, 1000, 4000))
        assertEquals(ServiceCpuTile.CPULoad.High, ServiceCpuTile.classify(3500, 1000, 4000))
    }

    @Test
    fun `classify does not crash when min equals max`() {
        assertEquals(ServiceCpuTile.CPULoad.High, ServiceCpuTile.classify(2000, 2000, 2000))
    }
}
