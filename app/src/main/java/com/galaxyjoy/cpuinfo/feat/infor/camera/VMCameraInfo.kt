package com.galaxyjoy.cpuinfo.feat.infor.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

/**
 * Surface Camera2 characteristics. Reads metadata only — no CAMERA permission required.
 *
 * For each camera ID emits: lens facing, focal lengths, sensor size, max resolution,
 * supported high-speed FPS (slow-motion), and capability flags (RAW, manual, OIS, HDR).
 */
@HiltViewModel
class VMCameraInfo @Inject constructor(
    private val cameraManager: CameraManager,
) : ViewModel() {

    val listLiveData = ListLiveData<Pair<String, String>>()

    init {
        if (listLiveData.isEmpty()) {
            populate()
        }
    }

    private fun populate() {
        val ids = try {
            cameraManager.cameraIdList
        } catch (e: Exception) {
            Timber.w(e, "Cannot read cameraIdList")
            emptyArray()
        }

        listLiveData.add("Total cameras" to ids.size.toString())

        ids.forEach { id ->
            val ch = try {
                cameraManager.getCameraCharacteristics(id)
            } catch (e: Exception) {
                Timber.w(e, "Cannot read characteristics for camera $id")
                return@forEach
            }

            val facingName = when (ch.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }
            listLiveData.add("Camera $id" to facingName)

            ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.let { fls ->
                listLiveData.add("  Focal lengths" to fls.joinToString(", ") { "${it}mm" })
            }

            ch.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.let { size ->
                listLiveData.add("  Sensor size" to "${size.width} × ${size.height} mm")
            }

            ch.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)?.let { px ->
                listLiveData.add("  Pixel array" to "${px.width} × ${px.height}")
            }

            ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.let { map ->
                describeStreams(map).forEach { listLiveData.add(it) }
            }

            ch.describeCapabilities().forEach { listLiveData.add(it) }
        }
    }

    private fun describeStreams(map: StreamConfigurationMap): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()

        val jpegSizes = map.getOutputSizes(android.graphics.ImageFormat.JPEG)
        jpegSizes?.maxByOrNull { it.width.toLong() * it.height }?.let {
            rows.add("  Max JPEG" to "${it.width} × ${it.height}")
        }

        val highSpeedSizes = map.highSpeedVideoSizes
        if (highSpeedSizes?.isNotEmpty() == true) {
            val maxSize = highSpeedSizes.maxByOrNull { it.width.toLong() * it.height }!!
            val fpsRanges = map.getHighSpeedVideoFpsRangesFor(maxSize)
            val maxFps = fpsRanges?.maxOfOrNull { it.upper } ?: 0
            rows.add("  Max slow-motion" to "${maxSize.width} × ${maxSize.height} @ ${maxFps}fps")
        }

        return rows
    }

    private fun CameraCharacteristics.describeCapabilities(): List<Pair<String, String>> {
        val caps = get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: return emptyList()

        val rows = mutableListOf<Pair<String, String>>()
        val raw = caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        val manual =
            caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
        val burst =
            caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)

        rows.add("  RAW capture" to raw.yesNo())
        rows.add("  Manual sensor" to manual.yesNo())
        rows.add("  Burst capture" to burst.yesNo())

        // OIS — available since API 23
        get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.let { modes ->
            val hasOis = modes.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON }
            rows.add("  OIS" to hasOis.yesNo())
        }

        // HDR scene mode hint — modern devices typically expose via Camera2 extensions
        get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)?.let { scenes ->
            val hasHdr =
                scenes.any { it == CameraCharacteristics.CONTROL_SCENE_MODE_HDR }
            rows.add("  HDR scene mode" to hasHdr.yesNo())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            get(CameraCharacteristics.LENS_POSE_TRANSLATION)?.let {
                // presence of pose translation hints at multi-camera physical lens
                rows.add("  Multi-camera lens" to "Yes")
            }
        }

        return rows
    }

    private fun Boolean.yesNo() = if (this) "Yes" else "No"
}
