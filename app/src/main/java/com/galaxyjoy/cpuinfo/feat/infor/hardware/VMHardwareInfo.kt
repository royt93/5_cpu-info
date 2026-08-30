package com.galaxyjoy.cpuinfo.feat.infor.hardware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.domain.observable.ObservableHardwareData
import com.galaxyjoy.cpuinfo.domain.observe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * ViewModel for [FrmHardwareInfo]
 */
@HiltViewModel
class VMHardwareInfo @Inject constructor(
    observableHardwareData: ObservableHardwareData,
) : ViewModel() {

    val viewState = observableHardwareData.observe()
        .distinctUntilChanged()
        .map { HardwareInfoViewState(it) }
        .asLiveData(viewModelScope.coroutineContext)
}
