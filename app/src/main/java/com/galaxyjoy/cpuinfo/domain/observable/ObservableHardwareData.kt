package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderHardware
import com.galaxyjoy.cpuinfo.domain.ImmutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.HardwareData
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Wireless/USB capability flags never change at runtime, so this emits once rather than polling
 * like [ObservableCpuData]/[ObservableRamData] do for their genuinely live values.
 */
class ObservableHardwareData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderHardware: DataProviderHardware,
) : ImmutableInteractor<Unit, HardwareData>() {

    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Unit) = flow {
        emit(dataProviderHardware.getHardwareData())
    }
}
