package com.galaxyjoy.cpuinfo.domain.action

import android.app.ActivityManager
import android.content.Context
import com.galaxyjoy.cpuinfo.domain.ResultInteractor
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class RamCleanupAction @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityManager: ActivityManager,
    dispatchersProvider: DispatchersProvider
) : ResultInteractor<Unit, Unit>() {

    override val dispatcher = dispatchersProvider.io

    override suspend fun doWork(params: Unit) {
        try {
            // Run garbage collection
            System.runFinalization()
            Runtime.getRuntime().gc()
            System.gc()

            // Kill background processes to free RAM
            val runningApps = activityManager.runningAppProcesses ?: emptyList()
            val myPackageName = context.packageName

            var killedCount = 0
            runningApps.forEach { processInfo ->
                // Don't kill our own process
                if (processInfo.processName != myPackageName) {
                    try {
                        activityManager.killBackgroundProcesses(processInfo.processName)
                        killedCount++
                    } catch (e: Exception) {
                        // Some processes cannot be killed due to permissions
                        Timber.d("Cannot kill process: ${processInfo.processName}")
                    }
                }
            }

            Timber.i("RAM Cleanup: GC run, attempted to kill $killedCount background processes")
        } catch (e: Exception) {
            Timber.e(e, "Error during RAM cleanup")
        }
    }
}
