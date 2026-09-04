package com.galaxyjoy.cpuinfo.util

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchmark
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmark
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmark
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * U24 real end-to-end tier — unlike [BenchPercentileCalculatorTest] (pure math, hand-built
 * lists), this exercises the actual persistence path each benchmark's Done screen uses:
 * `*ResultPrefs.saveResult()` -> real `SharedPreferences`/Gson round-trip -> `getHistory()` ->
 * [BenchPercentileCalculator.percentileOfLast]. Confirms nothing about the real
 * serialize/deserialize path breaks the calculator's assumption that the history list ends with
 * the just-saved run. Clears each benchmark's known SharedPreferences file first since a real
 * device (this suite runs on TECNO) already carries history from prior manual smoke testing.
 */
@RunWith(AndroidJUnit4::class)
class BenchPercentileIntegrationTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearKnownBenchPrefs() {
        listOf(
            "throttle_result_prefs",
            "storage_bench_result_prefs",
            "ram_bench_result_prefs",
            "gpu_bench_result_prefs",
        ).forEach { name ->
            appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test
    fun throttle_realSaveAndHistoryRoundTrip_currentRunIsBestOfThree() {
        val prefs = ThrottleResultPrefs(appContext)
        prefs.saveResult(throttleResult(sustainedFreqMhz = 2000))
        prefs.saveResult(throttleResult(sustainedFreqMhz = 1800))
        prefs.saveResult(throttleResult(sustainedFreqMhz = 2600))

        val percentile = BenchPercentileCalculator.percentileOfLast(
            prefs.getHistory().map { it.sustainedFreqMhz.toDouble() },
        )

        assertEquals(100, percentile)
    }

    @Test
    fun storage_realSaveAndHistoryRoundTrip_currentRunIsWorstOfThree() {
        val prefs = StorageBenchResultPrefs(appContext)
        prefs.saveResult(storageResult(seqWriteMbPerSec = 150.0))
        prefs.saveResult(storageResult(seqWriteMbPerSec = 200.0))
        prefs.saveResult(storageResult(seqWriteMbPerSec = 100.0))

        val percentile = BenchPercentileCalculator.percentileOfLast(
            prefs.getHistory().map { it.seqWriteMbPerSec },
        )

        assertEquals(33, percentile)
    }

    @Test
    fun ram_realSaveAndHistoryRoundTrip_currentRunTiesTheBest() {
        val prefs = RamBenchResultPrefs(appContext)
        prefs.saveResult(ramResult(writeMbPerSec = 4000.0))
        prefs.saveResult(ramResult(writeMbPerSec = 5000.0))
        prefs.saveResult(ramResult(writeMbPerSec = 5000.0))

        val percentile = BenchPercentileCalculator.percentileOfLast(
            prefs.getHistory().map { it.writeMbPerSec },
        )

        assertEquals(100, percentile)
    }

    @Test
    fun gpu_realSaveAndHistoryRoundTrip_onlyOneRunEverReturnsNull() {
        val prefs = GpuBenchResultPrefs(appContext)
        prefs.saveResult(gpuResult(avgFps = 60.0))

        val percentile = BenchPercentileCalculator.percentileOfLast(
            prefs.getHistory().map { it.avgFps },
        )

        assertEquals(null, percentile)
    }

    private fun throttleResult(sustainedFreqMhz: Long) = ThrottleFingerprint.Result(
        peakFreqMhz = sustainedFreqMhz + 200, sustainedFreqMhz = sustainedFreqMhz,
        throttlePercent = 7, throttled = false, startTempC = 30, maxTempC = 35,
        durationMs = 30_000, aborted = false, abortReason = null, opsPerSecond = 5_000_000L,
    )

    private fun storageResult(seqWriteMbPerSec: Double) = StorageBenchmark.Result(
        seqWriteMbPerSec = seqWriteMbPerSec, seqReadMbPerSec = 200.0,
        randomWriteOpsPerSec = 300.0, randomReadOpsPerSec = 400.0, hashMbPerSec = 50.0,
    )

    private fun ramResult(writeMbPerSec: Double) = RamBenchmark.Result(writeMbPerSec = writeMbPerSec, readMbPerSec = 5000.0)

    private fun gpuResult(avgFps: Double) = GpuBenchmark.Result(avgFps = avgFps, frameCount = 300, durationMs = 5000)
}
