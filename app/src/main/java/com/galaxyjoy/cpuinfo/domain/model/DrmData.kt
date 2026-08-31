package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

@Keep
data class DrmSchemeData(
    val name: String,
    val supported: Boolean,
    val securityLevel: String?,
    val hdcpLevel: String?,
    val maxHdcpLevel: String?,
    val version: String?,
)

@Keep
data class DrmData(val schemes: List<DrmSchemeData>)
