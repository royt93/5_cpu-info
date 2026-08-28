package com.galaxyjoy.cpuinfo.data.provider

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataProviderCpuTest {

    // Regression for B27: Runtime.availableProcessors() undercounts on big.LITTLE chips when
    // big cores are power-collapsed. parsePossibleCoreCount() reads every core the kernel knows
    // about from /sys/devices/system/cpu/possible instead.

    @Test
    fun `parsePossibleCoreCount handles single range`() {
        assertEquals(8, DataProviderCpu.parsePossibleCoreCount("0-7"))
    }

    @Test
    fun `parsePossibleCoreCount handles multiple comma-separated ranges`() {
        assertEquals(8, DataProviderCpu.parsePossibleCoreCount("0-3,4-7"))
    }

    @Test
    fun `parsePossibleCoreCount handles single core device`() {
        assertEquals(1, DataProviderCpu.parsePossibleCoreCount("0"))
    }

    @Test
    fun `parsePossibleCoreCount handles trailing newline and whitespace`() {
        assertEquals(8, DataProviderCpu.parsePossibleCoreCount("0-7\n"))
    }

    @Test
    fun `parsePossibleCoreCount returns null on blank input`() {
        assertNull(DataProviderCpu.parsePossibleCoreCount(""))
    }

    @Test
    fun `parsePossibleCoreCount returns null on null input`() {
        assertNull(DataProviderCpu.parsePossibleCoreCount(null))
    }

    @Test
    fun `parsePossibleCoreCount returns null on malformed input instead of throwing`() {
        assertNull(DataProviderCpu.parsePossibleCoreCount("not-a-range-at-all-xyz"))
    }

    @Test
    fun `parsePossibleCoreCount returns null on non-numeric range instead of throwing`() {
        assertNull(DataProviderCpu.parsePossibleCoreCount("abc-def"))
    }
}
