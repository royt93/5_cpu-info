package com.galaxyjoy.cpuinfo.feat.devicereport

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.data.provider.DataProviderScreen
import com.galaxyjoy.cpuinfo.data.provider.DataProviderStorage
import com.galaxyjoy.cpuinfo.feat.devicecard.DeviceCardProvider
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchmark
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreProvider
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipFile

/**
 * Real end-to-end tier — builds a real zip via real `Canvas`/`Bitmap` rendering (both card
 * renderers are `android.graphics.*`, stubbed under this project's JVM unit-test setup) and reads
 * it back with a real [ZipFile], same "verify the actual file on disk" tier as
 * [com.galaxyjoy.cpuinfo.feat.benchhistory.BenchHistoryExporterInstrumentedTest].
 */
@RunWith(AndroidJUnit4::class)
class DeviceReportExporterInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var throttlePrefs: ThrottleResultPrefs
    private lateinit var storagePrefs: StorageBenchResultPrefs
    private lateinit var ramPrefs: RamBenchResultPrefs
    private lateinit var gpuPrefs: GpuBenchResultPrefs
    private lateinit var exporter: DeviceReportExporter

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

        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val deviceCardProvider = DeviceCardProvider(
            dataProviderCpu = DataProviderCpu(),
            dataNativeProviderCpu = DataNativeProviderCpu(),
            dataProviderRam = DataProviderRam(activityManager),
            dataProviderStorage = DataProviderStorage(),
            dataProviderScreen = DataProviderScreen(
                appContext.resources,
                appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager,
            ),
            shieldScoreProvider = ShieldScoreProvider(
                dataProviderRam = DataProviderRam(activityManager),
                batteryStatusProvider = BatteryStatusProvider(appContext),
            ),
        )
        exporter = DeviceReportExporter(
            appContext, deviceCardProvider, throttlePrefs, storagePrefs, ramPrefs, gpuPrefs, DispatchersProvider(),
        )
    }

    @After
    fun tearDown() {
        appContext.cacheDir.listFiles { f -> f.name.startsWith("device_report_") }?.forEach(File::delete)
    }

    @Test
    fun noBenchmarksEverRun_zipHasDeviceCardAndCsvOnly_noBenchResultCard() {
        exporter.buildZip()

        val entries = readNewestZipEntryNames()
        assertTrue(entries.contains("device_card.png"))
        assertTrue(entries.contains("benchmark_history.csv"))
        assertTrue("bench_result_card.png should be absent: $entries", !entries.contains("bench_result_card.png"))
        assertNull(exporter.buildCombinedBenchResultsOrNull())
    }

    @Test
    fun allFourBenchmarksRun_zipHasAllThreeEntries() {
        throttlePrefs.saveResult(
            ThrottleFingerprint.Result(
                peakFreqMhz = 2800, sustainedFreqMhz = 2600, throttlePercent = 7, throttled = false,
                startTempC = 30, maxTempC = 35, durationMs = 30_000, aborted = false, abortReason = null,
                opsPerSecond = 5_000_000L,
            ),
        )
        storagePrefs.saveResult(
            com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmark.Result(
                seqWriteMbPerSec = 100.0, seqReadMbPerSec = 200.0,
                randomWriteOpsPerSec = 300.0, randomReadOpsPerSec = 400.0, hashMbPerSec = 50.0,
            ),
        )
        ramPrefs.saveResult(com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmark.Result(writeMbPerSec = 4000.0, readMbPerSec = 5000.0))
        gpuPrefs.saveResult(GpuBenchmark.Result(avgFps = 55.5, frameCount = 300, durationMs = 5000))

        exporter.buildZip()

        val entries = readNewestZipEntryNames()
        assertEquals(setOf("device_card.png", "bench_result_card.png", "benchmark_history.csv"), entries)
    }

    /** [DeviceReportExporter.buildZip] returns a `content://` `FileProvider` URI, not directly
     * openable as a [File] — read the real cache file back by name instead, since this test
     * controls the exporter directly and the cache dir is otherwise empty (per [tearDown]). */
    private fun readNewestZipEntryNames(): Set<String> {
        val zipFile = appContext.cacheDir.listFiles { f -> f.name.startsWith("device_report_") }
            ?.maxByOrNull { it.lastModified() }
            ?: error("no device_report_*.zip found in cache dir")
        return ZipFile(zipFile).use { zip -> zip.entries().asSequence().map { it.name }.toSet() }
    }
}
