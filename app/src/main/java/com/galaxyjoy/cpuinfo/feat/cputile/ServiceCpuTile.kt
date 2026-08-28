package com.galaxyjoy.cpuinfo.feat.cputile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@RequiresApi(api = Build.VERSION_CODES.N)
@AndroidEntryPoint
class ServiceCpuTile : TileService(), CoroutineScope {

    @Inject
    lateinit var dataProviderCpu: DataProviderCpu

    @Inject
    lateinit var dispatchersProvider: DispatchersProvider

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchersProvider.main

    private var refreshingJob: Job? = null

    private val minMaxAvg: Pair<Long, Long> by lazy {
        val cpuCount = dataProviderCpu.getNumberOfCores()
        if (cpuCount == 0) return@lazy Pair(0L, 0L)
        val minFreq = mutableListOf<Long>()
        val maxFreq = mutableListOf<Long>()
        for (i in 0 until cpuCount) {
            val minMax = dataProviderCpu.getMinMaxFreq(i)
            minFreq.add(minMax.first)
            maxFreq.add(minMax.second)
        }
        Pair(minFreq.sum() / cpuCount, maxFreq.sum() / cpuCount)
    }

    private val icons by lazy {
        mapOf(
            CPULoad.Low to Icon.createWithResource(this, R.drawable.ic_cpu_low),
            CPULoad.Medium to Icon.createWithResource(this, R.drawable.ic_cpu_med),
            CPULoad.High to Icon.createWithResource(this, R.drawable.ic_cpu_high)
        )
    }
    private val defaultIcon by lazy { Icon.createWithResource(this, R.drawable.ic_cpu_high) }

    override fun onStartListening() {
        super.onStartListening()
        refreshingJob?.cancel()
        refreshingJob = launch {
            while (true) {
                val load = withContext(dispatchersProvider.io) { averageCPUFreq() }
                qsTile?.apply {
                    label = "Avg ${load}MHz"
                    icon = getLoadIcon(load)
                    updateTile()
                }
                delay(REFRESHING_DELAY_MS)
            }
        }
    }

    override fun onStopListening() {
        refreshingJob?.cancel()
        super.onStopListening()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    private fun getLoadIcon(avgLoad: Long): Icon {
        val freqDiff = minMaxAvg.second - minMaxAvg.first
        val freqThirds = freqDiff / 3
        val loadEnum = when {
            avgLoad >= minMaxAvg.second - freqThirds -> CPULoad.High
            minMaxAvg.second - freqThirds > avgLoad && avgLoad >= minMaxAvg.first + freqThirds -> CPULoad.Medium
            else -> CPULoad.Low
        }
        return icons.getOrDefault(loadEnum, defaultIcon)
    }

    private fun averageCPUFreq(): Long {
        val cpuCount = dataProviderCpu.getNumberOfCores()
        if (cpuCount == 0) return 0L
        var sumFreq = 0L
        for (i in 0 until cpuCount) {
            sumFreq += dataProviderCpu.getCurrentFreq(i).coerceAtLeast(0L)
        }
        return (sumFreq / cpuCount)
    }

    enum class CPULoad {
        Low,
        Medium,
        High
    }

    companion object {
        private const val REFRESHING_DELAY_MS = 1000L
    }
}
