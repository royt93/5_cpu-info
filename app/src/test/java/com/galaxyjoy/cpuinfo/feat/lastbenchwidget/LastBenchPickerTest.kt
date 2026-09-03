package com.galaxyjoy.cpuinfo.feat.lastbenchwidget

import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastBenchPickerTest {

    private val throttle = ThrottleResultPrefs.SavedResult(
        timestampMs = 1_000L, peakFreqMhz = 2800, sustainedFreqMhz = 2600,
        throttlePercent = 7, maxTempC = 40, opsPerSecond = 5_000_000L,
    )
    private val storage = StorageBenchResultPrefs.SavedResult(
        timestampMs = 2_000L, seqWriteMbPerSec = 100.0, seqReadMbPerSec = 200.0,
        randomWriteOpsPerSec = 300.0, randomReadOpsPerSec = 400.0, hashMbPerSec = 50.0,
    )
    private val ram = RamBenchResultPrefs.SavedResult(timestampMs = 3_000L, writeMbPerSec = 4000.0, readMbPerSec = 5000.0)
    private val gpu = GpuBenchResultPrefs.SavedResult(timestampMs = 4_000L, avgFps = 55.5)

    @Test
    fun `all null returns null`() {
        assertNull(LastBenchPicker.pick(null, null, null, null))
    }

    @Test
    fun `single non-null result wins regardless of kind`() {
        val result = LastBenchPicker.pick(throttle, null, null, null)
        assertEquals(LastBenchPicker.Kind.THROTTLE, result?.kind)
    }

    @Test
    fun `picks the result with the largest timestamp`() {
        val result = LastBenchPicker.pick(throttle, storage, ram, gpu)
        assertEquals(LastBenchPicker.Kind.GPU, result?.kind)
        assertEquals(4_000L, result?.timestampMs)
    }

    @Test
    fun `an older timestamp does not win even if it is the only recently-added arg`() {
        val result = LastBenchPicker.pick(throttle, storage, ram, null)
        assertEquals(LastBenchPicker.Kind.RAM, result?.kind)
    }

    @Test
    fun `equal timestamps still return exactly one result, not a crash`() {
        val sameTimeGpu = gpu.copy(timestampMs = ram.timestampMs)
        val result = LastBenchPicker.pick(null, null, ram, sameTimeGpu)
        assertEquals(ram.timestampMs, result?.timestampMs)
    }
}
