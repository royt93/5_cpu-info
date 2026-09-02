package com.galaxyjoy.cpuinfo.feat.infor.cpu

import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import javax.inject.Inject

/**
 * Bridges native cluster data (F09/U06) with real sysfs frequency limits, then hands both to
 * the pure [ClusterTopologyBuilder]. Max frequency comes from sysfs (same source the existing
 * flat per-core list already uses), not libcpuinfo's `cluster.frequency` field, which is
 * unreliable on Android — the sysfs cpufreq nodes are the ground truth this app already trusts.
 */
class ClusterTopologyProvider @Inject constructor(
    private val dataNativeProviderCpu: DataNativeProviderCpu,
    private val dataProviderCpu: DataProviderCpu,
) {

    fun clusters(): List<ClusterTopologyBuilder.Cluster> {
        val clusterCount = dataNativeProviderCpu.getClusterCount()
        if (clusterCount == 0) return emptyList()

        val rawCaches = rawCachesFor(ClusterTopologyBuilder.CacheLevel.L1I) {
            Triple(
                dataNativeProviderCpu.getL1iCaches(),
                dataNativeProviderCpu.getL1iCacheProcessorStarts(),
                dataNativeProviderCpu.getL1iCacheProcessorCounts(),
            )
        } + rawCachesFor(ClusterTopologyBuilder.CacheLevel.L1D) {
            Triple(
                dataNativeProviderCpu.getL1dCaches(),
                dataNativeProviderCpu.getL1dCacheProcessorStarts(),
                dataNativeProviderCpu.getL1dCacheProcessorCounts(),
            )
        } + rawCachesFor(ClusterTopologyBuilder.CacheLevel.L2) {
            Triple(
                dataNativeProviderCpu.getL2Caches(),
                dataNativeProviderCpu.getL2CacheProcessorStarts(),
                dataNativeProviderCpu.getL2CacheProcessorCounts(),
            )
        } + rawCachesFor(ClusterTopologyBuilder.CacheLevel.L3) {
            Triple(
                dataNativeProviderCpu.getL3Caches(),
                dataNativeProviderCpu.getL3CacheProcessorStarts(),
                dataNativeProviderCpu.getL3CacheProcessorCounts(),
            )
        }

        val rawClusters = (0 until clusterCount).mapNotNull { clusterIndex ->
            val coreStart = dataNativeProviderCpu.getClusterCoreStart(clusterIndex)
            val coreCount = dataNativeProviderCpu.getClusterCoreCount(clusterIndex)
            if (coreStart < 0 || coreCount <= 0) return@mapNotNull null

            val maxFreqMhz = (coreStart until coreStart + coreCount)
                .map { dataProviderCpu.getMinMaxFreq(it).second }
                .filter { it > 0 }
                .maxOrNull() ?: 0L

            ClusterTopologyBuilder.RawCluster(
                coreStart = coreStart,
                coreCount = coreCount,
                vendorId = dataNativeProviderCpu.getClusterVendor(clusterIndex),
                uarchId = dataNativeProviderCpu.getClusterUarch(clusterIndex),
                maxFreqMhz = maxFreqMhz,
            )
        }

        return ClusterTopologyBuilder.build(rawClusters, rawCaches)
    }

    private inline fun rawCachesFor(
        level: ClusterTopologyBuilder.CacheLevel,
        fetch: () -> Triple<IntArray?, IntArray?, IntArray?>,
    ): List<ClusterTopologyBuilder.RawCache> {
        val (sizes, starts, counts) = fetch()
        if (sizes == null || starts == null || counts == null) return emptyList()
        if (sizes.size != starts.size || sizes.size != counts.size) return emptyList()
        return sizes.indices.map { i -> ClusterTopologyBuilder.RawCache(level, sizes[i], starts[i], counts[i]) }
    }
}
