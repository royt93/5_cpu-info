package com.galaxyjoy.cpuinfo.appinitializers

import android.app.Application
import com.getkeepsafe.relinker.ReLinker
import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import timber.log.Timber
import javax.inject.Inject

class NativeToolsInitializer @Inject constructor(
    private val dataNativeProviderCpu: DataNativeProviderCpu
) : AppInitializer {

    override fun init(application: Application) {
        try {
            ReLinker.loadLibrary(application, "cpuinfo-libs")
            dataNativeProviderCpu.initLibrary()
        } catch (e: Throwable) {
            Timber.e(e, "Failed to load native cpuinfo-libs, native CPU data will be unavailable")
        }
    }
}
