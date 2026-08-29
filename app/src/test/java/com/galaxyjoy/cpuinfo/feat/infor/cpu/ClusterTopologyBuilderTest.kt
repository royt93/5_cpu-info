package com.galaxyjoy.cpuinfo.feat.infor.cpu

import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder.RawCluster
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterTopologyBuilderTest {

    @Test
    fun `empty input returns empty list`() {
        assertTrue(ClusterTopologyBuilder.build(emptyList()).isEmpty())
    }

    @Test
    fun `single cluster is labeled ALL_CORES`() {
        val result = ClusterTopologyBuilder.build(
            listOf(RawCluster(coreStart = 0, coreCount = 8, vendorId = 4, uarchId = 0x00400102, maxFreqMhz = 2800)),
        )
        assertEquals(1, result.size)
        assertEquals(Tier.ALL_CORES, result[0].tier)
        assertEquals(0 until 8, result[0].coreIndexRange)
    }

    @Test
    fun `two clusters split into PERFORMANCE and EFFICIENCY by frequency`() {
        // Efficiency cluster listed FIRST in native order (core 0-3) but has the LOWER clock —
        // must still be ranked EFFICIENCY, not PERFORMANCE, by frequency not native order.
        val result = ClusterTopologyBuilder.build(
            listOf(
                RawCluster(coreStart = 0, coreCount = 4, vendorId = 3, uarchId = 0x00300355, maxFreqMhz = 1800),
                RawCluster(coreStart = 4, coreCount = 4, vendorId = 3, uarchId = 0x00300378, maxFreqMhz = 2900),
            ),
        )
        assertEquals(Tier.EFFICIENCY, result[0].tier)
        assertEquals(Tier.PERFORMANCE, result[1].tier)
    }

    @Test
    fun `three clusters split into PRIME, PERFORMANCE, EFFICIENCY by frequency`() {
        val result = ClusterTopologyBuilder.build(
            listOf(
                RawCluster(coreStart = 0, coreCount = 2, vendorId = 3, uarchId = 0x00300355, maxFreqMhz = 1800), // little
                RawCluster(coreStart = 2, coreCount = 5, vendorId = 3, uarchId = 0x00300378, maxFreqMhz = 2900), // mid
                RawCluster(coreStart = 7, coreCount = 1, vendorId = 3, uarchId = 0x00300502, maxFreqMhz = 3300), // prime
            ),
        )
        assertEquals(Tier.EFFICIENCY, result[0].tier)
        assertEquals(Tier.PERFORMANCE, result[1].tier)
        assertEquals(Tier.PRIME, result[2].tier)
        assertEquals(1, result[2].coreCount)
        assertEquals(7 until 8, result[2].coreIndexRange)
    }

    @Test
    fun `four or more clusters fall back to UNLABELED rather than guessing`() {
        val result = ClusterTopologyBuilder.build(
            List(4) { i -> RawCluster(coreStart = i * 2, coreCount = 2, vendorId = 3, uarchId = 0, maxFreqMhz = (i * 100).toLong()) },
        )
        assertTrue(result.all { it.tier == Tier.UNLABELED })
    }

    @Test
    fun `vendor and uarch names are resolved via ChipCatalog`() {
        val result = ClusterTopologyBuilder.build(
            listOf(RawCluster(coreStart = 0, coreCount = 4, vendorId = 6, uarchId = 0x00600104, maxFreqMhz = 2900)),
        )
        assertEquals("Samsung", result[0].vendorName)
        assertEquals("Samsung Exynos M5", result[0].uarchName)
    }
}
