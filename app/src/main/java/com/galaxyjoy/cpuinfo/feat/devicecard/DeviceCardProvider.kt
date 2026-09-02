package com.galaxyjoy.cpuinfo.feat.devicecard

import android.os.Build
import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.data.provider.DataProviderScreen
import com.galaxyjoy.cpuinfo.data.provider.DataProviderStorage
import com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreProvider
import javax.inject.Inject

/**
 * Reads current CPU/RAM/storage/screen/Shield-Score state and assembles [DeviceCardData] —
 * everything here is already exposed by an existing `DataProvider*`/[ShieldScoreProvider], no new
 * raw system reads (same reuse philosophy as
 * [com.galaxyjoy.cpuinfo.feat.canmydevice.CanMyDeviceProvider]).
 */
class DeviceCardProvider @Inject constructor(
    private val dataProviderCpu: DataProviderCpu,
    private val dataNativeProviderCpu: DataNativeProviderCpu,
    private val dataProviderRam: DataProviderRam,
    private val dataProviderStorage: DataProviderStorage,
    private val dataProviderScreen: DataProviderScreen,
    private val shieldScoreProvider: ShieldScoreProvider,
) {

    fun build(): DeviceCardData {
        val cpuName = dataNativeProviderCpu.getCpuName().takeIf(String::isNotBlank) ?: dataProviderCpu.getAbi()
        val display = dataProviderScreen.getScreenData().displayInfo

        return DeviceCardData(
            deviceModel = Build.MODEL,
            chipName = cpuName,
            coreCount = dataProviderCpu.getNumberOfCores(),
            ramTotalBytes = dataProviderRam.getTotalBytes(),
            storageTotalBytes = dataProviderStorage.getInternalVolume().totalBytes,
            screenResolution = display?.let { "${it.absoluteWidthPx}×${it.absoluteHeightPx}" } ?: "—",
            refreshRateHz = display?.refreshRate?.toInt() ?: 0,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            shieldScore = shieldScoreProvider.compute().overall,
        )
    }
}
