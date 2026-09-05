package com.galaxyjoy.cpuinfo.data.provider

import kotlin.math.abs

/** Pure validation shared by the live row and existing session diagnostics. */
object BatteryReading {

    /** `BATTERY_PROPERTY_CURRENT_NOW`/`CURRENT_AVERAGE` are documented in µA, but several OEMs
     * (confirmed on a Samsung Galaxy S24 Ultra — `adb shell dumpsys battery` reports
     * `current now: 1039` raw, matching what this property returns, for a device visibly pulling
     * ~1A over USB) report already-scaled mA instead. A real µA reading for an active phone is
     * never under [ALREADY_MA_THRESHOLD] in magnitude — trickle-charge current near full still
     * runs well above this on real hardware before charging just terminates outright — so a raw
     * value under that threshold can only be the buggy already-mA case. */
    fun currentMa(rawMicroAmps: Int): Double? =
        rawMicroAmps.takeUnless { it == Int.MIN_VALUE || it == Int.MAX_VALUE }
            ?.let { if (abs(it) < ALREADY_MA_THRESHOLD) it.toDouble() else it / 1000.0 }

    fun chargingCurrentMa(rawMicroAmps: Int, activelyCharging: Boolean): Double? {
        // Show magnitude only: OEMs disagree about charging polarity. Status, never sign,
        // determines charging. FULL/NOT_CHARGING/on-battery are hidden to avoid labelling
        // discharge current as charging current. Zero is valid while actively charging.
        return if (activelyCharging) currentMa(rawMicroAmps)?.let { abs(it) } else null
    }

    // Keep voltage visible on battery power too: battery terminal voltage is still useful.
    fun voltageMv(raw: Int): Int? = raw.takeIf { it > 0 && it != Int.MAX_VALUE }

    private const val ALREADY_MA_THRESHOLD = 20_000
}
