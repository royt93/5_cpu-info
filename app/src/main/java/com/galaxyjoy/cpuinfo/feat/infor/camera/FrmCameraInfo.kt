package com.galaxyjoy.cpuinfo.feat.infor.camera

import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.domain.model.CameraData
import com.galaxyjoy.cpuinfo.domain.model.CameraFacing
import com.galaxyjoy.cpuinfo.domain.model.CameraLensData
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseRvFragment
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmCameraInfo : BaseRvFragment() {

    private val viewModel: VMCameraInfo by viewModels()

    private val displayItems = ListLiveData<Pair<String, String>>()

    override fun setupRecyclerViewAdapter() {
        val adtInfoItems = AdtInfoItems(
            displayItems,
            AdtInfoItems.LayoutType.HORIZONTAL_LAYOUT,
            onClickListener = this,
        )
        displayItems.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtInfoItems),
        )
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            displayItems.replace(toDisplayItems(state.cameraData))
        }
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext()))
        recyclerView.adapter = adtInfoItems
    }

    private fun toDisplayItems(data: CameraData): List<Pair<String, String>> {
        val items = mutableListOf<Pair<String, String>>()
        items.add(getString(R.string.camera_total) to data.totalCameras.toString())
        data.lenses.forEach { items.addAll(toDisplayRows(it)) }
        return items
    }

    private fun toDisplayRows(lens: CameraLensData): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()

        val facingName = when (lens.facing) {
            CameraFacing.FRONT -> getString(R.string.front)
            CameraFacing.BACK -> getString(R.string.back)
            CameraFacing.EXTERNAL -> getString(R.string.external_facing)
            CameraFacing.UNKNOWN -> getString(R.string.unknown)
        }
        rows.add(getString(R.string.camera_label, lens.id) to facingName)

        lens.focalLengthsMm?.let { fls ->
            rows.add(
                "  " + getString(R.string.camera_focal_lengths) to
                    fls.joinToString(", ") { "${it}mm" },
            )
        }

        if (lens.sensorWidthMm != null && lens.sensorHeightMm != null) {
            rows.add(
                "  " + getString(R.string.camera_sensor_size) to
                    "${lens.sensorWidthMm} × ${lens.sensorHeightMm} mm",
            )
        }

        if (lens.pixelArrayWidth != null && lens.pixelArrayHeight != null) {
            rows.add(
                "  " + getString(R.string.camera_pixel_array) to
                    "${lens.pixelArrayWidth} × ${lens.pixelArrayHeight}",
            )
        }

        if (lens.maxJpegWidth != null && lens.maxJpegHeight != null) {
            rows.add(
                "  " + getString(R.string.camera_max_jpeg) to
                    "${lens.maxJpegWidth} × ${lens.maxJpegHeight}",
            )
        }

        if (lens.maxSlowMotionWidth != null && lens.maxSlowMotionHeight != null) {
            rows.add(
                "  " + getString(R.string.camera_max_slow_motion) to
                    "${lens.maxSlowMotionWidth} × ${lens.maxSlowMotionHeight} @ ${lens.maxSlowMotionFps}fps",
            )
        }

        lens.hasRawCapture?.let { rows.add("  " + getString(R.string.camera_raw_capture) to it.yesNo()) }
        lens.hasManualSensor?.let { rows.add("  " + getString(R.string.camera_manual_sensor) to it.yesNo()) }
        lens.hasBurstCapture?.let { rows.add("  " + getString(R.string.camera_burst_capture) to it.yesNo()) }
        lens.hasOis?.let { rows.add("  " + getString(R.string.camera_ois) to it.yesNo()) }
        lens.hasHdrSceneMode?.let { rows.add("  " + getString(R.string.camera_hdr_scene_mode) to it.yesNo()) }
        lens.hasMultiLens?.let {
            rows.add("  " + getString(R.string.camera_multi_lens) to getString(R.string.yes))
        }

        return rows
    }

    private fun Boolean.yesNo(): String =
        if (this) getString(R.string.yes) else getString(R.string.no)
}
