package com.galaxyjoy.cpuinfo.feat.snapshot

import com.galaxyjoy.cpuinfo.feat.truth.ChipCatalog

/**
 * U03 "Hardware Diff/Snapshot" — a point-in-time capture of the fields that matter for spotting
 * device changes over time (board swap, spoofing) or normal drift (storage/RAM usage, OTA patch
 * level). Kept flat/primitive so it round-trips through Gson without a custom adapter.
 */
data class HardwareSnapshot(
    val timestampMillis: Long,
    val cpuName: String,
    val cpuVendorId: Int,
    val cpuUarchId: Int,
    val coreCount: Int,
    val maxFreqMhz: Int,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val internalStorageTotalBytes: Long,
    val internalStorageFreeBytes: Long,
    val securityPatchLevel: String,
    val glEsVersion: String,
)

object HardwareSnapshotEvaluator {

    /**
     * IDENTITY fields describe the physical device — a change is a signal worth surfacing
     * (board swap, spoofed props, different chip). DRIFT fields are expected to change on every
     * capture (free storage, available RAM, patch level after an OTA) and are shown for context
     * only, never flagged as suspicious.
     */
    enum class FieldKind { IDENTITY, DRIFT }

    data class DiffRow(
        val label: String,
        val oldValue: String,
        val newValue: String,
        val changed: Boolean,
        val kind: FieldKind,
    )

    data class DiffResult(val rows: List<DiffRow>, val hasIdentityChange: Boolean)

    fun diff(old: HardwareSnapshot, new: HardwareSnapshot): DiffResult {
        val rows = listOf(
            row("SoC name", old.cpuName, new.cpuName, FieldKind.IDENTITY) { it },
            row("Chip vendor", old.cpuVendorId, new.cpuVendorId, FieldKind.IDENTITY, ChipCatalog::vendorName),
            row("Microarchitecture", old.cpuUarchId, new.cpuUarchId, FieldKind.IDENTITY, ChipCatalog::uarchName),
            row("Core count", old.coreCount, new.coreCount, FieldKind.IDENTITY) { it.toString() },
            row("Max CPU frequency", old.maxFreqMhz, new.maxFreqMhz, FieldKind.IDENTITY, ::formatMhz),
            row("Total RAM", old.totalRamBytes, new.totalRamBytes, FieldKind.IDENTITY, ::formatBytes),
            row("Available RAM", old.availableRamBytes, new.availableRamBytes, FieldKind.DRIFT, ::formatBytes),
            row(
                "Internal storage capacity",
                old.internalStorageTotalBytes,
                new.internalStorageTotalBytes,
                FieldKind.IDENTITY,
                ::formatBytes,
            ),
            row(
                "Internal storage free",
                old.internalStorageFreeBytes,
                new.internalStorageFreeBytes,
                FieldKind.DRIFT,
                ::formatBytes,
            ),
            row("Security patch level", old.securityPatchLevel, new.securityPatchLevel, FieldKind.DRIFT) { it },
            row("GLES version", old.glEsVersion, new.glEsVersion, FieldKind.IDENTITY) { it },
        )
        return DiffResult(rows = rows, hasIdentityChange = rows.any { it.kind == FieldKind.IDENTITY && it.changed })
    }

    private fun <T> row(
        label: String,
        oldRaw: T,
        newRaw: T,
        kind: FieldKind,
        format: (T) -> String,
    ): DiffRow = DiffRow(
        label = label,
        oldValue = format(oldRaw),
        newValue = format(newRaw),
        changed = oldRaw != newRaw,
        kind = kind,
    )

    private fun formatMhz(mhz: Int): String = if (mhz > 0) "$mhz MHz" else "Unknown"

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "Unknown"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return "%.2f GB".format(gb)
    }
}
