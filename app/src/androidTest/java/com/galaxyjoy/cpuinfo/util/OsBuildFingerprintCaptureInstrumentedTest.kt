package com.galaxyjoy.cpuinfo.util

import android.content.Context
import android.os.Build
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
 * U29 — real-device tier proving `saveResult()` on all 4 `*ResultPrefs` genuinely captures the
 * real `Build.FINGERPRINT` (not a unit-test-mockable value), the fact
 * [OsUpdateImpactCalculator] relies on to ever detect an update. Same real-SharedPreferences
 * round-trip approach as
 * [com.galaxyjoy.cpuinfo.feat.benchhistory.BenchHistoryExporterInstrumentedTest]. Clears each
 * prefs file first since the real device this runs on already carries history from prior manual
 * smoke testing.
 */
@RunWith(AndroidJUnit4::class)
class OsBuildFingerprintCaptureInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

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
    }

    @Test
    fun throttleSaveResult_persistsRealBuildFingerprint() {
        val prefs = ThrottleResultPrefs(appContext)
        prefs.saveResult(
            ThrottleFingerprint.Result(
                peakFreqMhz = 2800, sustainedFreqMhz = 2600, throttlePercent = 7, throttled = false,
                startTempC = 30, maxTempC = 35, durationMs = 30_000, aborted = false, abortReason = null,
                opsPerSecond = 5_000_000L,
            ),
        )

        assertEquals(Build.FINGERPRINT, prefs.getLastResult()?.osBuildFingerprint)
    }

    @Test
    fun storageSaveResult_persistsRealBuildFingerprint() {
        val prefs = StorageBenchResultPrefs(appContext)
        prefs.saveResult(
            StorageBenchmark.Result(
                seqWriteMbPerSec = 80.0, seqReadMbPerSec = 200.0,
                randomWriteOpsPerSec = 300.0, randomReadOpsPerSec = 400.0, hashMbPerSec = 50.0,
            ),
        )

        assertEquals(Build.FINGERPRINT, prefs.getLastResult()?.osBuildFingerprint)
    }

    @Test
    fun ramSaveResult_persistsRealBuildFingerprint() {
        val prefs = RamBenchResultPrefs(appContext)
        prefs.saveResult(RamBenchmark.Result(writeMbPerSec = 1234.5, readMbPerSec = 2345.6))

        assertEquals(Build.FINGERPRINT, prefs.getLastResult()?.osBuildFingerprint)
    }

    @Test
    fun gpuSaveResult_persistsRealBuildFingerprint() {
        val prefs = GpuBenchResultPrefs(appContext)
        prefs.saveResult(GpuBenchmark.Result(avgFps = 55.5, frameCount = 300, durationMs = 5000))

        assertEquals(Build.FINGERPRINT, prefs.getLastResult()?.osBuildFingerprint)
    }
}
