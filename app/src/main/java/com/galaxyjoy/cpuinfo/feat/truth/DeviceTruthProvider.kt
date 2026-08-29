package com.galaxyjoy.cpuinfo.feat.truth

import android.os.Build
import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import javax.inject.Inject

/**
 * Collects the real (native cpuinfo + register reads) and declared (Android Build) data U01
 * needs, then hands it to the pure [DeviceTruthEvaluator]. Core 0 is used as "the" primary core
 * for vendor/uarch/MIDR — on big.LITTLE chips the little cluster is core 0 and typically shares
 * the same silicon vendor as the big cluster, which is what matters for authenticity checks.
 */
class DeviceTruthProvider @Inject constructor(
    private val dataNativeProviderCpu: DataNativeProviderCpu,
    private val dataProviderCpu: DataProviderCpu,
) {

    fun snapshot(): DeviceTruthEvaluator.Snapshot {
        val detectedCoreCount = dataNativeProviderCpu.getCoreCount()
        val primaryCore = 0
        return DeviceTruthEvaluator.Snapshot(
            packageName = dataNativeProviderCpu.getCpuName(),
            primaryCoreVendorId = dataNativeProviderCpu.getCoreVendor(primaryCore),
            primaryCoreUarchId = dataNativeProviderCpu.getCoreUarch(primaryCore),
            primaryCoreMidr = dataNativeProviderCpu.getCoreMidr(primaryCore),
            mpidr = dataNativeProviderCpu.getMpidrEl1(),
            revidr = dataNativeProviderCpu.getRevidrEl1(),
            detectedCoreCount = detectedCoreCount,
            declaredCoreCount = dataProviderCpu.getNumberOfCores(),
            manufacturer = Build.MANUFACTURER ?: "",
            brand = Build.BRAND ?: "",
            model = Build.MODEL ?: "",
        )
    }
}
