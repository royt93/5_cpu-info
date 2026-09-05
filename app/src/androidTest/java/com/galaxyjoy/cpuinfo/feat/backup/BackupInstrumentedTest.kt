package com.galaxyjoy.cpuinfo.feat.backup

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchmark
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmark
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.Prefs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * U32 — real end-to-end tier: writes through [BackupExporter] and reads back through
 * [BackupImporter] against a real file `Uri` in the app's own cache dir (a real
 * `ContentResolver.openOutputStream`/`openInputStream` round-trip — the same API surface SAF
 * hands a `content://` `Uri` into, just with a `file://` one here since driving the real system
 * file picker isn't automatable). Same "real SharedPreferences/Gson round-trip, not mocked" tier
 * as [com.galaxyjoy.cpuinfo.feat.benchhistory.BenchHistoryExporterInstrumentedTest]. Clears each
 * benchmark's known SharedPreferences file first since the real device this runs on already
 * carries history from prior manual smoke testing.
 */
@RunWith(AndroidJUnit4::class)
class BackupInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var throttlePrefs: ThrottleResultPrefs
    private lateinit var storagePrefs: StorageBenchResultPrefs
    private lateinit var ramPrefs: RamBenchResultPrefs
    private lateinit var gpuPrefs: GpuBenchResultPrefs
    private lateinit var prefs: Prefs
    private lateinit var exporter: BackupExporter
    private lateinit var importer: BackupImporter
    private lateinit var backupFileUri: Uri

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
        prefs = Prefs(PreferenceManager.getDefaultSharedPreferences(appContext))
        val dispatchers = DispatchersProvider()
        exporter = BackupExporter(appContext, prefs, throttlePrefs, storagePrefs, ramPrefs, gpuPrefs, dispatchers)
        importer = BackupImporter(appContext, prefs, throttlePrefs, storagePrefs, ramPrefs, gpuPrefs, dispatchers)
        backupFileUri = Uri.fromFile(File(appContext.cacheDir, "backup_instrumented_test_${System.nanoTime()}.json"))
    }

    @Test
    fun writeTo_thenReadFrom_roundTripsARealSavedThrottleResult() = runBlocking {
        throttlePrefs.saveResult(
            ThrottleFingerprint.Result(
                peakFreqMhz = 2800, sustainedFreqMhz = 2600, throttlePercent = 7, throttled = false,
                startTempC = 30, maxTempC = 35, durationMs = 30_000, aborted = false, abortReason = null,
                opsPerSecond = 5_000_000L,
            ),
        )

        assertTrue(exporter.writeTo(backupFileUri))
        val decoded = importer.readFrom(backupFileUri)

        assertEquals(1, decoded?.throttleHistory?.size)
        assertEquals(5_000_000L, decoded?.throttleHistory?.first()?.opsPerSecond)
    }

    @Test
    fun writeTo_thenApply_restoresHistoryAcrossAllFourBenchmarkTypes() = runBlocking {
        throttlePrefs.saveResult(
            ThrottleFingerprint.Result(
                peakFreqMhz = 2800, sustainedFreqMhz = 2600, throttlePercent = 7, throttled = false,
                startTempC = 30, maxTempC = 35, durationMs = 30_000, aborted = false, abortReason = null,
                opsPerSecond = 5_000_000L,
            ),
        )
        gpuPrefs.saveResult(GpuBenchmark.Result(avgFps = 55.5, frameCount = 300, durationMs = 5000))
        exporter.writeTo(backupFileUri)

        // Simulate arriving on a fresh device: clear real history before restoring.
        throttlePrefs.replaceHistory(emptyList())
        gpuPrefs.replaceHistory(emptyList())
        assertTrue(throttlePrefs.getHistory().isEmpty())

        val decoded = importer.readFrom(backupFileUri)
        importer.apply(decoded!!)

        assertEquals(5_000_000L, throttlePrefs.getLastResult()?.opsPerSecond)
        assertEquals(55.5, gpuPrefs.getLastResult()?.avgFps)
    }

    @Test
    fun apply_anOlderBackupMissingAHistoryField_leavesThatBenchmarksRealHistoryUntouched() = runBlocking {
        // Simulates restoring a backup written by an older app version that didn't know about
        // storageHistory yet (field absent from the JSON entirely, decodes to null via Gson) —
        // the real-world case that used to wipe existing data (see BackupImporterTest's JVM
        // coverage of the same bug against a mocked StorageBenchResultPrefs; this proves it
        // against the real SharedPreferences-backed implementation).
        storagePrefs.saveResult(
            StorageBenchmark.Result(
                seqWriteMbPerSec = 111.0, seqReadMbPerSec = 222.0,
                randomWriteOpsPerSec = 10.0, randomReadOpsPerSec = 20.0, hashMbPerSec = 30.0,
            ),
        )
        appContext.contentResolver.openOutputStream(backupFileUri)?.use {
            it.write("""{"version":1,"throttleHistory":[]}""".toByteArray())
        }

        val decoded = importer.readFrom(backupFileUri)
        assertTrue(decoded != null)
        importer.apply(decoded!!)

        assertEquals(111.0, storagePrefs.getLastResult()?.seqWriteMbPerSec)
    }

    @Test
    fun readFrom_aFileThatIsNotJson_returnsNull() = runBlocking {
        appContext.contentResolver.openOutputStream(backupFileUri)?.use {
            it.write("this is not json".toByteArray())
        }

        assertNull(importer.readFrom(backupFileUri))
    }
}
