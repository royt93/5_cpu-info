package com.galaxyjoy.cpuinfo.feat.fleet

import android.os.Build
import android.os.Environment
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import javax.inject.Inject

class FleetCompareProvider @Inject constructor(
    private val dataProviderRam: DataProviderRam,
) {

    fun evaluate(): FleetCompareEvaluator.Result = FleetCompareEvaluator.evaluate(
        buildModel = Build.MODEL ?: "",
        actualRamBytes = dataProviderRam.getTotalBytes(),
        actualStorageBytes = Environment.getDataDirectory().totalSpace,
    )
}
