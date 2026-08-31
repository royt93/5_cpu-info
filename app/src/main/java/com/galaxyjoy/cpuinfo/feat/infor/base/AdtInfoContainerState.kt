package com.galaxyjoy.cpuinfo.feat.infor.base

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.infor.android.FrmAndroidInfo
import com.galaxyjoy.cpuinfo.feat.infor.audio.FrmAudioInfo
import com.galaxyjoy.cpuinfo.feat.infor.battery.FrmBatteryInfo
import com.galaxyjoy.cpuinfo.feat.infor.camera.FrmCameraInfo
import com.galaxyjoy.cpuinfo.feat.infor.cpu.FrmCpuInfo
import com.galaxyjoy.cpuinfo.feat.infor.dashboard.FrmDashboard
import com.galaxyjoy.cpuinfo.feat.infor.drm.FrmDrmInfo
import com.galaxyjoy.cpuinfo.feat.infor.gpu.FrmGpuInfo
import com.galaxyjoy.cpuinfo.feat.infor.hardware.FrmHardwareInfo
import com.galaxyjoy.cpuinfo.feat.infor.media.FrmMediaInfo
import com.galaxyjoy.cpuinfo.feat.infor.network.FrmNetworkInfo
import com.galaxyjoy.cpuinfo.feat.infor.ram.FrmRamInfo
import com.galaxyjoy.cpuinfo.feat.infor.screen.FrmScreenInfo
import com.galaxyjoy.cpuinfo.feat.infor.sensor.FrmSensorsInfo
import com.galaxyjoy.cpuinfo.feat.infor.storage.FrmStorageInfo
import com.galaxyjoy.cpuinfo.feat.storagebench.FrmStorageBench
import com.galaxyjoy.cpuinfo.feat.throttle.FrmThrottle

/**
 * Simple view pager for info fragments
 */
class AdtInfoContainerState(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment =
        when (position) {
            CPU_POS -> FrmCpuInfo()
            GPU_POS -> FrmGpuInfo()
            RAM_POS -> FrmRamInfo()
            STORAGE_POS -> FrmStorageInfo()
            SCREEN_POS -> FrmScreenInfo()
            ANDROID_POS -> FrmAndroidInfo()
            HARDWARE_POS -> FrmHardwareInfo()
            SENSORS_POS -> FrmSensorsInfo()
            DRM_POS -> FrmDrmInfo()
            MEDIA_POS -> FrmMediaInfo()
            CAMERA_POS -> FrmCameraInfo()
            THROTTLE_POS -> FrmThrottle()
            NETWORK_POS -> FrmNetworkInfo()
            AUDIO_POS -> FrmAudioInfo()
            BATTERY_POS -> FrmBatteryInfo()
            DASHBOARD_POS -> FrmDashboard()
            STORAGE_BENCH_POS -> FrmStorageBench()
            else -> throw IllegalArgumentException("Unknown position for ViewPager2")
        }

    override fun getItemCount(): Int = INFO_PAGE_AMOUNT

    fun getTitleRes(position: Int) = when (position) {
        CPU_POS -> R.string.cpu
        GPU_POS -> R.string.gpu
        RAM_POS -> R.string.ram
        STORAGE_POS -> R.string.storage
        SCREEN_POS -> R.string.screen
        ANDROID_POS -> R.string.android
        HARDWARE_POS -> R.string.hardware
        SENSORS_POS -> R.string.sensors
        DRM_POS -> R.string.drm
        MEDIA_POS -> R.string.media
        CAMERA_POS -> R.string.camera
        THROTTLE_POS -> R.string.throttle
        NETWORK_POS -> R.string.network
        AUDIO_POS -> R.string.audio
        BATTERY_POS -> R.string.battery
        DASHBOARD_POS -> R.string.dashboard
        STORAGE_BENCH_POS -> R.string.storage_bench
        else -> throw IllegalArgumentException("Unknown position for ViewPager2")
    }

    companion object {
        private const val CPU_POS = 0
        private const val GPU_POS = 1
        private const val RAM_POS = 2
        private const val STORAGE_POS = 3
        private const val SCREEN_POS = 4
        private const val ANDROID_POS = 5
        private const val HARDWARE_POS = 6
        private const val SENSORS_POS = 7
        private const val DRM_POS = 8
        private const val MEDIA_POS = 9
        private const val CAMERA_POS = 10
        private const val THROTTLE_POS = 11
        private const val NETWORK_POS = 12
        private const val AUDIO_POS = 13
        private const val BATTERY_POS = 14
        private const val DASHBOARD_POS = 15
        private const val STORAGE_BENCH_POS = 16

        private const val INFO_PAGE_AMOUNT = 17
    }
}
