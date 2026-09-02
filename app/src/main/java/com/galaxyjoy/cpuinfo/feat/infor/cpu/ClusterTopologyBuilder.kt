package com.galaxyjoy.cpuinfo.feat.infor.cpu

import com.galaxyjoy.cpuinfo.feat.truth.ChipCatalog

/**
 * Pure logic for F09/U06 "CPU Cluster Topology" — no Android deps. Groups cores by cluster
 * (Prime/Performance/Efficiency on big.LITTLE/DynamIQ chips) instead of the flat "Core 0-N" list
 * the CPU tab already shows, reusing the vendor/uarch catalog built for U01.
 */
object ClusterTopologyBuilder {

    enum class Tier { PRIME, PERFORMANCE, EFFICIENCY, ALL_CORES, UNLABELED }

    enum class CacheLevel(val label: String) {
        L1I("L1i"), L1D("L1d"), L2("L2"), L3("L3")
    }

    /** One cache instance already attributed to a cluster — [sharedCoreCount] is how many
     * logical cores share this exact instance (1 = private per-core, e.g. typical L1; >1 =
     * shared, e.g. L2/L3 often shared by a whole cluster). */
    data class CacheEntry(
        val level: CacheLevel,
        val sizeBytes: Int,
        val sharedCoreCount: Int,
    )

    /** One cache instance chip-wide, NOT yet attributed to any cluster — as read straight off
     * native processor_start/processor_count, before [build] matches it to whichever cluster's
     * core range contains [processorStart]. */
    data class RawCache(
        val level: CacheLevel,
        val sizeBytes: Int,
        val processorStart: Int,
        val processorCount: Int,
    )

    data class RawCluster(
        val coreStart: Int,
        val coreCount: Int,
        val vendorId: Int,
        val uarchId: Int,
        /** Highest max-frequency (MHz) among this cluster's cores, from sysfs — used only to
         * rank clusters by tier, not displayed directly. 0 if unavailable. */
        val maxFreqMhz: Long,
    )

    data class Cluster(
        val tier: Tier,
        val coreIndexRange: IntRange,
        val coreCount: Int,
        val vendorName: String,
        val uarchName: String,
        val maxFreqMhz: Long,
        val caches: List<CacheEntry> = emptyList(),
    )

    /**
     * Ranks clusters by max frequency (descending) — the highest-clocked cluster on a
     * multi-tier chip is conventionally the "Prime"/performance-oriented one. Clusters tied on
     * frequency (or when frequency data is unavailable) keep native cluster order.
     *
     * [rawCaches] is flat and chip-wide (not pre-split by cluster) — a cache instance belongs to
     * a cluster if its first sharing core ([RawCache.processorStart]) falls inside that
     * cluster's core range. Caches never span multiple clusters on real big.LITTLE/DynamIQ
     * topologies (private per-core, or shared within one cluster), so matching on
     * `processorStart` alone (not the full range) is sufficient. At most one instance per level
     * is kept per cluster (the first match) since all cores within one cluster share the same
     * uarch and therefore the same private-cache size.
     */
    fun build(rawClusters: List<RawCluster>, rawCaches: List<RawCache> = emptyList()): List<Cluster> {
        if (rawClusters.isEmpty()) return emptyList()

        val tiers = tiersFor(rawClusters.size)
        val rankedIndices = rawClusters.indices.sortedByDescending { rawClusters[it].maxFreqMhz }

        val tierByOriginalIndex = HashMap<Int, Tier>()
        rankedIndices.forEachIndexed { rank, originalIndex ->
            tierByOriginalIndex[originalIndex] = tiers.getOrElse(rank) { Tier.UNLABELED }
        }

        val cachesByLevel = rawCaches.groupBy { it.level }

        return rawClusters.mapIndexed { index, raw ->
            val coreRange = raw.coreStart until (raw.coreStart + raw.coreCount)
            val caches = cachesByLevel.mapNotNull { (level, instances) ->
                instances.firstOrNull { it.processorStart in coreRange }
                    ?.let { CacheEntry(level, it.sizeBytes, it.processorCount) }
            }

            Cluster(
                tier = tierByOriginalIndex[index] ?: Tier.UNLABELED,
                coreIndexRange = coreRange,
                coreCount = raw.coreCount,
                vendorName = ChipCatalog.vendorName(raw.vendorId),
                uarchName = ChipCatalog.uarchName(raw.uarchId),
                maxFreqMhz = raw.maxFreqMhz,
                caches = caches,
            )
        }
    }

    private fun tiersFor(clusterCount: Int): List<Tier> = when (clusterCount) {
        1 -> listOf(Tier.ALL_CORES)
        2 -> listOf(Tier.PERFORMANCE, Tier.EFFICIENCY)
        3 -> listOf(Tier.PRIME, Tier.PERFORMANCE, Tier.EFFICIENCY)
        else -> List(clusterCount) { Tier.UNLABELED }
    }
}
