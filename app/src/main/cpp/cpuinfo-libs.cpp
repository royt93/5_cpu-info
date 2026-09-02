#include <cstring>
#include <jni.h>
#include <cinttypes>
#include <android/log.h>
#include <cpuinfo.h>
#include <string>
#include <csetjmp>
#include <csignal>
#include <mutex>

#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, "cpuinfo-libs::", __VA_ARGS__))

#if defined(__aarch64__)
// MPIDR_EL1/REVIDR_EL1 aren't exposed by libcpuinfo or /proc/cpuinfo (unlike MIDR_EL1, which the
// kernel already decodes into /proc/cpuinfo's "CPU implementer/part/variant/revision" fields) —
// U01 (Device Truth Score) reads them directly via `mrs`. Whether EL0 userspace is allowed to
// execute `mrs` against an _EL1 register is kernel/vendor-dependent; on a kernel that traps it,
// the instruction raises SIGILL instead of returning a value. Guard the read with a signal
// handler so an unsupported device gets "unavailable" instead of crashing the app.
static std::mutex g_registerReadMutex;
static sigjmp_buf g_registerReadJmpBuf;

static void handleIllegalInstruction(int) {
    siglongjmp(g_registerReadJmpBuf, 1);
}

static bool readSystemRegisterSafely(uint64_t (*readRegister)(), uint64_t *outValue) {
    std::lock_guard<std::mutex> lock(g_registerReadMutex);

    struct sigaction newAction{};
    struct sigaction oldAction{};
    newAction.sa_handler = handleIllegalInstruction;
    sigemptyset(&newAction.sa_mask);
    newAction.sa_flags = 0;
    if (sigaction(SIGILL, &newAction, &oldAction) != 0) {
        return false;
    }

    bool succeeded = false;
    if (sigsetjmp(g_registerReadJmpBuf, 1) == 0) {
        *outValue = readRegister();
        succeeded = true;
    }

    sigaction(SIGILL, &oldAction, nullptr);
    return succeeded;
}

static uint64_t readMpidrEl1() {
    uint64_t value;
    asm volatile("mrs %0, mpidr_el1" : "=r"(value));
    return value;
}

static uint64_t readRevidrEl1() {
    uint64_t value;
    asm volatile("mrs %0, revidr_el1" : "=r"(value));
    return value;
}
#endif

