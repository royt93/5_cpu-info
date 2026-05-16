package com.galaxyjoy.cpuinfo.feat.infor.screen

import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.round2
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel which is responsible for screen details
 *
 */
@HiltViewModel
class VMScreenInfo @Inject constructor(
    private val resources: Resources,
    private val windowManager: WindowManager,
    private val displayManager: DisplayManager,
) : ViewModel() {

    val listLiveData = ListLiveData<Pair<String, String>>()

    init {
        getScreenData()
    }

    /**
     * Get all screen details
     */
    private fun getScreenData() {
        if (listLiveData.isNotEmpty()) {
            listLiveData.clear()
        }
        listLiveData.add(getScreenClass())
        listLiveData.add(getDensityClass())
        listLiveData.addAll(getInfoFromDisplayMetrics())
        listLiveData.addAll(getExtendedDisplayInfo())
    }

    /**
     * Classify screen into three main classes: large, normal, small
     */
    private fun getScreenClass(): Pair<String, String> {
        val screenSize =
            resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

        val screenClass: String = when (screenSize) {
            Configuration.SCREENLAYOUT_SIZE_LARGE -> resources.getString(R.string.large)
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> resources.getString(R.string.normal)
            Configuration.SCREENLAYOUT_SIZE_SMALL -> resources.getString(R.string.small)
            else -> resources.getString(R.string.unknown)
        }

        return Pair(resources.getString(R.string.screen_class), screenClass)
    }

    /**
     * Classify screen into density classes: ldpi, mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi
     */
    private fun getDensityClass(): Pair<String, String> {
        val densityDpi = resources.displayMetrics.densityDpi

        val densityClass: String
        when (densityDpi) {
            DisplayMetrics.DENSITY_LOW -> {
                densityClass = "ldpi"
            }

            DisplayMetrics.DENSITY_MEDIUM -> {
                densityClass = "mdpi"
            }

            DisplayMetrics.DENSITY_TV, DisplayMetrics.DENSITY_HIGH -> {
                densityClass = "hdpi"
            }

            DisplayMetrics.DENSITY_XHIGH, DisplayMetrics.DENSITY_280 -> {
                densityClass = "xhdpi"
            }

            DisplayMetrics.DENSITY_XXHIGH, DisplayMetrics.DENSITY_360, DisplayMetrics.DENSITY_400,
            DisplayMetrics.DENSITY_420 -> {
                densityClass = "xxhdpi"
            }

            DisplayMetrics.DENSITY_XXXHIGH, DisplayMetrics.DENSITY_560 -> {
                densityClass = "xxxhdpi"
            }

            else -> {
                densityClass = resources.getString(R.string.unknown)
            }
        }

        return Pair(resources.getString(R.string.density_class), densityClass)
    }

    @Suppress("DEPRECATION")
    private fun getInfoFromDisplayMetrics(): List<Pair<String, String>> {
        val functionsList = mutableListOf<Pair<String, String>>()

        val display = windowManager.defaultDisplay

        val metrics = DisplayMetrics()
        try {
            display.getRealMetrics(metrics)
            functionsList.add(
                Pair(
                    resources.getString(R.string.width),
                    "${metrics.widthPixels}px"
                )
            )
            functionsList.add(
                Pair(
                    resources.getString(R.string.height),
                    "${metrics.heightPixels}px"
                )
            )

            val density = metrics.density
            val dpHeight = metrics.heightPixels / density
            val dpWidth = metrics.widthPixels / density

            functionsList.add(Pair(resources.getString(R.string.dp_width), "${dpWidth.toInt()}dp"))
            functionsList.add(
                Pair(
                    resources.getString(R.string.dp_height),
                    "${dpHeight.toInt()}dp"
                )
            )
            functionsList.add(Pair(resources.getString(R.string.density), "$density"))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        display.getMetrics(metrics)
        functionsList.add(
            Pair(
                resources.getString(R.string.absolute_width),
                "${metrics.widthPixels}px"
            )
        )
        functionsList.add(
            Pair(
                resources.getString(R.string.absolute_height),
                "${metrics.heightPixels}px"
            )
        )

        val refreshRate = display.refreshRate
        functionsList.add(
            Pair(
                resources.getString(R.string.refresh_rate),
                "${refreshRate.round2()}"
            )
        )

        val orientation = display.rotation
        functionsList.add(Pair(resources.getString(R.string.orientation), "$orientation"))

        return functionsList
    }

    /**
     * Surface display capabilities relevant to 2026 devices: supported refresh rate modes,
     * HDR capabilities, wide color gamut, notch/cutout, native xdpi/ydpi.
     */
    private fun getExtendedDisplayInfo(): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return rows

        // Per-axis pixel density — many displays have non-square pixels reported here
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        rows.add("xdpi" to "${metrics.xdpi.round2()}")
        rows.add("ydpi" to "${metrics.ydpi.round2()}")

        // Supported refresh rate modes (Android 6+)
        val modes = display.supportedModes
        if (modes.isNotEmpty()) {
            val description = modes.joinToString(", ") {
                "${it.physicalWidth}×${it.physicalHeight}@${it.refreshRate.round2()}"
            }
            rows.add("Supported modes" to description)
        }

        // HDR capabilities (Android 7+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val hdr = display.hdrCapabilities
            val types = hdr?.supportedHdrTypes?.map(::hdrTypeName) ?: emptyList()
            rows.add("HDR support" to if (types.isEmpty()) "No" else types.joinToString(", "))
        }

        // Wide color gamut (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            rows.add("Wide color gamut" to display.isWideColorGamut.yesNo())
        }

        // Cutout / notch (Android 9+, via window insets) — best-effort from current window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = display.cutout
            if (cutout != null) {
                val boundingRectsSize = cutout.boundingRects.size
                rows.add("Display cutout" to "$boundingRectsSize rect(s)")
            } else {
                rows.add("Display cutout" to "None")
            }
        }

        return rows
    }

    private fun hdrTypeName(@Suppress("DEPRECATION") type: Int): String = when (type) {
        Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
        Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
        Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
        4 -> "HDR10+" // Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS, API 29+
        else -> "Type $type"
    }

    private fun Boolean.yesNo() = if (this) "Yes" else "No"
}
