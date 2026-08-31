package com.galaxyjoy.cpuinfo.feat.infor.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.domain.observable.ObservableMediaData
import com.galaxyjoy.cpuinfo.domain.observe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * ViewModel for [FrmMediaInfo]
 */
@HiltViewModel
class VMMediaInfo @Inject constructor(
    observableMediaData: ObservableMediaData,
) : ViewModel() {

    val viewState = observableMediaData.observe()
        .map { MediaInfoViewState(it) }
        .asLiveData(viewModelScope.coroutineContext)
}
