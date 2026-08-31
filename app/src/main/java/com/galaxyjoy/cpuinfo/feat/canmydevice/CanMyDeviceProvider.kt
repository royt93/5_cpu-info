package com.galaxyjoy.cpuinfo.feat.canmydevice

import com.galaxyjoy.cpuinfo.data.provider.DataProviderCamera
import com.galaxyjoy.cpuinfo.data.provider.DataProviderDrm
import com.galaxyjoy.cpuinfo.data.provider.DataProviderGpu
import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessProvider
import javax.inject.Inject

/**
 * Gathers the capability data U05 needs by reaching directly into the same DataProvider classes
 * their own tabs already use (same pattern as [com.galaxyjoy.cpuinfo.feat.truth.DeviceTruthProvider]
 * / [AiReadinessProvider] — no new raw system reads), then hands it to the pure
 * [CanMyDeviceEvaluator]. AI readiness is reused verbatim via [AiReadinessProvider] rather than
 * re-derived.
 */
class CanMyDeviceProvider @Inject constructor(
    private val dataProviderDrm: DataProviderDrm,
    private val dataProviderCamera: DataProviderCamera,
    private val dataProviderGpu: DataProviderGpu,
    private val aiReadinessProvider: AiReadinessProvider,
) {

    fun evaluate(): CanMyDeviceEvaluator.Result {
        val snapshot = CanMyDeviceEvaluator.Snapshot(
            drmSchemes = dataProviderDrm.getSchemes(),
            cameraLenses = dataProviderCamera.getCameraData().lenses,
            vulkanVersion = dataProviderGpu.getVulkanVersion(),
            aiReadiness = aiReadinessProvider.evaluate(),
        )
        return CanMyDeviceEvaluator.evaluate(snapshot)
    }
}
