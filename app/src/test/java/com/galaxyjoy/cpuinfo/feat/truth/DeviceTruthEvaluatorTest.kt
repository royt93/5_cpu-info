package com.galaxyjoy.cpuinfo.feat.truth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTruthEvaluatorTest {

    private fun baseSnapshot(
        vendorId: Int = 4, // Qualcomm — not brand-locked
        uarchId: Int = 0x00400102, // Kryo
        manufacturer: String = "samsung",
        brand: String = "samsung",
        detectedCoreCount: Int = 8,
        declaredCoreCount: Int = 8,
    ) = DeviceTruthEvaluator.Snapshot(
        packageName = "SM8650",
        primaryCoreVendorId = vendorId,
        primaryCoreUarchId = uarchId,
        primaryCoreMidr = 0x517F802L,
        mpidr = 0x81000000L,
        revidr = 0x0L,
        detectedCoreCount = detectedCoreCount,
        declaredCoreCount = declaredCoreCount,
        manufacturer = manufacturer,
        brand = brand,
        model = "SM-S928B",
    )

    @Test
    fun `non brand-locked vendor never flags mismatch regardless of manufacturer`() {
        val result = DeviceTruthEvaluator.evaluate(baseSnapshot(vendorId = 4, manufacturer = "google"))
        assertFalse(result.hasMismatch)
        val vendorRow = result.rows.first { it.label == "Chip vendor" }
        assertEquals(DeviceTruthEvaluator.Verdict.INFO, vendorRow.verdict)
    }

    @Test
    fun `Samsung Exynos on a Samsung device matches`() {
        val result = DeviceTruthEvaluator.evaluate(
            baseSnapshot(vendorId = 6, uarchId = 0x00600104, manufacturer = "samsung", brand = "samsung")
        )
        val vendorRow = result.rows.first { it.label == "Chip vendor" }
        assertEquals(DeviceTruthEvaluator.Verdict.MATCH, vendorRow.verdict)
        assertFalse(result.hasMismatch)
    }

    @Test
    fun `Samsung Exynos on a non-Samsung device flags mismatch`() {
        val result = DeviceTruthEvaluator.evaluate(
            baseSnapshot(vendorId = 6, uarchId = 0x00600104, manufacturer = "xiaomi", brand = "xiaomi")
        )
        val vendorRow = result.rows.first { it.label == "Chip vendor" }
        assertEquals(DeviceTruthEvaluator.Verdict.MISMATCH, vendorRow.verdict)
        assertTrue(result.hasMismatch)
    }

    @Test
    fun `HiSilicon on a Honor device matches via honor alias`() {
        val result = DeviceTruthEvaluator.evaluate(
            baseSnapshot(vendorId = 15, uarchId = 0x00C00100, manufacturer = "HONOR", brand = "HONOR")
        )
        val vendorRow = result.rows.first { it.label == "Chip vendor" }
        assertEquals(DeviceTruthEvaluator.Verdict.MATCH, vendorRow.verdict)
    }

    @Test
    fun `core count mismatch is flagged but does not crash on unknown counts`() {
        val mismatched = DeviceTruthEvaluator.evaluate(baseSnapshot(detectedCoreCount = 8, declaredCoreCount = 4))
        val coreRow = mismatched.rows.first { it.label == "Core count" }
        assertEquals(DeviceTruthEvaluator.Verdict.MISMATCH, coreRow.verdict)
        assertTrue(mismatched.hasMismatch)

        val unknown = DeviceTruthEvaluator.evaluate(baseSnapshot(detectedCoreCount = 0, declaredCoreCount = 8))
        val unknownRow = unknown.rows.first { it.label == "Core count" }
        assertEquals(DeviceTruthEvaluator.Verdict.INFO, unknownRow.verdict)
        assertEquals("Unknown", unknownRow.detected)
    }

    @Test
    fun `register rows format negative values as unavailable`() {
        val snapshot = baseSnapshot().copy(mpidr = -1L, revidr = -1L)
        val result = DeviceTruthEvaluator.evaluate(snapshot)
        assertEquals("Unavailable", result.rows.first { it.label == "MPIDR_EL1" }.detected)
        assertEquals("Unavailable", result.rows.first { it.label == "REVIDR_EL1" }.detected)
    }

    @Test
    fun `register rows format positive values as zero-padded hex`() {
        val result = DeviceTruthEvaluator.evaluate(baseSnapshot())
        assertEquals("0x0517F802", result.rows.first { it.label == "MIDR_EL1" }.detected)
    }

    @Test
    fun `evaluate always returns exactly 7 evidence rows`() {
        val result = DeviceTruthEvaluator.evaluate(baseSnapshot())
        assertEquals(7, result.rows.size)
    }
}
