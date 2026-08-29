package com.galaxyjoy.cpuinfo.feat.airead

import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import javax.inject.Inject

class AiReadinessProvider @Inject constructor(
    private val dataNativeProviderCpu: DataNativeProviderCpu,
    private val dataProviderCpu: DataProviderCpu,
    private val dataProviderRam: DataProviderRam,
) {

    fun evaluate(): AiReadinessEvaluator.Result {
        val flags = AiReadinessEvaluator.IsaFlags(
            neonDot = dataNativeProviderCpu.hasArmNeonDot(),
            i8mm = dataNativeProviderCpu.hasArmI8mm(),
            bf16 = dataNativeProviderCpu.hasArmBf16(),
            fp16Arith = dataNativeProviderCpu.hasArmFp16Arith(),
            sve = dataNativeProviderCpu.hasArmSve(),
            sve2 = dataNativeProviderCpu.hasArmSve2(),
        )
        return AiReadinessEvaluator.evaluate(
            flags = flags,
            totalRamBytes = dataProviderRam.getTotalBytes(),
            coreCount = dataProviderCpu.getNumberOfCores(),
        )
    }
}
