package com.galaxyjoy.cpuinfo.feat.devicereport

import android.content.Context
import com.galaxyjoy.cpuinfo.feat.devicecard.DeviceCardProvider
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class DeviceReportExporterTest {

    private val context: Context = mockk(relaxed = true)
    private val deviceCardProvider: DeviceCardProvider = mockk(relaxed = true)
    private val throttlePrefs: ThrottleResultPrefs = mockk()
    private val storagePrefs: StorageBenchResultPrefs = mockk()
    private val ramPrefs: RamBenchResultPrefs = mockk()
    private val gpuPrefs: GpuBenchResultPrefs = mockk()

    private val exporter = DeviceReportExporter(
        context, deviceCardProvider, throttlePrefs, storagePrefs, ramPrefs, gpuPrefs, DispatchersProvider(),
    )

    private val throttleResult = ThrottleResultPrefs.SavedResult(
        timestampMs = 1L, peakFreqMhz = 2800, sustainedFreqMhz = 2600, throttlePercent = 7, maxTempC = 35, opsPerSecond = 5_000_000L,
    )
    private val storageResult = StorageBenchResultPrefs.SavedResult(
        timestampMs = 1L, seqWriteMbPerSec = 100.0, seqReadMbPerSec = 200.0,
        randomWriteOpsPerSec = 300.0, randomReadOpsPerSec = 400.0, hashMbPerSec = 50.0,
    )
    private val ramResult = RamBenchResultPrefs.SavedResult(timestampMs = 1L, writeMbPerSec = 4000.0, readMbPerSec = 5000.0)
    private val gpuResult = GpuBenchResultPrefs.SavedResult(timestampMs = 1L, avgFps = 55.5)

    @Test
    fun `all 4 benchmarks have a saved result - combines correctly`() {
        every { throttlePrefs.getLastResult() } returns throttleResult
        every { storagePrefs.getLastResult() } returns storageResult
        every { ramPrefs.getLastResult() } returns ramResult
        every { gpuPrefs.getLastResult() } returns gpuResult

        val result = exporter.buildCombinedBenchResultsOrNull()

        assertEquals(2600, result?.throttle?.sustainedFreqMhz)
        assertEquals(100.0, result?.storage?.seqWriteMbPerSec)
        assertEquals(200.0, result?.storage?.seqReadMbPerSec)
        assertEquals(4000.0, result?.ram?.writeMbPerSec)
        assertEquals(5000.0, result?.ram?.readMbPerSec)
        assertEquals(55.5, result?.gpu?.avgFps)
    }

    @Test
    fun `missing throttle result - returns null`() {
        every { throttlePrefs.getLastResult() } returns null
        every { storagePrefs.getLastResult() } returns storageResult
        every { ramPrefs.getLastResult() } returns ramResult
        every { gpuPrefs.getLastResult() } returns gpuResult

        assertNull(exporter.buildCombinedBenchResultsOrNull())
    }

    @Test
    fun `missing storage result - returns null`() {
        every { throttlePrefs.getLastResult() } returns throttleResult
        every { storagePrefs.getLastResult() } returns null
        every { ramPrefs.getLastResult() } returns ramResult
        every { gpuPrefs.getLastResult() } returns gpuResult

        assertNull(exporter.buildCombinedBenchResultsOrNull())
    }

    @Test
    fun `missing ram result - returns null`() {
        every { throttlePrefs.getLastResult() } returns throttleResult
        every { storagePrefs.getLastResult() } returns storageResult
        every { ramPrefs.getLastResult() } returns null
        every { gpuPrefs.getLastResult() } returns gpuResult

        assertNull(exporter.buildCombinedBenchResultsOrNull())
    }

    @Test
    fun `missing gpu result - returns null`() {
        every { throttlePrefs.getLastResult() } returns throttleResult
        every { storagePrefs.getLastResult() } returns storageResult
        every { ramPrefs.getLastResult() } returns ramResult
        every { gpuPrefs.getLastResult() } returns null

        assertNull(exporter.buildCombinedBenchResultsOrNull())
    }

    @Test
    fun `nothing ever run - returns null`() {
        every { throttlePrefs.getLastResult() } returns null
        every { storagePrefs.getLastResult() } returns null
        every { ramPrefs.getLastResult() } returns null
        every { gpuPrefs.getLastResult() } returns null

        assertNull(exporter.buildCombinedBenchResultsOrNull())
    }
}
