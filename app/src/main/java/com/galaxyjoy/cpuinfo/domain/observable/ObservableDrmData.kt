package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderDrm
import com.galaxyjoy.cpuinfo.domain.ImmutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.DrmData
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** DRM scheme support/levels are static for the process lifetime — emits once, same shape as
 * [ObservableHardwareData]/[ObservableMediaData]. */
class ObservableDrmData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderDrm: DataProviderDrm,
) : ImmutableInteractor<Unit, DrmData>() {

    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Unit) = flow {
        emit(DrmData(dataProviderDrm.getSchemes()))
    }
}
