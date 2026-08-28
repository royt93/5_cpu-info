package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderGpu
import com.galaxyjoy.cpuinfo.domain.MutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.GpuData
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObservableGpuData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderGpu: DataProviderGpu
) : MutableInteractor<ObservableGpuData.Params, GpuData>() {

    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Params) = flow {
        emit(
            GpuData(
                vulkanVersion = dataProviderGpu.getVulkanVersion(),
                glesVersion = dataProviderGpu.getGlEsVersion(),
                glVendor = params.glVendor,
                glRenderer = params.glRenderer,
                glExtensions = params.glExtensions
            )
        )
    }

    data class Params(
        val glVendor: String? = null,
        val glRenderer: String? = null,
        val glExtensions: String? = null
    )
}
