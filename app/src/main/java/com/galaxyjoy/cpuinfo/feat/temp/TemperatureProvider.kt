package com.galaxyjoy.cpuinfo.feat.temp

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Battery temperature reader shared by [com.galaxyjoy.cpuinfo.feat.throttle.ThrottleTestRunner]
 * and [com.galaxyjoy.cpuinfo.feat.infor.battery.VMBatteryInfo]. CPU temperature discovery/reading
 * moved to [com.galaxyjoy.cpuinfo.data.provider.DataProviderTemperature] (Sprint 20) along with
 * this tab's own VM — kept here as-is rather than also migrating those 2 unrelated call sites in
 * the same change; the two now duplicate the same sticky-intent read, a small enough overlap to
 * leave for a future cleanup pass rather than widen this migration's blast radius.
 */
@Singleton
class TemperatureProvider @Inject constructor(@ApplicationContext val appContext: Context) {

    /**
     * @return battery temperature divided by 10 to get value in Celsius
     */
    fun getBatteryTemperature(): Int {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = appContext.registerReceiver(null, iFilter)
        return (batteryStatus?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE, 0
        ) ?: 0) / 10
    }
}
