package com.galaxyjoy.cpuinfo.appinitializers

import android.app.Application
import android.os.SystemClock
import com.galaxyjoy.cpuinfo.feat.shield.RebootStabilityDetector
import com.galaxyjoy.cpuinfo.feat.shield.RebootStabilityPrefs
import javax.inject.Inject

/** E21 — records a reboot each time the wall-clock-minus-uptime boot time moves versus the last
 * app launch. Runs once per process start, same place [NativeToolsInitializer] does its one-shot
 * setup work. */
class RebootStabilityInitializer @Inject constructor(
    private val prefs: RebootStabilityPrefs,
) : AppInitializer {

    override fun init(application: Application) {
        val currentBootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        if (RebootStabilityDetector.isNewBoot(currentBootTime, prefs.getLastKnownBootTimeMillis())) {
            prefs.incrementRebootCount()
        }
        prefs.recordBootTime(currentBootTime)
    }
}
