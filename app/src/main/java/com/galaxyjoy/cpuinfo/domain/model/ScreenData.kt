package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

enum class ScreenSizeCategory { LARGE, NORMAL, SMALL, UNKNOWN }

enum class ScreenDensityCategory { LDPI, MDPI, HDPI, XHDPI, XXHDPI, XXXHDPI, UNKNOWN }

@Keep
data class DisplayModeData(
    val physicalWidth: Int,
    val physicalHeight: Int,
    val refreshRate: Float,
)

/**
 * [realMetrics] is null when [android.view.Display.getRealMetrics] throws (defensive — mirrors
 * the try/catch in the pre-migration VM code); the rest come from a separate, unguarded
 * [android.view.Display.getMetrics]/[android.view.Display.getRealMetrics] call so they're always
 * present whenever [ScreenDisplayInfo] itself is non-null.
 */
@Keep
data class ScreenRealMetrics(
    val widthPx: Int,
    val heightPx: Int,
    val dpWidth: Int,
    val dpHeight: Int,
    val density: Float,
)

/** Null (the whole group) only when `DisplayManager.getDisplay(DEFAULT_DISPLAY)` itself returns null. */
@Keep
data class ScreenDisplayInfo(
    val realMetrics: ScreenRealMetrics?,
    val absoluteWidthPx: Int,
    val absoluteHeightPx: Int,
    val refreshRate: Float,
    val rotation: Int,
    val xdpi: Float,
    val ydpi: Float,
    val supportedModes: List<DisplayModeData>,
    /** Raw `Display.HdrCapabilities.HDR_TYPE_*` ints; null when API < N (always true at minSdk=24, kept for parity with the pre-migration SDK gate). */
    val hdrTypes: List<Int>?,
    /** Null when API < O (feature doesn't exist below Oreo). */
    val isWideColorGamut: Boolean?,
    /** Null when API < P (feature doesn't exist below Pie); -1 when API >= P but device reports no cutout. */
    val cutoutRectCount: Int?,
)

@Keep
data class ScreenData(
    val sizeCategory: ScreenSizeCategory,
    val densityCategory: ScreenDensityCategory,
    val displayInfo: ScreenDisplayInfo?,
)
