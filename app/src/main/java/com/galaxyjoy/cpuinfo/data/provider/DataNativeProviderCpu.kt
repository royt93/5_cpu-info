package com.galaxyjoy.cpuinfo.data.provider

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataNativeProviderCpu @Inject constructor() {

    external fun initLibrary()

    external fun getCpuName(): String

    external fun hasArmNeon(): Boolean

    external fun getL1dCaches(): IntArray?

    external fun getL1iCaches(): IntArray?

    external fun getL2Caches(): IntArray?

    external fun getL3Caches(): IntArray?

    external fun getL4Caches(): IntArray?

    // --- U01 "Device Truth Score" additions below ---

    external fun getCoreCount(): Int

    /** [cpuinfo_vendor] enum ordinal for the given core index, or 0 (unknown) if out of range. */
    external fun getCoreVendor(coreIndex: Int): Int

    /** [cpuinfo_uarch] enum value for the given core index, or 0 (unknown) if out of range. */
    external fun getCoreUarch(coreIndex: Int): Int

    /** Raw MIDR_EL1, already decoded by libcpuinfo from /proc/cpuinfo. -1 if unavailable. */
    external fun getCoreMidr(coreIndex: Int): Long

    /** Raw MPIDR_EL1 via direct register read. -1 if unavailable (non-arm64, or read faulted). */
    external fun getMpidrEl1(): Long

    /** Raw REVIDR_EL1 via direct register read. -1 if unavailable (non-arm64, or read faulted). */
    external fun getRevidrEl1(): Long

    // --- F09/U06 "CPU Cluster Topology" additions below ---

    external fun getClusterCount(): Int

    /** Index of the first logical core in this cluster, or -1 if out of range. */
    external fun getClusterCoreStart(clusterIndex: Int): Int

    external fun getClusterCoreCount(clusterIndex: Int): Int

    /** [cpuinfo_vendor] enum ordinal shared by all cores in this cluster. */
    external fun getClusterVendor(clusterIndex: Int): Int

    /** [cpuinfo_uarch] enum value shared by all cores in this cluster. */
    external fun getClusterUarch(clusterIndex: Int): Int
}
