package com.galaxyjoy.cpuinfo.feat.truth

/**
 * Pure comparison logic for U01 "Device Truth Score" — no Android deps. Cross-references
 * hardware actually detected by native `cpuinfo` (parsed from real MIDR/register data) against
 * what the device claims about itself, and produces concrete per-field evidence rather than a
 * single opaque score, per the feature's original design goal.
 */
object DeviceTruthEvaluator {

    enum class Verdict { MATCH, MISMATCH, INFO }

    data class Row(
        val label: String,
        val detected: String,
        val declared: String,
        val verdict: Verdict,
    )

    data class Snapshot(
        val packageName: String,
        val primaryCoreVendorId: Int,
        val primaryCoreUarchId: Int,
        val primaryCoreMidr: Long,
        val mpidr: Long,
        val revidr: Long,
        val detectedCoreCount: Int,
        val declaredCoreCount: Int,
        val manufacturer: String,
        val brand: String,
        val model: String,
    )

    data class Result(
        val rows: List<Row>,
        val hasMismatch: Boolean,
    )

    fun evaluate(snapshot: Snapshot): Result {
        val rows = mutableListOf<Row>()

        rows += Row(
            label = "SoC",
            detected = snapshot.packageName.ifBlank { "Unknown" },
            declared = "${snapshot.manufacturer} ${snapshot.model}".trim(),
            verdict = Verdict.INFO,
        )

        val vendorName = ChipCatalog.vendorName(snapshot.primaryCoreVendorId)
        val vendorVerdict = when {
            !ChipCatalog.isBrandLocked(snapshot.primaryCoreVendorId) -> Verdict.INFO
            ChipCatalog.isPlausibleBrand(snapshot.primaryCoreVendorId, snapshot.manufacturer) ||
                ChipCatalog.isPlausibleBrand(snapshot.primaryCoreVendorId, snapshot.brand) -> Verdict.MATCH
            else -> Verdict.MISMATCH
        }
        rows += Row(
            label = "Chip vendor",
            detected = vendorName,
            declared = snapshot.manufacturer,
            verdict = vendorVerdict,
        )

        rows += Row(
            label = "Microarchitecture",
            detected = ChipCatalog.uarchName(snapshot.primaryCoreUarchId),
            declared = "-",
            verdict = Verdict.INFO,
        )

        val coreCountVerdict = if (snapshot.detectedCoreCount > 0 && snapshot.declaredCoreCount > 0) {
            if (snapshot.detectedCoreCount == snapshot.declaredCoreCount) Verdict.MATCH else Verdict.MISMATCH
        } else {
            Verdict.INFO
        }
        rows += Row(
            label = "Core count",
            detected = snapshot.detectedCoreCount.takeIf { it > 0 }?.toString() ?: "Unknown",
            declared = snapshot.declaredCoreCount.takeIf { it > 0 }?.toString() ?: "Unknown",
            verdict = coreCountVerdict,
        )

        rows += Row(
            label = "MIDR_EL1",
            detected = formatRegister(snapshot.primaryCoreMidr),
            declared = "-",
            verdict = Verdict.INFO,
        )
        rows += Row(
            label = "MPIDR_EL1",
            detected = formatRegister(snapshot.mpidr),
            declared = "-",
            verdict = Verdict.INFO,
        )
        rows += Row(
            label = "REVIDR_EL1",
            detected = formatRegister(snapshot.revidr),
            declared = "-",
            verdict = Verdict.INFO,
        )

        return Result(rows = rows, hasMismatch = rows.any { it.verdict == Verdict.MISMATCH })
    }

    private fun formatRegister(value: Long): String =
        if (value < 0) "Unavailable" else "0x%08X".format(value)
}
