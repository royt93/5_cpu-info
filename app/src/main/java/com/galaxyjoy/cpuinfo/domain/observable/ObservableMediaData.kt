package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderMedia
import com.galaxyjoy.cpuinfo.domain.ImmutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.MediaData
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** The codec list is static for the process lifetime, so this emits once — same shape as
 * [ObservableHardwareData]. */
class ObservableMediaData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderMedia: DataProviderMedia,
) : ImmutableInteractor<Unit, MediaData>() {

    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Unit) = flow {
        emit(MediaData(dataProviderMedia.getMediaCodecs()))
    }
}
