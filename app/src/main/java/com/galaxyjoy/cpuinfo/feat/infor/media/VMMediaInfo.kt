package com.galaxyjoy.cpuinfo.feat.infor.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Surface media codec metadata from [MediaCodecList].
 *
 * Each codec contributes a small block: header (name) + capabilities (HW/SW, types).
 * Decoders listed before encoders. Hardware-accelerated codecs prioritised first
 * within each group.
 */
@HiltViewModel
class VMMediaInfo @Inject constructor() : ViewModel() {

    val listLiveData = ListLiveData<Pair<String, String>>()

    init {
        if (listLiveData.isEmpty()) {
            populate()
        }
    }

    private fun populate() {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .sortedWith(
                compareBy(
                    { it.isEncoder },
                    { !it.isHardwareAcceleratedSafe() },
                    { it.name },
                ),
            )

        listLiveData.add("Total codecs" to codecs.size.toString())
        listLiveData.add("Decoders" to codecs.count { !it.isEncoder }.toString())
        listLiveData.add("Encoders" to codecs.count { it.isEncoder }.toString())

        codecs.forEach { codec ->
            val role = if (codec.isEncoder) "encoder" else "decoder"
            val accel = if (codec.isHardwareAcceleratedSafe()) "HW" else "SW"
            val types = codec.supportedTypes.joinToString(", ")
            listLiveData.add(codec.name to "$role · $accel · $types")
        }
    }

    private fun MediaCodecInfo.isHardwareAcceleratedSafe(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) isHardwareAccelerated else !name.startsWith("OMX.google.")
}
