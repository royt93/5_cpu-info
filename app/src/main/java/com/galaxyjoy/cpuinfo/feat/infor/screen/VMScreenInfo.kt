package com.galaxyjoy.cpuinfo.feat.infor.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.domain.observable.ObservableScreenData
import com.galaxyjoy.cpuinfo.domain.observe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * ViewModel for [FrmScreenInfo]
 */
@HiltViewModel
class VMScreenInfo @Inject constructor(
    observableScreenData: ObservableScreenData,
) : ViewModel() {

    val viewState = observableScreenData.observe()
        .distinctUntilChanged()
        .map { ScreenInfoViewState(it) }
        .asLiveData(viewModelScope.coroutineContext)
}
