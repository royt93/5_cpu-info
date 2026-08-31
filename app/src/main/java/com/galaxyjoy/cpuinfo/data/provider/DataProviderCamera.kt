package com.galaxyjoy.cpuinfo.data.provider

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.galaxyjoy.cpuinfo.domain.model.CameraData
import com.galaxyjoy.cpuinfo.domain.model.CameraFacing
import com.galaxyjoy.cpuinfo.domain.model.CameraLensData
import timber.log.Timber
import javax.inject.Inject

/**
 * Reads Camera2 characteristics metadata only — no CAMERA permission required.
 */
class DataProviderCamera @Inject constructor(
    private val cameraManager: CameraManager,
) {

    fun getCameraData(): CameraData {
        val ids = try {
            cameraManager.cameraIdList
        } catch (e: Exception) {
            Timber.w(e, "Cannot read cameraIdList")
            emptyArray()
        }

        val lenses = ids.mapNotNull { id ->
            val ch = try {
                cameraManager.getCameraCharacteristics(id)
            } catch (e: Exception) {
                Timber.w(e, "Cannot read characteristics for camera $id")
                return@mapNotNull null
            }

            // OEM HAL quirks (seen on Samsung/Xiaomi) can throw IllegalArgumentException /
            // AssertionError deep inside stream-configuration lookups for a single camera —
            // isolate per camera so one bad camera doesn't drop the whole list.
            try {
                buildLensData(id, ch)
            } catch (e: Throwable) {
                Timber.w(e, "Failed to read full characteristics for camera $id")
                null
            }
        }

        return CameraData(totalCameras = ids.size, lenses = lenses)
    }

    private fun buildLensData(id: String, ch: CameraCharacteristics): CameraLensData {
        val facing = when (ch.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
            CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
            else -> CameraFacing.UNKNOWN
        }

        val focalLengthsMm = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList()
        val sensorSize = ch.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val pixelArray = ch.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)

        val map = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val maxJpeg = map?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height }

        var maxSlowMotionWidth: Int? = null
        var maxSlowMotionHeight: Int? = null
        var maxSlowMotionFps: Int? = null
        val highSpeedSizes = map?.highSpeedVideoSizes
        if (highSpeedSizes?.isNotEmpty() == true) {
            val maxSize = highSpeedSizes.maxByOrNull { it.width.toLong() * it.height }!!
            val fpsRanges = map.getHighSpeedVideoFpsRangesFor(maxSize)
            maxSlowMotionWidth = maxSize.width
            maxSlowMotionHeight = maxSize.height
            maxSlowMotionFps = fpsRanges?.maxOfOrNull { it.upper } ?: 0
        }

        // NOTE: preserves an original quirk — when REQUEST_AVAILABLE_CAPABILITIES itself is
        // null, OIS/HDR/multi-lens are also skipped even though they come from unrelated
        // characteristics keys. Not fixing here, just carrying the old behavior forward.
        val caps = ch.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val hasRawCapture: Boolean?
        val hasManualSensor: Boolean?
        val hasBurstCapture: Boolean?
        val hasOis: Boolean?
        val hasHdrSceneMode: Boolean?
        val hasMultiLens: Boolean?
        if (caps == null) {
            hasRawCapture = null
            hasManualSensor = null
            hasBurstCapture = null
            hasOis = null
            hasHdrSceneMode = null
            hasMultiLens = null
        } else {
            hasRawCapture = caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
            hasManualSensor = caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
            hasBurstCapture = caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)
            hasOis = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                ?.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON }
            hasHdrSceneMode = ch.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
                ?.any { it == CameraCharacteristics.CONTROL_SCENE_MODE_HDR }
            hasMultiLens = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ch.get(CameraCharacteristics.LENS_POSE_TRANSLATION) != null
            ) {
                true
            } else {
                null
            }
        }

        return CameraLensData(
            id = id,
            facing = facing,
            focalLengthsMm = focalLengthsMm,
            sensorWidthMm = sensorSize?.width,
            sensorHeightMm = sensorSize?.height,
            pixelArrayWidth = pixelArray?.width,
            pixelArrayHeight = pixelArray?.height,
            maxJpegWidth = maxJpeg?.width,
            maxJpegHeight = maxJpeg?.height,
            maxSlowMotionWidth = maxSlowMotionWidth,
            maxSlowMotionHeight = maxSlowMotionHeight,
            maxSlowMotionFps = maxSlowMotionFps,
            hasRawCapture = hasRawCapture,
            hasManualSensor = hasManualSensor,
            hasBurstCapture = hasBurstCapture,
            hasOis = hasOis,
            hasHdrSceneMode = hasHdrSceneMode,
            hasMultiLens = hasMultiLens,
        )
    }
}
