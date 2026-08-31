package com.galaxyjoy.cpuinfo.feat.infor.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.domain.observable.ObservableCameraData
import com.galaxyjoy.cpuinfo.domain.observe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * ViewModel for [FrmCameraInfo]
 */
@HiltViewModel
class VMCameraInfo @Inject constructor(
    observableCameraData: ObservableCameraData,
) : ViewModel() {

    val viewState = observableCameraData.observe()
        .map { CameraInfoViewState(it) }
        .asLiveData(viewModelScope.coroutineContext)
}
