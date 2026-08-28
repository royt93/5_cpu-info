package com.galaxyjoy.cpuinfo.feat.infor.camera

import android.content.res.Resources
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.R
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
    private val resources: Resources,
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

        listLiveData.add(resources.getString(R.string.camera_total) to ids.size.toString())

        ids.forEach { id ->
            val ch = try {
                cameraManager.getCameraCharacteristics(id)
            } catch (e: Exception) {
                Timber.w(e, "Cannot read characteristics for camera $id")
                return@forEach
            }

            // OEM HAL quirks (seen on Samsung/Xiaomi) can throw IllegalArgumentException /
            // AssertionError deep inside stream-configuration lookups for a single camera —
            // isolate per camera so one bad camera doesn't abort the whole init() and crash
            // the tab.
            try {
                populateCamera(id, ch)
            } catch (e: Throwable) {
                Timber.w(e, "Failed to read full characteristics for camera $id")
            }
        }
    }

    private fun populateCamera(id: String, ch: CameraCharacteristics) {
            val facingName = when (ch.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> resources.getString(R.string.front)
                CameraCharacteristics.LENS_FACING_BACK -> resources.getString(R.string.back)
                CameraCharacteristics.LENS_FACING_EXTERNAL ->
                    resources.getString(R.string.external_facing)
                else -> resources.getString(R.string.unknown)
            }
            listLiveData.add(resources.getString(R.string.camera_label, id) to facingName)

            ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.let { fls ->
                listLiveData.add(
                    "  " + resources.getString(R.string.camera_focal_lengths) to
                        fls.joinToString(", ") { "${it}mm" },
                )
            }

            ch.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.let { size ->
                listLiveData.add(
                    "  " + resources.getString(R.string.camera_sensor_size) to
                        "${size.width} × ${size.height} mm",
                )
            }

            ch.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)?.let { px ->
                listLiveData.add(
                    "  " + resources.getString(R.string.camera_pixel_array) to
                        "${px.width} × ${px.height}",
                )
            }

            ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.let { map ->
                describeStreams(map).forEach { listLiveData.add(it) }
            }

            ch.describeCapabilities().forEach { listLiveData.add(it) }
    }

    private fun describeStreams(map: StreamConfigurationMap): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()

        val jpegSizes = map.getOutputSizes(android.graphics.ImageFormat.JPEG)
        jpegSizes?.maxByOrNull { it.width.toLong() * it.height }?.let {
            rows.add(
                "  " + resources.getString(R.string.camera_max_jpeg) to
                    "${it.width} × ${it.height}",
            )
        }

        val highSpeedSizes = map.highSpeedVideoSizes
        if (highSpeedSizes?.isNotEmpty() == true) {
            val maxSize = highSpeedSizes.maxByOrNull { it.width.toLong() * it.height }!!
            val fpsRanges = map.getHighSpeedVideoFpsRangesFor(maxSize)
            val maxFps = fpsRanges?.maxOfOrNull { it.upper } ?: 0
            rows.add(
                "  " + resources.getString(R.string.camera_max_slow_motion) to
                    "${maxSize.width} × ${maxSize.height} @ ${maxFps}fps",
            )
        }

        return rows
    }

    private fun CameraCharacteristics.describeCapabilities(): List<Pair<String, String>> {
        val caps = get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: return emptyList()

        val rows = mutableListOf<Pair<String, String>>()
        rows.add(
            "  " + resources.getString(R.string.camera_raw_capture) to
                caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW).yesNo(),
        )
        rows.add(
            "  " + resources.getString(R.string.camera_manual_sensor) to
                caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
                    .yesNo(),
        )
        rows.add(
            "  " + resources.getString(R.string.camera_burst_capture) to
                caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)
                    .yesNo(),
        )

        get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.let { modes ->
            val hasOis = modes.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON }
            rows.add("  " + resources.getString(R.string.camera_ois) to hasOis.yesNo())
        }

        get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)?.let { scenes ->
            val hasHdr = scenes.any { it == CameraCharacteristics.CONTROL_SCENE_MODE_HDR }
            rows.add("  " + resources.getString(R.string.camera_hdr_scene_mode) to hasHdr.yesNo())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            get(CameraCharacteristics.LENS_POSE_TRANSLATION)?.let {
                rows.add(
                    "  " + resources.getString(R.string.camera_multi_lens) to
                        resources.getString(R.string.yes),
                )
            }
        }

        return rows
    }

    private fun Boolean.yesNo(): String =
        if (this) resources.getString(R.string.yes) else resources.getString(R.string.no)
}
