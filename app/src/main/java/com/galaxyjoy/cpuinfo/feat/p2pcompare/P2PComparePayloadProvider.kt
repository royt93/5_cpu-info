package com.galaxyjoy.cpuinfo.feat.p2pcompare

import android.os.Build
import android.os.Environment
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import javax.inject.Inject

/** Same real-values source as [com.galaxyjoy.cpuinfo.feat.fleet.FleetCompareProvider]. */
class P2PComparePayloadProvider @Inject constructor(
    private val dataProviderRam: DataProviderRam,
) {

    fun buildLocalPayload(): DeviceComparePayload = DeviceComparePayload.create(
        deviceModel = Build.MODEL ?: "",
        ramBytes = dataProviderRam.getTotalBytes(),
        storageBytes = Environment.getDataDirectory().totalSpace,
    )
}
