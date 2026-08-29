package com.galaxyjoy.cpuinfo.feat.infor.gpu

import android.content.pm.PackageManager
import android.os.Build
import javax.inject.Inject

/**
 * Vulkan capability flags beyond the hardware-version string [DataProviderGpu] already exposes —
 * hardware level (feature-set tier, distinct from the version number) and compute-shader support.
 * Read from [PackageManager.systemAvailableFeatures] the same way as the existing Vulkan
 * hardware-version lookup; no native Vulkan API calls needed for these coarse capability bits.
 */
class GraphicsDetailProvider @Inject constructor(
    private val packageManager: PackageManager,
) {

    data class VulkanCapability(
        val hardwareLevel: Int,
        val computeSupported: Boolean,
    )

    fun vulkanCapability(): VulkanCapability {
        val hardwareLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            packageManager.systemAvailableFeatures.find {
                it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL
            }?.version ?: -1
        } else {
            -1
        }

        val computeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_COMPUTE)

        return VulkanCapability(hardwareLevel = hardwareLevel, computeSupported = computeSupported)
    }
}
