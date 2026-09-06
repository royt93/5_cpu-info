package com.galaxyjoy.cpuinfo.feat.ramtruth

import android.app.ActivityManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real JVM unit test — genuine [java.nio.ByteBuffer.allocateDirect] off-heap allocation (works on
 * a plain JVM, no Android device needed for that part), with [Context]/[ActivityManager] mocked
 * just to control `getMemoryInfo()`'s reported `availMem`/`lowMemory`. Worth doing (unlike the 4
 * pre-existing `*BenchmarkRunner`s, which are real-device-only) for the same reason
 * `StorageTruthRunnerTest` is: a silent wiring bug here (e.g. the low-memory check never actually
 * firing) would make this feature either always pass or crash real devices, and that's exactly the
 * kind of mistake worth catching before it ships.
 */
class RamTruthRunnerTest {

    private fun activityManager(availMemBytes: Long, lowMemory: Boolean = false): ActivityManager = mockk {
        every { getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.availMem = availMemBytes
            info.lowMemory = lowMemory
        }
    }

    private fun context(availMemBytes: Long, lowMemory: Boolean = false): Context = mockk {
        every { getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager(availMemBytes, lowMemory)
    }

    @Test
    fun `a normal run measures one chunk per probe-capped chunk count`() = runBlocking {
        val availMem = 2_000L * 1024 * 1024 // 2000MB free -> probeCap = 500MB (25%) -> 15 chunks of 32MB
        val runner = RamTruthRunner(context(availMem))
        val states = mutableListOf<RamTruthRunner.State>()

        runner.run { states += it }

        val finished = states.filterIsInstance<RamTruthRunner.State.Finished>().single()
        val expectedChunkCount = (RamTruthBenchmark.probeCapBytes(availMem) / RamTruthBenchmark.CHUNK_SIZE_BYTES).toInt()
        assertEquals(expectedChunkCount, finished.result.measurements.size)
        assertTrue(finished.result.measurements.all { it.mbPerSec > 0 })
    }

    @Test
    fun `insufficient available memory aborts before allocating anything`() = runBlocking {
        val runner = RamTruthRunner(context(availMemBytes = 10L * 1024 * 1024)) // 10MB free -> probeCap tiny
        val states = mutableListOf<RamTruthRunner.State>()

        runner.run { states += it }

        assertEquals(
            RamTruthBenchmark.AbortReason.INSUFFICIENT_AVAILABLE_MEMORY,
            states.filterIsInstance<RamTruthRunner.State.Aborted>().single().reason,
        )
        assertTrue(states.filterIsInstance<RamTruthRunner.State.Running>().isEmpty())
    }

    @Test
    fun `a low-memory signal aborts before the next chunk is allocated`() = runBlocking {
        val runner = RamTruthRunner(context(availMemBytes = 2_000L * 1024 * 1024, lowMemory = true))
        val states = mutableListOf<RamTruthRunner.State>()

        runner.run { states += it }

        assertEquals(
            RamTruthBenchmark.AbortReason.LOW_MEMORY_SIGNAL,
            states.filterIsInstance<RamTruthRunner.State.Aborted>().single().reason,
        )
        assertTrue(states.filterIsInstance<RamTruthRunner.State.Finished>().isEmpty())
    }

    @Test
    fun `requestStop during probing aborts as interrupted with no finished result`() = runBlocking {
        val runner = RamTruthRunner(context(availMemBytes = 2_000L * 1024 * 1024))
        val states = mutableListOf<RamTruthRunner.State>()

        runner.run { state ->
            states += state
            if (state is RamTruthRunner.State.Running) runner.requestStop()
        }

        assertEquals(
            RamTruthBenchmark.AbortReason.INTERRUPTED,
            states.filterIsInstance<RamTruthRunner.State.Aborted>().single().reason,
        )
        assertTrue(states.filterIsInstance<RamTruthRunner.State.Finished>().isEmpty())
    }
}
