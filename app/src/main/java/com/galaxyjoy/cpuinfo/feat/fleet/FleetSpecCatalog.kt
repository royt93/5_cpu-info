package com.galaxyjoy.cpuinfo.feat.fleet

/**
 * U04 "Privacy-preserving Fleet Compare" — a small, hand-curated, offline reference table of the
 * *lowest* RAM/storage configuration ever officially sold for a handful of well-known flagship
 * models (per GSMArena/manufacturer spec sheets). Deliberately not a live "compare against other
 * users' devices" — that would need a server and real user data. Comparing against a bundled
 * reference table instead means this check runs fully offline: nothing about this device is ever
 * sent anywhere, which is a stronger privacy property than the "privacy-preserving aggregation"
 * a live version would need to build.
 *
 * [minRamGb]/[minStorageGb] are the lowest configuration for the model (not a typical/average
 * one) — a real unit can legitimately report anything at or above this, so only a device
 * reporting notably *below* the floor is a meaningful signal (see [FleetCompareEvaluator]'s
 * tolerance margins for how "notably" is defined).
 */
object FleetSpecCatalog {

    data class FleetSpecEntry(
        val displayName: String,
        val modelPrefixes: List<String>,
        val minRamGb: Int,
        val minStorageGb: Int,
    )

    private val ENTRIES: List<FleetSpecEntry> = listOf(
        FleetSpecEntry("Samsung Galaxy S24 Ultra", listOf("SM-S928"), minRamGb = 12, minStorageGb = 256),
        FleetSpecEntry("Samsung Galaxy S23 Ultra", listOf("SM-S918"), minRamGb = 8, minStorageGb = 256),
        FleetSpecEntry("Samsung Galaxy S22 Ultra", listOf("SM-S908"), minRamGb = 8, minStorageGb = 128),
        FleetSpecEntry("Google Pixel 9 Pro", listOf("Pixel 9 Pro"), minRamGb = 16, minStorageGb = 128),
        FleetSpecEntry("Google Pixel 8 Pro", listOf("Pixel 8 Pro"), minRamGb = 12, minStorageGb = 128),
        FleetSpecEntry("Google Pixel 7 Pro", listOf("Pixel 7 Pro"), minRamGb = 12, minStorageGb = 128),
        FleetSpecEntry("Google Pixel 6 Pro", listOf("Pixel 6 Pro"), minRamGb = 12, minStorageGb = 128),
    )

    /** @param buildModel typically [android.os.Build.MODEL]. */
    fun findMatch(buildModel: String): FleetSpecEntry? =
        ENTRIES.firstOrNull { entry -> entry.modelPrefixes.any { buildModel.startsWith(it, ignoreCase = true) } }
}
