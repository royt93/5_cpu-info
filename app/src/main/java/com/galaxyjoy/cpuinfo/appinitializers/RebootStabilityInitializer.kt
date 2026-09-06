package com.galaxyjoy.cpuinfo.appinitializers

import android.app.Application
import android.content.ContentResolver
import android.provider.Settings
import android.util.Log
import com.galaxyjoy.cpuinfo.feat.shield.RebootStabilityDetector
import com.galaxyjoy.cpuinfo.feat.shield.RebootStabilityPrefs
import javax.inject.Inject

/** E21 — records a reboot each time `Settings.Global.BOOT_COUNT` differs from the last app
 * launch. Runs once per process start, same place [NativeToolsInitializer] does its one-shot
 * setup work. Wrapped in try/catch, unlike some sibling initializers — a failure here must not
 * take down the rest of [InitializersApp]'s set or crash app startup. */
class RebootStabilityInitializer @Inject constructor(
    private val contentResolver: ContentResolver,
    private val prefs: RebootStabilityPrefs,
) : AppInitializer {

    override fun init(application: Application) {
        try {
            val currentBootCount = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
            if (RebootStabilityDetector.isNewBoot(currentBootCount, prefs.getLastKnownBootCount())) {
                prefs.incrementRebootCount()
            }
            prefs.recordBootCount(currentBootCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record boot count", e)
        }
    }

    companion object {
        private const val TAG = "RebootStabilityInit"
    }
}
