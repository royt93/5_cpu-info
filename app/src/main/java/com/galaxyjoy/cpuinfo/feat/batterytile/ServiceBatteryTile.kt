package com.galaxyjoy.cpuinfo.feat.batterytile

import android.graphics.drawable.Icon
import android.os.BatteryManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.qstile.openAppAndCollapse
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
class ServiceBatteryTile : TileService(), CoroutineScope {

    @Inject
    lateinit var batteryManager: BatteryManager

    @Inject
    lateinit var dispatchersProvider: DispatchersProvider

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchersProvider.main

    private var refreshingJob: Job? = null

    private val icons by lazy {
        mapOf(
            BatteryLevel.Low to Icon.createWithResource(this, R.drawable.ic_battery_low),
            BatteryLevel.Medium to Icon.createWithResource(this, R.drawable.ic_battery_med),
            BatteryLevel.High to Icon.createWithResource(this, R.drawable.ic_battery_high),
        )
    }
    private val defaultIcon by lazy { Icon.createWithResource(this, R.drawable.ic_battery_high) }

    override fun onStartListening() {
        super.onStartListening()
        refreshingJob?.cancel()
        refreshingJob = launch {
            while (true) {
                updateTileInfo()
                delay(REFRESHING_DELAY_MS)
            }
        }
    }

    override fun onClick() {
        super.onClick()
        openAppAndCollapse()
    }

    override fun onStopListening() {
        refreshingJob?.cancel()
        super.onStopListening()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    private suspend fun updateTileInfo() {
        val (level, charging) = withContext(dispatchersProvider.io) {
            val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            (capacity.takeIf { it in 0..100 } ?: 0) to batteryManager.isCharging
        }

        qsTile?.apply {
            label = if (charging) "⚡$level%" else "$level%"
            icon = getLoadIcon(level)
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    private fun getLoadIcon(level: Int): Icon {
        val loadEnum = when {
            level <= 20 -> BatteryLevel.Low
            level <= 50 -> BatteryLevel.Medium
            else -> BatteryLevel.High
        }
        return icons.getOrDefault(loadEnum, defaultIcon)
    }

    enum class BatteryLevel {
        Low,
        Medium,
        High,
    }

    companion object {
        private const val REFRESHING_DELAY_MS = 10_000L
    }
}
