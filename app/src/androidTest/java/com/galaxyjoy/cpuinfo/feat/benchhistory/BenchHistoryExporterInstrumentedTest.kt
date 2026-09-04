package com.galaxyjoy.cpuinfo.feat.benchhistory

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchmark
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real end-to-end tier — unlike [BenchHistoryExporterTest] (MockK-stubbed `getHistory()`), this
 * saves through the actual `*ResultPrefs.saveResult()` -> real SharedPreferences/Gson round-trip
 * -> `getHistory()` -> [BenchHistoryExporter.buildCsv]. Same real-pipeline reasoning as
 * [com.galaxyjoy.cpuinfo.util.BenchPercentileIntegrationTest]. Clears each benchmark's known
 * SharedPreferences file first since the real device this runs on already carries history from
 * prior manual smoke testing.
 */
@RunWith(AndroidJUnit4::class)
class BenchHistoryExporterInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var throttlePrefs: ThrottleResultPrefs
    private lateinit var storagePrefs: StorageBenchResultPrefs
    private lateinit var ramPrefs: RamBenchResultPrefs
    private lateinit var gpuPrefs: GpuBenchResultPrefs
    private lateinit var exporter: BenchHistoryExporter

    @Before
    fun setUp() {
        listOf(
            "throttle_result_prefs",
            "storage_bench_result_prefs",
            "ram_bench_result_prefs",
            "gpu_bench_result_prefs",
        ).forEach { name ->
            appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
        throttlePrefs = ThrottleResultPrefs(appContext)
        storagePrefs = StorageBenchResultPrefs(appContext)
        ramPrefs = RamBenchResultPrefs(appContext)
        gpuPrefs = GpuBenchResultPrefs(appContext)
        exporter = BenchHistoryExporter(throttlePrefs, storagePrefs, ramPrefs, gpuPrefs)
    }

    @Test
    fun emptyHistory_stillProducesAllFourSectionHeaders() {
        val csv = exporter.buildCsv()

        assertTrue(csv.contains("=== CPU Throttle ==="))
        assertTrue(csv.contains("=== Storage ==="))
        assertTrue(csv.contains("=== RAM ==="))
        assertTrue(csv.contains("=== GPU ==="))
    }

    @Test
    fun realSavedThrottleResult_appearsInExportedCsv() {
        throttlePrefs.saveResult(
            ThrottleFingerprint.Result(
                peakFreqMhz = 2800, sustainedFreqMhz = 2600, throttlePercent = 7, throttled = false,
                startTempC = 30, maxTempC = 35, durationMs = 30_000, aborted = false, abortReason = null,
                opsPerSecond = 5_000_000L,
            ),
        )

        val csv = exporter.buildCsv()

        assertTrue("csv was:\n$csv", csv.contains("2800,2600,7,35,5000000"))
    }

    @Test
    fun realSavedGpuResult_formatsFpsWithOneDecimal() {
        gpuPrefs.saveResult(GpuBenchmark.Result(avgFps = 55.5, frameCount = 300, durationMs = 5000))

        val csv = exporter.buildCsv()

        assertTrue("csv was:\n$csv", csv.contains(",55.5"))
    }
}
