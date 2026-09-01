package com.galaxyjoy.cpuinfo.feat.infor.screen

import android.view.Display
import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.domain.model.ScreenData
import com.galaxyjoy.cpuinfo.domain.model.ScreenDensityCategory
import com.galaxyjoy.cpuinfo.domain.model.ScreenSizeCategory
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseRvFragment
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import com.galaxyjoy.cpuinfo.util.round2
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmScreenInfo : BaseRvFragment() {

    private val viewModel: VMScreenInfo by viewModels()

    private val displayItems = ListLiveData<Pair<String, String>>()

    override fun setupRecyclerViewAdapter() {
        val adtInfoItems = AdtInfoItems(
            itemsObservableList = displayItems,
            layoutType = AdtInfoItems.LayoutType.HORIZONTAL_LAYOUT, onClickListener = this
        )
        displayItems.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtInfoItems)
        )
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            displayItems.replace(toDisplayItems(state.screenData))
        }
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext()))
        recyclerView.adapter = adtInfoItems
    }

    private fun toDisplayItems(data: ScreenData): List<Pair<String, String>> {
        val items = mutableListOf<Pair<String, String>>()
        items.add(getString(R.string.screen_class) to sizeCategoryLabel(data.sizeCategory))
        items.add(getString(R.string.density_class) to densityCategoryLabel(data.densityCategory))

        val displayInfo = data.displayInfo ?: return items

        displayInfo.realMetrics?.let { real ->
            items.add(getString(R.string.width) to "${real.widthPx}px")
            items.add(getString(R.string.height) to "${real.heightPx}px")
            items.add(getString(R.string.dp_width) to "${real.dpWidth}dp")
            items.add(getString(R.string.dp_height) to "${real.dpHeight}dp")
            items.add(getString(R.string.density) to "${real.density}")
        }

        items.add(getString(R.string.absolute_width) to "${displayInfo.absoluteWidthPx}px")
        items.add(getString(R.string.absolute_height) to "${displayInfo.absoluteHeightPx}px")
        items.add(getString(R.string.refresh_rate) to "${displayInfo.refreshRate.round2()}")
        items.add(getString(R.string.orientation) to "${displayInfo.rotation}")

        items.add("xdpi" to "${displayInfo.xdpi.round2()}")
        items.add("ydpi" to "${displayInfo.ydpi.round2()}")

        if (displayInfo.supportedModes.isNotEmpty()) {
            val description = displayInfo.supportedModes.joinToString(", ") {
                "${it.physicalWidth}×${it.physicalHeight}@${it.refreshRate.round2()}"
            }
            items.add(getString(R.string.display_supported_modes) to description)
        }

        displayInfo.hdrTypes?.let { hdrTypes ->
            val value = if (hdrTypes.isEmpty()) {
                getString(R.string.display_hdr_none)
            } else {
                hdrTypes.joinToString(", ") { hdrTypeName(it) }
            }
            items.add(getString(R.string.display_hdr_support) to value)
        }

        displayInfo.isWideColorGamut?.let { isWideColorGamut ->
            val yesNo = if (isWideColorGamut) getString(R.string.yes) else getString(R.string.no)
            items.add(getString(R.string.display_wide_color) to yesNo)
        }

        displayInfo.cutoutRectCount?.let { cutoutRectCount ->
            val value = if (cutoutRectCount >= 0) {
                getString(R.string.display_cutout_rects, cutoutRectCount)
            } else {
                getString(R.string.display_cutout_none)
            }
            items.add(getString(R.string.display_cutout) to value)
        }

        return items
    }

    private fun sizeCategoryLabel(category: ScreenSizeCategory): String = when (category) {
        ScreenSizeCategory.LARGE -> getString(R.string.large)
        ScreenSizeCategory.NORMAL -> getString(R.string.normal)
        ScreenSizeCategory.SMALL -> getString(R.string.small)
        ScreenSizeCategory.UNKNOWN -> getString(R.string.unknown)
    }

    private fun densityCategoryLabel(category: ScreenDensityCategory): String = when (category) {
        ScreenDensityCategory.LDPI -> "ldpi"
        ScreenDensityCategory.MDPI -> "mdpi"
        ScreenDensityCategory.HDPI -> "hdpi"
        ScreenDensityCategory.XHDPI -> "xhdpi"
        ScreenDensityCategory.XXHDPI -> "xxhdpi"
        ScreenDensityCategory.XXXHDPI -> "xxxhdpi"
        ScreenDensityCategory.UNKNOWN -> getString(R.string.unknown)
    }

    /** HDR profile labels are universally-recognised brand names — keep verbatim. */
    @Suppress("DEPRECATION")
    private fun hdrTypeName(type: Int): String = when (type) {
        Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
        Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
        Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
        4 -> "HDR10+" // Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS, API 29+
        else -> "Type $type"
    }
}
