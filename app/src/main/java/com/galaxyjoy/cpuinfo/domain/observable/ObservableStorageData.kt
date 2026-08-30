package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderStorage
import com.galaxyjoy.cpuinfo.domain.MutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.StorageData
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Unlike CPU/RAM/GPU/Hardware/Temp (poll or read-once), storage only changes on SD-card
 * mount/unmount events, so this is trigger-driven rather than a polling loop: call `invoke(Unit)`
 * whenever a mount event fires (or on first load). The Fragment still owns the actual
 * `BroadcastReceiver` registration and its debounce — both are Android/lifecycle concerns — it
 * just forwards each event here via [com.galaxyjoy.cpuinfo.feat.infor.storage.StorageInfoViewModel.refreshSdCard].
 *
 * Re-reads internal/external volumes too on every trigger, not just the SD card slot — free
 * space genuinely drifts over time, and re-fetching everything as one snapshot is simpler than
 * partially patching a stale one.
 */
class ObservableStorageData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderStorage: DataProviderStorage,
) : MutableInteractor<Unit, StorageData>() {

    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Unit) = flow {
        emit(
            StorageData(
                internal = dataProviderStorage.getInternalVolume(),
                external = dataProviderStorage.getExternalVolume(),
                sdCard = dataProviderStorage.findSdCardVolume(),
            ),
        )
    }
}
