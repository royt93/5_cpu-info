package com.galaxyjoy.cpuinfo.feat.siliconlottery

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiliconLotteryBenchmarkTest {

    private fun core(index: Int, ops: Long, affinityConfirmed: Boolean = true) =
        SiliconLotteryBenchmark.CoreResult(index, ops, affinityConfirmed)

    @Test
    fun `shouldAbortForSafety matches the shared threshold`() {
        assertFalse(SiliconLotteryBenchmark.shouldAbortForSafety(SiliconLotteryBenchmark.SAFETY_ABORT_TEMP_C - 1))
        assertTrue(SiliconLotteryBenchmark.shouldAbortForSafety(SiliconLotteryBenchmark.SAFETY_ABORT_TEMP_C))
    }

    @Test
    fun `strongest and weakest pick the max and min ops-per-second core`() {
        val result = SiliconLotteryBenchmark.Result(listOf(core(0, 100), core(1, 300), core(2, 200)))

        assertEquals(1, SiliconLotteryBenchmark.strongest(result)?.coreIndex)
        assertEquals(0, SiliconLotteryBenchmark.weakest(result)?.coreIndex)
    }

    @Test
    fun `strongest and weakest are null for an empty result`() {
        val result = SiliconLotteryBenchmark.Result(emptyList())

        assertNull(SiliconLotteryBenchmark.strongest(result))
        assertNull(SiliconLotteryBenchmark.weakest(result))
    }

    @Test
    fun `spreadPercent computes the percentage gap between strongest and weakest`() {
        val result = SiliconLotteryBenchmark.Result(listOf(core(0, 500), core(1, 1000)))

        assertEquals(50.0, SiliconLotteryBenchmark.spreadPercent(result))
    }

    @Test
    fun `spreadPercent is zero for a single core`() {
        val result = SiliconLotteryBenchmark.Result(listOf(core(0, 1000)))

        assertEquals(0.0, SiliconLotteryBenchmark.spreadPercent(result))
    }

    @Test
    fun `spreadPercent is zero for an empty result`() {
        assertEquals(0.0, SiliconLotteryBenchmark.spreadPercent(SiliconLotteryBenchmark.Result(emptyList())))
    }

    @Test
    fun `spreadPercent is zero when the strongest core measured zero ops`() {
        val result = SiliconLotteryBenchmark.Result(listOf(core(0, 0), core(1, 0)))

        assertEquals(0.0, SiliconLotteryBenchmark.spreadPercent(result))
    }

    @Test
    fun `spreadPercent is zero when all cores are equally fast`() {
        val result = SiliconLotteryBenchmark.Result(listOf(core(0, 1000), core(1, 1000), core(2, 1000)))

        assertEquals(0.0, SiliconLotteryBenchmark.spreadPercent(result))
    }
}
