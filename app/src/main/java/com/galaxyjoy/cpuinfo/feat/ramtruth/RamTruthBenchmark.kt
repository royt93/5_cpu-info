package com.galaxyjoy.cpuinfo.feat.ramtruth

/**
 * Pure E03 "RAM Truth" model — no Android deps. [RamTruthRunner] grows a real off-heap allocation
 * chunk by chunk and times each chunk's first-touch write here.
 *
 * Detects marketed "virtual RAM" / "extended RAM" (e.g. "8GB + 8GB extended") where the extra
 * capacity is actually storage-backed swap/zram, not real DRAM: real RAM access latency is
 * roughly flat regardless of how much of it is already committed, while swap-backed pages are
 * dramatically slower to first-fault-in (a real disk/compressed-memory round trip instead of a
 * plain page-table update) — so throughput sampled per chunk as allocation grows should stay flat
 * on genuine RAM and fall off a cliff once the growing allocation crosses into swap-backed
 * territory.
 *
 * A single slow chunk isn't trusted as a cliff on its own — GC pauses, scheduling jitter, or a
 * background app's own allocation burst can transiently slow one measurement on perfectly genuine
 * RAM. Only a *sustained* run of slow chunks counts, which swap-backed memory reliably produces
 * (every subsequent chunk keeps paying the same penalty) but transient noise does not.
 */
object RamTruthBenchmark {

    const val CHUNK_SIZE_BYTES = 32 * 1024 * 1024

    /** Never probe more than this fraction of currently-reported-available RAM, or more than
     * [ABSOLUTE_MAX_PROBE_BYTES] regardless — this test temporarily holds real memory that could
     * otherwise go to other apps, and an Android low-memory kill of THIS process from over-probing
     * is an uncatchable hard crash, not a normal exception. Conservative by design. */
    const val MAX_PROBE_FRACTION_OF_AVAILABLE = 0.25
    const val ABSOLUTE_MAX_PROBE_BYTES = 800L * 1024 * 1024

    const val BASELINE_CHUNK_COUNT = 3
    const val SUSTAINED_CLIFF_CHUNK_COUNT = 3
    const val CLIFF_THROUGHPUT_RATIO = 0.5

    enum class Verdict { GENUINE, SUSPECT_VIRTUAL_RAM, INCONCLUSIVE }

    enum class AbortReason { INTERRUPTED, INSUFFICIENT_AVAILABLE_MEMORY, LOW_MEMORY_SIGNAL }

    data class ChunkMeasurement(val chunkIndex: Int, val mbPerSec: Double)

    data class Result(val measurements: List<ChunkMeasurement>)

    /** Bytes this test is allowed to touch in total, given how much RAM Android currently reports
     * as free. Bounded on both ends: a fraction of what's free, and an absolute safety ceiling. */
    fun probeCapBytes(availMemBytes: Long): Long =
        minOf((availMemBytes * MAX_PROBE_FRACTION_OF_AVAILABLE).toLong(), ABSOLUTE_MAX_PROBE_BYTES)

    /** `null` means "can't evaluate at all" (too few measurements, or a zero/degenerate
     * baseline) — distinct from "evaluated and found no cliff", which [cliffStreakStart] itself
     * returns as `null` too but only after a real baseline was established. Shared by [evaluate]
     * and [cliffAtBytes] so they always agree on where the cliff is. */
    private fun cliffStreakStart(result: Result): Int? {
        if (result.measurements.size < BASELINE_CHUNK_COUNT + SUSTAINED_CLIFF_CHUNK_COUNT) return null
        val baseline = result.measurements.take(BASELINE_CHUNK_COUNT).map { it.mbPerSec }.average()
        if (baseline <= 0.0) return null
        val threshold = baseline * CLIFF_THROUGHPUT_RATIO
        val candidates = result.measurements.drop(BASELINE_CHUNK_COUNT)
        for (i in 0..candidates.size - SUSTAINED_CLIFF_CHUNK_COUNT) {
            if (candidates.subList(i, i + SUSTAINED_CLIFF_CHUNK_COUNT).all { it.mbPerSec < threshold }) {
                return BASELINE_CHUNK_COUNT + i
            }
        }
        return null
    }

    private fun hasUsableBaseline(result: Result): Boolean =
        result.measurements.size >= BASELINE_CHUNK_COUNT + SUSTAINED_CLIFF_CHUNK_COUNT &&
            result.measurements.take(BASELINE_CHUNK_COUNT).map { it.mbPerSec }.average() > 0.0

    fun evaluate(result: Result): Verdict = when {
        !hasUsableBaseline(result) -> Verdict.INCONCLUSIVE
        cliffStreakStart(result) != null -> Verdict.SUSPECT_VIRTUAL_RAM
        else -> Verdict.GENUINE
    }

    /** Byte offset (into the whole probed span) where the sustained slow streak begins, or `null`
     * if [evaluate] didn't return [Verdict.SUSPECT_VIRTUAL_RAM]. */
    fun cliffAtBytes(result: Result): Long? =
        cliffStreakStart(result)?.let { result.measurements[it].chunkIndex.toLong() * CHUNK_SIZE_BYTES }
}
