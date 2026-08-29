package com.galaxyjoy.cpuinfo.feat.infor.cpu

import com.galaxyjoy.cpuinfo.feat.truth.ChipCatalog

/**
 * Pure logic for F09/U06 "CPU Cluster Topology" — no Android deps. Groups cores by cluster
 * (Prime/Performance/Efficiency on big.LITTLE/DynamIQ chips) instead of the flat "Core 0-N" list
 * the CPU tab already shows, reusing the vendor/uarch catalog built for U01.
 */
object ClusterTopologyBuilder {

    enum class Tier { PRIME, PERFORMANCE, EFFICIENCY, ALL_CORES, UNLABELED }

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
    )

    /**
     * Ranks clusters by max frequency (descending) — the highest-clocked cluster on a
     * multi-tier chip is conventionally the "Prime"/performance-oriented one. Clusters tied on
     * frequency (or when frequency data is unavailable) keep native cluster order.
     */
    fun build(rawClusters: List<RawCluster>): List<Cluster> {
        if (rawClusters.isEmpty()) return emptyList()

        val tiers = tiersFor(rawClusters.size)
        val rankedIndices = rawClusters.indices.sortedByDescending { rawClusters[it].maxFreqMhz }

        val tierByOriginalIndex = HashMap<Int, Tier>()
        rankedIndices.forEachIndexed { rank, originalIndex ->
            tierByOriginalIndex[originalIndex] = tiers.getOrElse(rank) { Tier.UNLABELED }
        }

        return rawClusters.mapIndexed { index, raw ->
            Cluster(
                tier = tierByOriginalIndex[index] ?: Tier.UNLABELED,
                coreIndexRange = raw.coreStart until (raw.coreStart + raw.coreCount),
                coreCount = raw.coreCount,
                vendorName = ChipCatalog.vendorName(raw.vendorId),
                uarchName = ChipCatalog.uarchName(raw.uarchId),
                maxFreqMhz = raw.maxFreqMhz,
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
