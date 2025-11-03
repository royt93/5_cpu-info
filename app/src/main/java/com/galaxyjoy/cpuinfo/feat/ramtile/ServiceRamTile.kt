package com.galaxyjoy.cpuinfo.feat.ramtile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.domain.action.RamCleanupAction
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
class ServiceRamTile : TileService(), CoroutineScope {

    @Inject
    lateinit var dataProviderRam: DataProviderRam

    @Inject
    lateinit var ramCleanupAction: RamCleanupAction

    @Inject
    lateinit var dispatchersProvider: DispatchersProvider

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchersProvider.main

    private var refreshingJob: Job? = null

    private val icons by lazy {
        mapOf(
            RAMLoad.Low to Icon.createWithResource(this, R.drawable.ic_ram_low),
            RAMLoad.Medium to Icon.createWithResource(this, R.drawable.ic_ram_med),
            RAMLoad.High to Icon.createWithResource(this, R.drawable.ic_ram_high)
        )
    }
    private val defaultIcon by lazy { Icon.createWithResource(this, R.drawable.ic_ram_high) }

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
        launch {
            // Run RAM cleanup
            withContext(dispatchersProvider.io) {
                ramCleanupAction(Unit)
            }
            // Update tile immediately after cleanup
            delay(500)
            updateTileInfo()
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

    private fun updateTileInfo() {
        val availablePercentage = dataProviderRam.getAvailablePercentage()
        val usedPercentage = 100 - availablePercentage
        val availGB = dataProviderRam.getAvailableBytes() / (1024 * 1024 * 1024.0)

        qsTile?.apply {
            label = "${"%.1f".format(availGB)}GB Free"
            contentDescription = "RAM: $usedPercentage% used"
            icon = getLoadIcon(usedPercentage)
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    private fun getLoadIcon(usedPercentage: Int): Icon {
        val loadEnum = when {
            usedPercentage >= 75 -> RAMLoad.High
            usedPercentage >= 50 -> RAMLoad.Medium
            else -> RAMLoad.Low
        }
        return icons.getOrDefault(loadEnum, defaultIcon)
    }

    enum class RAMLoad {
        Low,
        Medium,
        High
    }

    companion object {
        private const val REFRESHING_DELAY_MS = 2000L
    }
}
