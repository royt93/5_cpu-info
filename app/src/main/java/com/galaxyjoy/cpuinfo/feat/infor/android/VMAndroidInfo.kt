package com.galaxyjoy.cpuinfo.feat.infor.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.domain.observable.ObservableAndroidData
import com.galaxyjoy.cpuinfo.domain.observe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * ViewModel for [FrmAndroidInfo]
 */
@HiltViewModel
class VMAndroidInfo @Inject constructor(
    observableAndroidData: ObservableAndroidData,
) : ViewModel() {

    val viewState = observableAndroidData.observe()
        .distinctUntilChanged()
        .map { AndroidInfoViewState(it) }
        .asLiveData(viewModelScope.coroutineContext)
}
