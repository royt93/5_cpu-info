package com.galaxyjoy.cpuinfo.data.provider

import android.app.ActivityManager
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.galaxyjoy.cpuinfo.R
import javax.inject.Inject

class DataProviderGpu @Inject constructor(
    private val activityManager: ActivityManager,
    private val packageManager: PackageManager,
    private val resources: Resources,
) {

    fun getGlEsVersion(): String {
        return activityManager.deviceConfigurationInfo.glEsVersion
    }

    /**
     * Obtain Vulkan version
     */
    fun getVulkanVersion(): String {
        val default = resources.getString(R.string.unknown)
        if (Build.VERSION.SDK_INT < 24) {
            return default
        }

        val vulkan = packageManager.systemAvailableFeatures.find {
            it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION
        }?.version ?: 0
        if (vulkan == 0) {
            return default
        }

        return decodeVulkanVersion(vulkan)
    }

    companion object {
        /**
         * Extract major.minor.patch from the Vulkan hardware version bit field.
         * See: https://developer.android.com/reference/android/content/pm/PackageManager#FEATURE_VULKAN_HARDWARE_VERSION
         * Uses unsigned shifts — signed `shr` sign-extends and corrupts minor/patch whenever
         * their high bit is set.
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
        internal fun decodeVulkanVersion(vulkan: Int): String {
            val major = (vulkan ushr 22) and 0x3FF   // Higher 10 bits
            val minor = (vulkan ushr 12) and 0x3FF   // Middle 10 bits
            val patch = vulkan and 0xFFF             // Lower 12 bits
            return "$major.$minor.$patch"
        }
    }
}
