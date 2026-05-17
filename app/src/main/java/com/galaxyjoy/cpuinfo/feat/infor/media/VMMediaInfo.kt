package com.galaxyjoy.cpuinfo.feat.infor.media

import android.content.res.Resources
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.R
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
class VMMediaInfo @Inject constructor(
    private val resources: Resources,
) : ViewModel() {

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

        listLiveData.add(resources.getString(R.string.media_total_codecs) to codecs.size.toString())
        listLiveData.add(resources.getString(R.string.media_decoders) to codecs.count { !it.isEncoder }.toString())
        listLiveData.add(resources.getString(R.string.media_encoders) to codecs.count { it.isEncoder }.toString())

        val encoderLabel = resources.getString(R.string.media_role_encoder)
        val decoderLabel = resources.getString(R.string.media_role_decoder)
        val hwLabel = resources.getString(R.string.media_accel_hw)
        val swLabel = resources.getString(R.string.media_accel_sw)

        codecs.forEach { codec ->
            val role = if (codec.isEncoder) encoderLabel else decoderLabel
            val accel = if (codec.isHardwareAcceleratedSafe()) hwLabel else swLabel
            val types = codec.supportedTypes.joinToString(", ")
            val summary = resources.getString(R.string.media_codec_summary, role, accel, types)
            listLiveData.add(codec.name to summary)
        }
    }

    private fun MediaCodecInfo.isHardwareAcceleratedSafe(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) isHardwareAccelerated else !name.startsWith("OMX.google.")
}
