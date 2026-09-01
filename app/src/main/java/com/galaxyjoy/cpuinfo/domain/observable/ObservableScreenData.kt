package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderScreen
import com.galaxyjoy.cpuinfo.domain.ImmutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.ScreenData
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Screen classification + [android.view.Display] metrics never change at runtime, so this emits
 * once rather than polling like [ObservableCpuData]/[ObservableRamData] do for genuinely live values.
 */
class ObservableScreenData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderScreen: DataProviderScreen,
) : ImmutableInteractor<Unit, ScreenData>() {

    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Unit) = flow {
        emit(dataProviderScreen.getScreenData())
    }
}
