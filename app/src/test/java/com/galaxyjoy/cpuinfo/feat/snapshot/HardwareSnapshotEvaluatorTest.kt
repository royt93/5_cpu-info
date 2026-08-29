package com.galaxyjoy.cpuinfo.feat.snapshot

import com.galaxyjoy.cpuinfo.feat.snapshot.HardwareSnapshotEvaluator.FieldKind
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class HardwareSnapshotEvaluatorTest {

    private fun baseSnapshot(): HardwareSnapshot = HardwareSnapshot(
        timestampMillis = 1_000L,
        cpuName = "Snapdragon 8 Gen 3",
        cpuVendorId = 4, // Qualcomm
        cpuUarchId = 0x00300378, // ARM Cortex-A78
        coreCount = 8,
        maxFreqMhz = 3200,
        totalRamBytes = 12L * 1024 * 1024 * 1024,
        availableRamBytes = 6L * 1024 * 1024 * 1024,
        internalStorageTotalBytes = 256L * 1024 * 1024 * 1024,
        internalStorageFreeBytes = 100L * 1024 * 1024 * 1024,
        securityPatchLevel = "2026-01-01",
        glEsVersion = "3.2",
    )

    @Test
    fun `identical snapshots produce no changed rows`() {
        val snapshot = baseSnapshot()

        val result = HardwareSnapshotEvaluator.diff(snapshot, snapshot)

        assertFalse(result.hasIdentityChange)
        assertTrue(result.rows.none { it.changed })
    }

    @Test
    fun `changed SoC name is flagged as identity change`() {
        val old = baseSnapshot()
        val new = old.copy(cpuName = "Different Chip")

        val result = HardwareSnapshotEvaluator.diff(old, new)

        assertTrue(result.hasIdentityChange)
        val row = result.rows.first { it.label == "SoC name" }
        assertTrue(row.changed)
        assertEquals(FieldKind.IDENTITY, row.kind)
        assertEquals("Snapdragon 8 Gen 3", row.oldValue)
        assertEquals("Different Chip", row.newValue)
    }

    @Test
    fun `changed vendor id resolves to display names via ChipCatalog`() {
        val old = baseSnapshot()
        val new = old.copy(cpuVendorId = 6) // Samsung

        val result = HardwareSnapshotEvaluator.diff(old, new)

        val row = result.rows.first { it.label == "Chip vendor" }
        assertTrue(row.changed)
        assertEquals("Qualcomm", row.oldValue)
        assertEquals("Samsung", row.newValue)
    }

    @Test
    fun `only available RAM changing is drift, not an identity change`() {
        val old = baseSnapshot()
        val new = old.copy(availableRamBytes = old.availableRamBytes / 2)

        val result = HardwareSnapshotEvaluator.diff(old, new)

        assertFalse(result.hasIdentityChange)
        val row = result.rows.first { it.label == "Available RAM" }
        assertTrue(row.changed)
        assertEquals(FieldKind.DRIFT, row.kind)
    }

    @Test
    fun `only security patch level changing is drift`() {
        val old = baseSnapshot()
        val new = old.copy(securityPatchLevel = "2026-06-01")

        val result = HardwareSnapshotEvaluator.diff(old, new)

        assertFalse(result.hasIdentityChange)
        val row = result.rows.first { it.label == "Security patch level" }
        assertTrue(row.changed)
        assertEquals(FieldKind.DRIFT, row.kind)
    }

    @Test
    fun `changed total RAM is an identity change`() {
        val old = baseSnapshot()
        val new = old.copy(totalRamBytes = old.totalRamBytes * 2)

        val result = HardwareSnapshotEvaluator.diff(old, new)

        assertTrue(result.hasIdentityChange)
        val row = result.rows.first { it.label == "Total RAM" }
        assertEquals(FieldKind.IDENTITY, row.kind)
        assertTrue(row.changed)
    }

    @Test
    fun `unknown max frequency formats as Unknown rather than a negative number`() {
        val old = baseSnapshot().copy(maxFreqMhz = -1)
        val new = old

        val result = HardwareSnapshotEvaluator.diff(old, new)

        val row = result.rows.first { it.label == "Max CPU frequency" }
        assertEquals("Unknown", row.oldValue)
        assertEquals("Unknown", row.newValue)
    }
}
