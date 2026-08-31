package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

@Keep
enum class CameraFacing { FRONT, BACK, EXTERNAL, UNKNOWN }

@Keep
data class CameraLensData(
    val id: String,
    val facing: CameraFacing,
    val focalLengthsMm: List<Float>? = null,
    val sensorWidthMm: Float? = null,
    val sensorHeightMm: Float? = null,
    val pixelArrayWidth: Int? = null,
    val pixelArrayHeight: Int? = null,
    val maxJpegWidth: Int? = null,
    val maxJpegHeight: Int? = null,
    val maxSlowMotionWidth: Int? = null,
    val maxSlowMotionHeight: Int? = null,
    val maxSlowMotionFps: Int? = null,
    // null = capability info unavailable for this camera (REQUEST_AVAILABLE_CAPABILITIES was
    // null) — not "false". See DataProviderCamera for the quirk this preserves.
    val hasRawCapture: Boolean? = null,
    val hasManualSensor: Boolean? = null,
    val hasBurstCapture: Boolean? = null,
    val hasOis: Boolean? = null,
    val hasHdrSceneMode: Boolean? = null,
    // Only ever null or true (never explicit false) — matches original behavior.
    val hasMultiLens: Boolean? = null,
)

@Keep
data class CameraData(val totalCameras: Int, val lenses: List<CameraLensData>)
