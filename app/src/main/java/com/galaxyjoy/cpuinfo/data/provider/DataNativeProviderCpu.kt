package com.galaxyjoy.cpuinfo.data.provider

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class DataNativeProviderCpu @Inject constructor() {

    external fun initLibrary()

    external fun getCpuName(): String

    external fun hasArmNeon(): Boolean

    external fun getL1dCaches(): IntArray?

    external fun getL1iCaches(): IntArray?

    external fun getL2Caches(): IntArray?

    external fun getL3Caches(): IntArray?

    external fun getL4Caches(): IntArray?

    // --- U06 "CPU Cluster Topology" cache-per-cluster additions below ---
    // Parallel arrays to getL1dCaches()/etc above — index i here describes the same cache
    // instance as index i there. processorStart/processorCount are the logical-core range that
    // shares that cache instance, used to attribute each cache to its cluster.

    external fun getL1dCacheProcessorStarts(): IntArray?

    external fun getL1dCacheProcessorCounts(): IntArray?

    external fun getL1iCacheProcessorStarts(): IntArray?

    external fun getL1iCacheProcessorCounts(): IntArray?

    external fun getL2CacheProcessorStarts(): IntArray?

    external fun getL2CacheProcessorCounts(): IntArray?

    external fun getL3CacheProcessorStarts(): IntArray?

    external fun getL3CacheProcessorCounts(): IntArray?

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

    // --- F10/U12 "AI Readiness Score" additions below ---

    /** Int8 matrix multiply extension — key accelerator for quantized on-device LLM inference. */
    external fun hasArmI8mm(): Boolean

    /** bfloat16 support — used by many on-device ML runtimes. */
    external fun hasArmBf16(): Boolean

    /** NEON dot-product instructions — baseline quantized-inference acceleration. */
    external fun hasArmNeonDot(): Boolean

    external fun hasArmSve(): Boolean

    external fun hasArmSve2(): Boolean

    external fun hasArmFp16Arith(): Boolean

    // --- U31 "Cluster Benchmark" additions below ---

    /** Pins the calling thread to logical cores `[coreIndexStart, coreIndexStart+coreIndexCount)`
     * via `sched_setaffinity`. Must be called from the worker thread itself. Returns `false`
     * (rather than throwing) if the kernel/cgroup policy denies it. */
    /** `open` (unlike every other member here) so a JVM unit test can override it with a plain
     * fake — MockK can't stub `external fun`s (they have no bytecode body to proxy; stubbing one
     * throws `UnsatisfiedLinkError` immediately, a known limitation throughout this codebase since
     * U06). [ClusterBenchmarkRunner] is the one caller that branches on this method's return
     * value, so it's the one native call worth unlocking for real unit testing. */
    open external fun setThreadAffinity(coreIndexStart: Int, coreIndexCount: Int): Boolean
}
