package com.galaxyjoy.cpuinfo.appinitializers

import android.app.Application
import android.util.Log
import com.getkeepsafe.relinker.ReLinker
import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class NativeToolsInitializer @Inject constructor(
    private val dataNativeProviderCpu: DataNativeProviderCpu
) : AppInitializer {

    // Uses android.util.Log, not Timber: this initializer is bound ahead of InitializerTimber in
    // AppModuleBinds (@IntoSet order), so Timber's tree isn't planted yet when this runs — a
    // Timber call here would silently vanish (confirmed via real-device logcat: no output at all).
    override fun init(application: Application) {
        try {
            val elapsedMs = measureTimeMillis {
                ReLinker.loadLibrary(application, "cpuinfo-libs")
                dataNativeProviderCpu.initLibrary()
            }
            Log.i(TAG, "cpuinfo-libs loaded + initialized in ${elapsedMs}ms")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load native cpuinfo-libs, native CPU data will be unavailable", e)
        }
    }

    companion object {
        private const val TAG = "NativeToolsInitializer"
    }
}
