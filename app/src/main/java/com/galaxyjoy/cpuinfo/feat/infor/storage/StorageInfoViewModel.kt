package com.galaxyjoy.cpuinfo.feat.infor.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.domain.observable.ObservableStorageData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * ViewModel for [FrmStorageInfo]
 */
@HiltViewModel
class StorageInfoViewModel @Inject constructor(
    private val observableStorageData: ObservableStorageData,
) : ViewModel() {

    val viewState = observableStorageData.observe()
        .map { StorageInfoViewState(it) }
        .asLiveData(viewModelScope.coroutineContext)

    init {
        observableStorageData(Unit)
    }

    /**
     * Re-read internal/external/SD volumes — called on SD card mount/unmount events (see
     * [FrmStorageInfo]'s debounced `BroadcastReceiver`).
     */
    fun refreshSdCard() {
        observableStorageData(Unit)
    }
}