extern "C"
JNIEXPORT void JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_initLibrary(JNIEnv *env,
                                                                            jobject thiz) {
    if (!cpuinfo_initialize()) {
        LOGI("Error during initialization");
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getCpuName(JNIEnv *env,
                                                                           jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_packages_count() == 0) {
        return env->NewStringUTF("");
    }
    const struct cpuinfo_package *package = cpuinfo_get_package(0);
    if (package == nullptr || package->name[0] == '\0') {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(package->name);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_hasArmNeon(JNIEnv *env,
                                                                           jobject thiz) {
    if (!cpuinfo_initialize()) {
        return false;
    }
    return cpuinfo_has_arm_neon();
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL1dCaches(JNIEnv *env,
                                                                             jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l1d_caches_count() == 0) {
        return nullptr;
    }

    uint32_t cacheCount = cpuinfo_get_l1d_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l1dCaches = cpuinfo_get_l1d_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = l1dCaches[i].size;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL1iCaches(JNIEnv *env,
                                                                             jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l1i_caches_count() == 0) {
        return nullptr;
    }

    uint32_t cacheCount = cpuinfo_get_l1i_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l1iCaches = cpuinfo_get_l1i_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = l1iCaches[i].size;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL2Caches(JNIEnv *env,
                                                                            jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l2_caches_count() == 0) {
        return nullptr;
    }

    uint32_t cacheCount = cpuinfo_get_l2_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l2Caches = cpuinfo_get_l2_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = l2Caches[i].size;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL3Caches(JNIEnv *env,
                                                                            jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l3_caches_count() == 0) {
        return nullptr;
    }

    uint32_t cacheCount = cpuinfo_get_l3_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l3Caches = cpuinfo_get_l3_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = l3Caches[i].size;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL4Caches(JNIEnv *env,
                                                                            jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l4_caches_count() == 0) {
        return nullptr;
    }

    uint32_t cacheCount = cpuinfo_get_l4_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l4Caches = cpuinfo_get_l4_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = l4Caches[i].size;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

// --- U06 "CPU Cluster Topology" cache-per-cluster additions below ---
// Same cache structs read above for .size, now also reading .processor_start/.processor_count —
// the logical-core index (and count of cores sharing that cache instance) needed to attribute
// each cache to the cluster it belongs to. Both fields are unconditional members of
// cpuinfo_cache (cpuinfo.h), populated from /sys/.../cache/index*/shared_cpu_list on ARM.

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL1dCacheProcessorStarts(
        JNIEnv *env, jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l1d_caches_count() == 0) {
        return nullptr;
    }
    uint32_t cacheCount = cpuinfo_get_l1d_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l1dCaches = cpuinfo_get_l1d_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = (jint) l1dCaches[i].processor_start;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL1dCacheProcessorCounts(
        JNIEnv *env, jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l1d_caches_count() == 0) {
        return nullptr;
    }
    uint32_t cacheCount = cpuinfo_get_l1d_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l1dCaches = cpuinfo_get_l1d_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = (jint) l1dCaches[i].processor_count;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL1iCacheProcessorStarts(
        JNIEnv *env, jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l1i_caches_count() == 0) {
        return nullptr;
    }
    uint32_t cacheCount = cpuinfo_get_l1i_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l1iCaches = cpuinfo_get_l1i_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = (jint) l1iCaches[i].processor_start;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL1iCacheProcessorCounts(
        JNIEnv *env, jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l1i_caches_count() == 0) {
        return nullptr;
    }
    uint32_t cacheCount = cpuinfo_get_l1i_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l1iCaches = cpuinfo_get_l1i_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = (jint) l1iCaches[i].processor_count;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL2CacheProcessorStarts(
        JNIEnv *env, jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l2_caches_count() == 0) {
        return nullptr;
    }
    uint32_t cacheCount = cpuinfo_get_l2_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l2Caches = cpuinfo_get_l2_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = (jint) l2Caches[i].processor_start;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL2CacheProcessorCounts(
        JNIEnv *env, jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l2_caches_count() == 0) {
        return nullptr;
    }
    uint32_t cacheCount = cpuinfo_get_l2_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l2Caches = cpuinfo_get_l2_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = (jint) l2Caches[i].processor_count;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL3CacheProcessorStarts(
        JNIEnv *env, jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l3_caches_count() == 0) {
        return nullptr;
    }
    uint32_t cacheCount = cpuinfo_get_l3_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l3Caches = cpuinfo_get_l3_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = (jint) l3Caches[i].processor_start;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getL3CacheProcessorCounts(
        JNIEnv *env, jobject thiz) {
    if (!cpuinfo_initialize() || cpuinfo_get_l3_caches_count() == 0) {
        return nullptr;
    }
    uint32_t cacheCount = cpuinfo_get_l3_caches_count();
    jintArray result = env->NewIntArray(cacheCount);
    jint internalArray[cacheCount];
    auto l3Caches = cpuinfo_get_l3_caches();
    for (uint32_t i = 0; i < cacheCount; i++) {
        internalArray[i] = (jint) l3Caches[i].processor_count;
    }
    env->SetIntArrayRegion(result, 0, cacheCount, internalArray);
    return result;
}

// --- U01 "Device Truth Score" additions below ---

extern "C"
JNIEXPORT jint JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getCoreCount(JNIEnv *env,
                                                                              jobject thiz) {
    if (!cpuinfo_initialize()) {
        return 0;
    }
    return (jint) cpuinfo_get_cores_count();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getCoreVendor(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jint coreIndex) {
    if (!cpuinfo_initialize() || coreIndex < 0) {
        return 0;
    }
    if ((uint32_t) coreIndex >= cpuinfo_get_cores_count()) {
        return 0;
    }
    const struct cpuinfo_core *core = cpuinfo_get_core((uint32_t) coreIndex);
    return core == nullptr ? 0 : (jint) core->vendor;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getCoreUarch(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jint coreIndex) {
    if (!cpuinfo_initialize() || coreIndex < 0) {
        return 0;
    }
    if ((uint32_t) coreIndex >= cpuinfo_get_cores_count()) {
        return 0;
    }
    const struct cpuinfo_core *core = cpuinfo_get_core((uint32_t) coreIndex);
    return core == nullptr ? 0 : (jint) core->uarch;
}

/**
 * Raw MIDR_EL1 value for the core, as already parsed by libcpuinfo from /proc/cpuinfo (no direct
 * register read — safe on every device/kernel). Only meaningful on ARM/ARM64; returns -1 on x86.
 */
extern "C"
JNIEXPORT jlong JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getCoreMidr(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jint coreIndex) {
    if (!cpuinfo_initialize() || coreIndex < 0) {
        return -1;
    }
    if ((uint32_t) coreIndex >= cpuinfo_get_cores_count()) {
        return -1;
    }
    const struct cpuinfo_core *core = cpuinfo_get_core((uint32_t) coreIndex);
    if (core == nullptr) {
        return -1;
    }
#if CPUINFO_ARCH_ARM || CPUINFO_ARCH_ARM64
    return (jlong) core->midr;
#else
    return -1;
#endif
}

/**
 * Direct MPIDR_EL1 read (see the signal-guarded helpers above) — arm64 only, and only if the
 * kernel permits EL0 access. Returns -1 when unsupported (wrong ABI, or the read faulted).
 */
extern "C"
JNIEXPORT jlong JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getMpidrEl1(JNIEnv *env,
                                                                             jobject thiz) {
#if defined(__aarch64__)
    uint64_t value;
    if (readSystemRegisterSafely(readMpidrEl1, &value)) {
        return (jlong) value;
    }
#endif
    return -1;
}

/**
 * Direct REVIDR_EL1 read — same safety caveats as [getMpidrEl1]. REVIDR is IMPLEMENTATION
 * DEFINED (silicon revision/errata bits), so the raw value is shown as evidence for the user to
 * compare externally rather than interpreted by this app.
 */
extern "C"
JNIEXPORT jlong JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getRevidrEl1(JNIEnv *env,
                                                                              jobject thiz) {
#if defined(__aarch64__)
    uint64_t value;
    if (readSystemRegisterSafely(readRevidrEl1, &value)) {
        return (jlong) value;
    }
#endif
    return -1;
}

// --- F09/U06 "CPU Cluster Topology" additions below ---

extern "C"
JNIEXPORT jint JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getClusterCount(JNIEnv *env,
                                                                                 jobject thiz) {
    if (!cpuinfo_initialize()) {
        return 0;
    }
    return (jint) cpuinfo_get_clusters_count();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getClusterCoreStart(JNIEnv *env,
                                                                                     jobject thiz,
                                                                                     jint clusterIndex) {
    if (!cpuinfo_initialize() || clusterIndex < 0 ||
        (uint32_t) clusterIndex >= cpuinfo_get_clusters_count()) {
        return -1;
    }
    const struct cpuinfo_cluster *cluster = cpuinfo_get_cluster((uint32_t) clusterIndex);
    return cluster == nullptr ? -1 : (jint) cluster->core_start;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getClusterCoreCount(JNIEnv *env,
                                                                                     jobject thiz,
                                                                                     jint clusterIndex) {
    if (!cpuinfo_initialize() || clusterIndex < 0 ||
        (uint32_t) clusterIndex >= cpuinfo_get_clusters_count()) {
        return 0;
    }
    const struct cpuinfo_cluster *cluster = cpuinfo_get_cluster((uint32_t) clusterIndex);
    return cluster == nullptr ? 0 : (jint) cluster->core_count;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getClusterVendor(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  jint clusterIndex) {
    if (!cpuinfo_initialize() || clusterIndex < 0 ||
        (uint32_t) clusterIndex >= cpuinfo_get_clusters_count()) {
        return 0;
    }
    const struct cpuinfo_cluster *cluster = cpuinfo_get_cluster((uint32_t) clusterIndex);
    return cluster == nullptr ? 0 : (jint) cluster->vendor;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_getClusterUarch(JNIEnv *env,
                                                                                 jobject thiz,
                                                                                 jint clusterIndex) {
    if (!cpuinfo_initialize() || clusterIndex < 0 ||
        (uint32_t) clusterIndex >= cpuinfo_get_clusters_count()) {
        return 0;
    }
    const struct cpuinfo_cluster *cluster = cpuinfo_get_cluster((uint32_t) clusterIndex);
    return cluster == nullptr ? 0 : (jint) cluster->uarch;
}

// --- F10/U12 "AI Readiness Score" additions below ---
// All of these ISA extension checks are libcpuinfo inline functions already parsed from
// /proc/cpuinfo "Features" — no new register reads, same safety profile as hasArmNeon() above.
// They're no-ops (return false) on non-ARM builds (x86 emulator), matching cpuinfo's own guards.

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_hasArmI8mm(JNIEnv *env,
                                                                            jobject thiz) {
    if (!cpuinfo_initialize()) {
        return false;
    }
#if CPUINFO_ARCH_ARM || CPUINFO_ARCH_ARM64
    return cpuinfo_has_arm_i8mm();
#else
    return false;
#endif
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_hasArmBf16(JNIEnv *env,
                                                                            jobject thiz) {
    if (!cpuinfo_initialize()) {
        return false;
    }
#if CPUINFO_ARCH_ARM || CPUINFO_ARCH_ARM64
    return cpuinfo_has_arm_bf16();
#else
    return false;
#endif
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_hasArmNeonDot(JNIEnv *env,
                                                                               jobject thiz) {
    if (!cpuinfo_initialize()) {
        return false;
    }
#if CPUINFO_ARCH_ARM || CPUINFO_ARCH_ARM64
    return cpuinfo_has_arm_neon_dot();
#else
    return false;
#endif
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_hasArmSve(JNIEnv *env,
                                                                           jobject thiz) {
    if (!cpuinfo_initialize()) {
        return false;
    }
#if CPUINFO_ARCH_ARM || CPUINFO_ARCH_ARM64
    return cpuinfo_has_arm_sve();
#else
    return false;
#endif
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_hasArmSve2(JNIEnv *env,
                                                                            jobject thiz) {
    if (!cpuinfo_initialize()) {
        return false;
    }
#if CPUINFO_ARCH_ARM || CPUINFO_ARCH_ARM64
    return cpuinfo_has_arm_sve2();
#else
    return false;
#endif
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_galaxyjoy_cpuinfo_data_provider_DataNativeProviderCpu_hasArmFp16Arith(JNIEnv *env,
                                                                                 jobject thiz) {
    if (!cpuinfo_initialize()) {
        return false;
    }
#if CPUINFO_ARCH_ARM || CPUINFO_ARCH_ARM64
    return cpuinfo_has_arm_fp16_arith();
#else
    return false;
#endif
}
