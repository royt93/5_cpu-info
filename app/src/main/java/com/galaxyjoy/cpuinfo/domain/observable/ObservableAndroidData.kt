package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderAndroid
import com.galaxyjoy.cpuinfo.domain.ImmutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.AndroidData
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Android OS / security posture info never changes at runtime, so this emits once rather than
 * polling like [ObservableCpuData]/[ObservableRamData] do for genuinely live values.
 */
class ObservableAndroidData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderAndroid: DataProviderAndroid,
) : ImmutableInteractor<Unit, AndroidData>() {

    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Unit) = flow {
        emit(dataProviderAndroid.getAndroidData())
    }
}
