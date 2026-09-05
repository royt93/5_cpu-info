package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderBattery
import com.galaxyjoy.cpuinfo.domain.ImmutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.BatteryData
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObservableBatteryData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderBattery: DataProviderBattery,
) : ImmutableInteractor<Unit, BatteryData>() {
    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Unit) = flow {
        while (true) {
            emit(dataProviderBattery.getBatteryData())
            delay(3000L)
        }
    }
}
