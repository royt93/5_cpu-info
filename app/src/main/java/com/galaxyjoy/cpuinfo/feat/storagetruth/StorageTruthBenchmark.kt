package com.galaxyjoy.cpuinfo.feat.storagetruth

import com.galaxyjoy.cpuinfo.util.BenchmarkSafety
import java.util.Random

/**
 * Pure E01 "Storage Truth" model — no Android/file-system deps. [StorageTruthRunner] drives the
 * actual write-then-verify I/O against a real file and feeds it through [findMismatches] here.
 *
 * Detects counterfeit flash that reports more capacity than it physically has: fake controllers
 * silently wrap writes beyond their real capacity back onto already-used physical addresses
 * (`physicalAddress = declaredAddress mod realCapacity`), corrupting whatever was written there
 * before. Writing a distinct, unpredictable pattern per block and verifying it later exposes
 * exactly this — a later block's write corrupting an earlier block's content is something normal
 * flash wear/read errors essentially never do (they either read back clean or throw an I/O
 * error), but is EXACTLY what capacity-fraud wraparound produces.
 *
 * Same "quick free scan only, no VIP deep scan yet" scope decision as this app's other new
 * benchmarks ship an MVP first — see `doc/task/epic-05-new-ideas.md` E01 for what's deferred.
 */
object StorageTruthBenchmark {

    const val BLOCK_SIZE_BYTES = 1 * 1024 * 1024

    /** Total test size for the free quick scan — large enough to catch the most common,
     * egregious counterfeit flash (real capacity under ~512MB, a well-documented scam category
     * for cheap USB sticks/microSD cards), small enough to finish in well under a minute on
     * typical eMMC/UFS write speeds. */
    const val TEST_SIZE_BYTES = 512L * 1024 * 1024

    const val SAFETY_ABORT_TEMP_C = BenchmarkSafety.SAFETY_ABORT_TEMP_C

    enum class Verdict { GENUINE, SUSPECT_FAKE, INCONCLUSIVE }

    enum class AbortReason { OVERHEAT, INTERRUPTED, INSUFFICIENT_SPACE }

    data class MismatchedBlock(val blockIndex: Int, val expectedOffsetBytes: Long)

    data class Result(val blocksTested: Int, val mismatches: List<MismatchedBlock>)

    fun shouldAbortForSafety(tempC: Int): Boolean = BenchmarkSafety.shouldAbortForSafety(tempC)

    fun evaluate(result: Result): Verdict = when {
        result.blocksTested == 0 -> Verdict.INCONCLUSIVE
        result.mismatches.isEmpty() -> Verdict.GENUINE
        else -> Verdict.SUSPECT_FAKE
    }

    /** Deterministic pseudo-random content for a given block index — regenerable from just the
     * index (no need to keep the original bytes in memory to verify later). [java.util.Random]'s
     * algorithm is specified deterministic for a given seed, so the same [blockIndex] always
     * reproduces the exact same bytes within this process. Collision odds between two different
     * indices producing the same content are astronomically small, so any mismatch found by
     * [findMismatches] is a real signal, not coincidence. */
    fun generatePattern(blockIndex: Int, blockSizeBytes: Int = BLOCK_SIZE_BYTES): ByteArray {
        val bytes = ByteArray(blockSizeBytes)
        Random(blockIndex.toLong()).nextBytes(bytes)
        return bytes
    }

    /** Pure comparison core, decoupled from real file I/O so it's unit-testable without a real
     * (or fake) storage device — [readBlock] can be backed by a real file in [StorageTruthRunner]
     * or by a lambda that deliberately returns a wrong-index's pattern in tests, to simulate
     * exactly the wraparound corruption this feature exists to catch. */
    fun findMismatches(blockCount: Int, blockSizeBytes: Int = BLOCK_SIZE_BYTES, readBlock: (Int) -> ByteArray): List<MismatchedBlock> {
        val mismatches = mutableListOf<MismatchedBlock>()
        for (i in 0 until blockCount) {
            val actual = readBlock(i)
            val expected = generatePattern(i, blockSizeBytes)
            if (!actual.contentEquals(expected)) {
                mismatches += MismatchedBlock(i, i.toLong() * blockSizeBytes)
            }
        }
        return mismatches
    }
}
