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

        return ClusterTopologyBuilder.build(rawClusters)
    }
}
