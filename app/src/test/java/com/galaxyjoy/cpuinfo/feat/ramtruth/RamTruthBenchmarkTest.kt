package com.galaxyjoy.cpuinfo.feat.ramtruth

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RamTruthBenchmarkTest {

    private fun chunk(index: Int, mbPerSec: Double) = RamTruthBenchmark.ChunkMeasurement(index, mbPerSec)

    @Test
    fun `probeCapBytes is the fraction of available memory when below the absolute ceiling`() {
        val availMem = 1_000L * 1024 * 1024 // 1000MB
        val expected = (availMem * RamTruthBenchmark.MAX_PROBE_FRACTION_OF_AVAILABLE).toLong()
        assertEquals(expected, RamTruthBenchmark.probeCapBytes(availMem))
    }

    @Test
    fun `probeCapBytes is capped at the absolute ceiling on a device with huge available memory`() {
        val hugeAvailMem = 100_000L * 1024 * 1024 // 100GB free
        assertEquals(RamTruthBenchmark.ABSOLUTE_MAX_PROBE_BYTES, RamTruthBenchmark.probeCapBytes(hugeAvailMem))
    }

    @Test
    fun `evaluate is INCONCLUSIVE when fewer measurements than baseline plus sustained window exist`() {
        val result = RamTruthBenchmark.Result(listOf(chunk(0, 1000.0), chunk(1, 1000.0)))
        assertEquals(RamTruthBenchmark.Verdict.INCONCLUSIVE, RamTruthBenchmark.evaluate(result))
    }

    @Test
    fun `evaluate is GENUINE when throughput stays flat across all chunks`() {
        val measurements = (0 until 10).map { chunk(it, 1000.0) }
        assertEquals(RamTruthBenchmark.Verdict.GENUINE, RamTruthBenchmark.evaluate(RamTruthBenchmark.Result(measurements)))
    }

    @Test
    fun `evaluate ignores a single transient slow chunk surrounded by fast ones`() {
        val measurements = (0 until 10).map { i -> chunk(i, if (i == 5) 50.0 else 1000.0) }
        assertEquals(RamTruthBenchmark.Verdict.GENUINE, RamTruthBenchmark.evaluate(RamTruthBenchmark.Result(measurements)))
    }

    @Test
    fun `evaluate is SUSPECT_VIRTUAL_RAM when a sustained slow streak follows a fast baseline`() {
        // Baseline (chunks 0-2) fast, then chunks 3-5 (>= SUSTAINED_CLIFF_CHUNK_COUNT) all slow.
        val measurements = listOf(
            chunk(0, 1000.0), chunk(1, 1000.0), chunk(2, 1000.0),
            chunk(3, 100.0), chunk(4, 100.0), chunk(5, 100.0),
        )
        assertEquals(RamTruthBenchmark.Verdict.SUSPECT_VIRTUAL_RAM, RamTruthBenchmark.evaluate(RamTruthBenchmark.Result(measurements)))
    }

    @Test
    fun `cliffAtBytes points at the first chunk of the sustained slow streak`() {
        val measurements = listOf(
            chunk(0, 1000.0), chunk(1, 1000.0), chunk(2, 1000.0),
            chunk(3, 100.0), chunk(4, 100.0), chunk(5, 100.0),
        )
        val result = RamTruthBenchmark.Result(measurements)

        assertEquals(3L * RamTruthBenchmark.CHUNK_SIZE_BYTES, RamTruthBenchmark.cliffAtBytes(result))
    }

    @Test
    fun `cliffAtBytes is null when the verdict is not SUSPECT_VIRTUAL_RAM`() {
        val measurements = (0 until 10).map { chunk(it, 1000.0) }
        assertNull(RamTruthBenchmark.cliffAtBytes(RamTruthBenchmark.Result(measurements)))
    }

    @Test
    fun `evaluate is INCONCLUSIVE when the baseline itself measured zero throughput`() {
        val measurements = (0 until 10).map { chunk(it, 0.0) }
        assertEquals(RamTruthBenchmark.Verdict.INCONCLUSIVE, RamTruthBenchmark.evaluate(RamTruthBenchmark.Result(measurements)))
    }
}
