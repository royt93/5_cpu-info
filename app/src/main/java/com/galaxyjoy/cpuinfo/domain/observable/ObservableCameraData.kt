package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderCamera
import com.galaxyjoy.cpuinfo.domain.ImmutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.CameraData
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Camera2 characteristics are static for the process lifetime — emits once, same shape as
 * [ObservableHardwareData]/[ObservableMediaData]/[ObservableDrmData]. */
class ObservableCameraData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderCamera: DataProviderCamera,
) : ImmutableInteractor<Unit, CameraData>() {

    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Unit) = flow {
        emit(dataProviderCamera.getCameraData())
    }
}
