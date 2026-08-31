package com.galaxyjoy.cpuinfo.feat.infor.drm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.domain.observable.ObservableDrmData
import com.galaxyjoy.cpuinfo.domain.observe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * ViewModel for [FrmDrmInfo]
 */
@HiltViewModel
class VMDrmInfo @Inject constructor(
    observableDrmData: ObservableDrmData,
) : ViewModel() {

    val viewState = observableDrmData.observe()
        .map { DrmInfoViewState(it) }
        .asLiveData(viewModelScope.coroutineContext)
}
