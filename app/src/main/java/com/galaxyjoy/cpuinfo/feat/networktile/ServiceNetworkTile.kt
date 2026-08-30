package com.galaxyjoy.cpuinfo.feat.networktile

import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

/**
 * Shows connection type only (Wi-Fi/Mobile/Ethernet/Offline) — no signal strength number.
 * Signal strength needs location permission for Wi-Fi RSSI pre-API 29 (see the consent flow
 * `feat/infor/network` already needs for that), which doesn't fit a permission-less glanceable
 * tile.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
@AndroidEntryPoint
class ServiceNetworkTile : TileService(), CoroutineScope {

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    @Inject
    lateinit var dispatchersProvider: DispatchersProvider

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchersProvider.main

    private var refreshingJob: Job? = null

    private val onIcon by lazy { Icon.createWithResource(this, R.drawable.ic_network_on) }
    private val offIcon by lazy { Icon.createWithResource(this, R.drawable.ic_network_off) }

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
        val (label, connected) = withContext(dispatchersProvider.io) { connectionState() }

        qsTile?.apply {
            this.label = label
            icon = if (connected) onIcon else offIcon
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    private fun connectionState(): Pair<String, Boolean> {
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
            ?: return "Offline" to false

        val label = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Offline"
        }
        return label to (label != "Offline")
    }

    companion object {
        private const val REFRESHING_DELAY_MS = 5_000L
    }
}
