package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

@Keep
data class GpuData(
    val vulkanVersion: String,
    val glesVersion: String,
    val glVendor: String?,
    val glRenderer: String?,
    val glExtensions: String?
)
