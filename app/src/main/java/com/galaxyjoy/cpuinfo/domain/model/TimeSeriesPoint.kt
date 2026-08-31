package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

@Keep
data class TimeSeriesPoint(val timestampMs: Long, val value: Float)
