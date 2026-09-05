package com.galaxyjoy.cpuinfo.domain.model

import android.content.Intent

data class BatteryData(
    val status: Intent?,
    val voltageMv: Int?,
    val chargingCurrentMa: Double?,
    val currentMa: Double?,
    val temperature: Float?,
    val designedCapacity: Double,
    val chargeCounter: Long?,
    val energyCounter: Long?,
    val cycleCount: Int?,
)
