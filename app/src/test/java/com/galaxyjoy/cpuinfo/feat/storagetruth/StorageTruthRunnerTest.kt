package com.galaxyjoy.cpuinfo.feat.storagetruth

import android.content.Context
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real JVM unit test — genuine [java.io.RandomAccessFile] I/O against a real temp directory (no
 * Android framework needed for that part), with [Context] mocked just to redirect `cacheDir` to
 * it. This is the first `*BenchmarkRunner` in this codebase whose write/verify wiring gets a JVM
 * test at all (the other 4 rely on real-device-only proof) — worth the exception here because a
 * silent bug in the write-then-verify plumbing (e.g. reading before the write phase actually
 * finished, an off-by-one in block indexing) would make this feature always report GENUINE
 * regardless of real corruption, defeating its entire purpose.
 */
class StorageTruthRunnerTest {

    private val tempDir = Files.createTempDirectory("storage_truth_test").toFile()
    private val temperatureProvider: TemperatureProvider = mockk {
        every { getBatteryTemperature() } returns 25
    }
    private val context: Context = mockk {
        every { cacheDir } returns tempDir
    }
    private val runner = StorageTruthRunner(context, temperatureProvider)

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `a normal write-verify cycle reports GENUINE with zero mismatches`() = runBlocking {
        val states = mutableListOf<StorageTruthRunner.State>()

        runner.run(testSizeBytes = 10L * 1024, blockSizeBytes = 1024) { states += it }

        val finished = states.filterIsInstance<StorageTruthRunner.State.Finished>().single()
        assertEquals(10, finished.result.blocksTested)
        assertTrue(finished.result.mismatches.isEmpty())
        assertEquals(StorageTruthBenchmark.Verdict.GENUINE, StorageTruthBenchmark.evaluate(finished.result))
    }

    @Test
    fun `the test file is deleted after a successful run`() = runBlocking {
        runner.run(testSizeBytes = 10L * 1024, blockSizeBytes = 1024) {}

        assertTrue(tempDir.listFiles()?.isEmpty() != false)
    }

    @Test
    fun `reports Writing then Verifying states in order`() = runBlocking {
        val states = mutableListOf<StorageTruthRunner.State>()

        runner.run(testSizeBytes = 4L * 1024, blockSizeBytes = 1024) { states += it }

        val running = states.filterIsInstance<StorageTruthRunner.State.Running>()
        assertEquals(StorageTruthRunner.Phase.WRITING, running.first().phase)
        assertEquals(StorageTruthRunner.Phase.VERIFYING, running.last().phase)
    }

    @Test
    fun `overheating temperature aborts before writing any block`() = runBlocking {
        val hotTemperatureProvider: TemperatureProvider = mockk {
            every { getBatteryTemperature() } returns StorageTruthBenchmark.SAFETY_ABORT_TEMP_C
        }
        val hotRunner = StorageTruthRunner(context, hotTemperatureProvider)
        val states = mutableListOf<StorageTruthRunner.State>()

        hotRunner.run(testSizeBytes = 4L * 1024, blockSizeBytes = 1024) { states += it }

        assertEquals(StorageTruthBenchmark.AbortReason.OVERHEAT, states.filterIsInstance<StorageTruthRunner.State.Aborted>().single().reason)
        assertTrue(states.filterIsInstance<StorageTruthRunner.State.Running>().isEmpty())
    }

    @Test
    fun `requestStop during writing aborts as interrupted with no finished result`() = runBlocking {
        val states = mutableListOf<StorageTruthRunner.State>()

        runner.run(testSizeBytes = 10L * 1024, blockSizeBytes = 1024) { state ->
            states += state
            if (state is StorageTruthRunner.State.Running && state.phase == StorageTruthRunner.Phase.WRITING) {
                runner.requestStop()
            }
        }

        assertEquals(StorageTruthBenchmark.AbortReason.INTERRUPTED, states.filterIsInstance<StorageTruthRunner.State.Aborted>().single().reason)
        assertTrue(states.filterIsInstance<StorageTruthRunner.State.Finished>().isEmpty())
    }

    @Test
    fun `insufficient free space aborts before touching the file system`() = runBlocking {
        val tinyDir = Files.createTempDirectory("storage_truth_tiny").toFile()
        val tinyContext: Context = mockk { every { cacheDir } returns tinyDir }
        val tinyRunner = StorageTruthRunner(tinyContext, temperatureProvider)
        val states = mutableListOf<StorageTruthRunner.State>()

        // usableSpace on a real temp dir is real disk free space, always far more than this — so
        // ask for an impossibly large test size instead of trying to fill the real disk.
        tinyRunner.run(testSizeBytes = Long.MAX_VALUE / 4, blockSizeBytes = 1024) { states += it }
        tinyDir.deleteRecursively()

        assertEquals(StorageTruthBenchmark.AbortReason.INSUFFICIENT_SPACE, states.filterIsInstance<StorageTruthRunner.State.Aborted>().single().reason)
    }
}
