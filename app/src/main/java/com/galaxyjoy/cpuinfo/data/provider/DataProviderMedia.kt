package com.galaxyjoy.cpuinfo.data.provider

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import com.galaxyjoy.cpuinfo.domain.model.MediaCodecData
import javax.inject.Inject

/**
 * Decoders listed before encoders, hardware-accelerated codecs prioritized first within each
 * group.
 */
class DataProviderMedia @Inject constructor() {

    fun getMediaCodecs(): List<MediaCodecData> =
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .sortedWith(
                compareBy(
                    { it.isEncoder },
                    { !it.isHardwareAcceleratedSafe() },
                    { it.name },
                ),
            )
            .map {
                MediaCodecData(
                    name = it.name,
                    isEncoder = it.isEncoder,
                    isHardwareAccelerated = it.isHardwareAcceleratedSafe(),
                    supportedTypes = it.supportedTypes.toList(),
                )
            }

    private fun MediaCodecInfo.isHardwareAcceleratedSafe(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) isHardwareAccelerated else !name.startsWith("OMX.google.")
}
