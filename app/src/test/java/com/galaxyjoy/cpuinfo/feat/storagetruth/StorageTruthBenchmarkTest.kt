package com.galaxyjoy.cpuinfo.feat.storagetruth

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageTruthBenchmarkTest {

    @Test
    fun `shouldAbortForSafety matches the shared threshold`() {
        assertFalse(StorageTruthBenchmark.shouldAbortForSafety(StorageTruthBenchmark.SAFETY_ABORT_TEMP_C - 1))
        assertTrue(StorageTruthBenchmark.shouldAbortForSafety(StorageTruthBenchmark.SAFETY_ABORT_TEMP_C))
    }

    @Test
    fun `generatePattern is deterministic for the same block index`() {
        val a = StorageTruthBenchmark.generatePattern(42, 1024)
        val b = StorageTruthBenchmark.generatePattern(42, 1024)

        assertTrue(a.contentEquals(b))
    }

    @Test
    fun `generatePattern differs between block indices`() {
        val a = StorageTruthBenchmark.generatePattern(0, 1024)
        val b = StorageTruthBenchmark.generatePattern(1, 1024)

        assertFalse(a.contentEquals(b))
    }

    @Test
    fun `findMismatches returns empty when every block matches its own expected pattern`() {
        val mismatches = StorageTruthBenchmark.findMismatches(blockCount = 10, blockSizeBytes = 256) { i ->
            StorageTruthBenchmark.generatePattern(i, 256)
        }

        assertTrue(mismatches.isEmpty())
    }

    @Test
    fun `findMismatches detects a single corrupted block`() {
        val mismatches = StorageTruthBenchmark.findMismatches(blockCount = 10, blockSizeBytes = 256) { i ->
            if (i == 3) StorageTruthBenchmark.generatePattern(99, 256) else StorageTruthBenchmark.generatePattern(i, 256)
        }

        assertEquals(1, mismatches.size)
        assertEquals(3, mismatches.single().blockIndex)
        assertEquals(3L * 256, mismatches.single().expectedOffsetBytes)
    }

    @Test
    fun `findMismatches simulates wraparound corruption from a smaller real capacity`() {
        // Models exactly the fraud this feature targets: a fake chip with real capacity of 4
        // blocks silently wraps writes beyond that back onto blocks 0-3, so after writing blocks
        // 0..9 in order, each physical slot ends up holding whichever declared index wrote to it
        // LAST — only the slots where that last writer happens to equal the slot's own identity
        // index (a multiple of realCapacityBlocks apart) still read back correctly.
        val physicalContent = HashMap<Int, ByteArray>()
        val realCapacityBlocks = 4
        for (declaredIndex in 0 until 10) {
            physicalContent[declaredIndex % realCapacityBlocks] = StorageTruthBenchmark.generatePattern(declaredIndex, 256)
        }

        val mismatches = StorageTruthBenchmark.findMismatches(blockCount = 10, blockSizeBytes = 256) { i ->
            physicalContent.getValue(i % realCapacityBlocks)
        }

        // Slot 0's last writer is declared index 8 (0,4,8 -> 8 wins) — so re-reading declared
        // index 0 now yields index 8's pattern: a clear, detectable mismatch.
        assertTrue(mismatches.any { it.blockIndex == 0 })
        // Re-reading declared index 8 itself matches (it WAS the last writer of that slot) — the
        // wraparound is real but not every index reads back wrong, exactly like a real fake chip.
        assertTrue(mismatches.none { it.blockIndex == 8 })
        assertTrue(mismatches.isNotEmpty())
    }

    @Test
    fun `evaluate is GENUINE for zero mismatches`() {
        assertEquals(StorageTruthBenchmark.Verdict.GENUINE, StorageTruthBenchmark.evaluate(StorageTruthBenchmark.Result(10, emptyList())))
    }

    @Test
    fun `evaluate is SUSPECT_FAKE when any mismatch is present`() {
        val result = StorageTruthBenchmark.Result(10, listOf(StorageTruthBenchmark.MismatchedBlock(3, 768)))
        assertEquals(StorageTruthBenchmark.Verdict.SUSPECT_FAKE, StorageTruthBenchmark.evaluate(result))
    }

    @Test
    fun `evaluate is INCONCLUSIVE when zero blocks were tested`() {
        assertEquals(StorageTruthBenchmark.Verdict.INCONCLUSIVE, StorageTruthBenchmark.evaluate(StorageTruthBenchmark.Result(0, emptyList())))
    }
}
