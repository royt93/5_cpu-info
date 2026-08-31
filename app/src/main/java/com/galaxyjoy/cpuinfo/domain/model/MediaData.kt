package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

@Keep
data class MediaCodecData(
    val name: String,
    val isEncoder: Boolean,
    val isHardwareAccelerated: Boolean,
    val supportedTypes: List<String>,
)

@Keep
data class MediaData(val codecs: List<MediaCodecData>)
