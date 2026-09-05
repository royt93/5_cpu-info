package com.galaxyjoy.cpuinfo.feat.storagetruth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E01 — real-device tier: proves the write/verify cycle works against this device's REAL cache
 * directory and REAL flash (not a JVM temp dir), same "runner classes proven for real on-device"
 * precedent as the other `*BenchmarkRunner`s. Uses a small `testSizeBytes` override so this stays
 * fast — the production 512MB default is exercised only by manual smoke testing.
 */
@RunWith(AndroidJUnit4::class)
class StorageTruthRunnerInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val temperatureProvider = TemperatureProvider(appContext)
    private val runner = StorageTruthRunner(appContext, temperatureProvider)

    @Test
    fun run_onRealCacheDir_writesAndVerifiesWithoutMismatches() = runBlocking {
        val states = mutableListOf<StorageTruthRunner.State>()

        runner.run(testSizeBytes = 8L * 1024 * 1024, blockSizeBytes = 1024 * 1024) { states += it }

        val finished = states.filterIsInstance<StorageTruthRunner.State.Finished>().single()
        assertEquals(8, finished.result.blocksTested)
        assertTrue(finished.result.mismatches.isEmpty())
        assertEquals(StorageTruthBenchmark.Verdict.GENUINE, StorageTruthBenchmark.evaluate(finished.result))
        assertTrue("test file should be deleted after the run", appContext.cacheDir.listFiles { f -> f.name.startsWith("storage_truth_") }?.isEmpty() != false)
    }
}
