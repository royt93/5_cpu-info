package com.galaxyjoy.cpuinfo.data.provider

import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import com.galaxyjoy.cpuinfo.domain.model.DisplayModeData
import com.galaxyjoy.cpuinfo.domain.model.ScreenData
import com.galaxyjoy.cpuinfo.domain.model.ScreenDensityCategory
import com.galaxyjoy.cpuinfo.domain.model.ScreenDisplayInfo
import com.galaxyjoy.cpuinfo.domain.model.ScreenRealMetrics
import com.galaxyjoy.cpuinfo.domain.model.ScreenSizeCategory
import javax.inject.Inject

/**
 * Screen classification + [Display] metrics/capabilities — all static for the life of the
 * process, read once by [com.galaxyjoy.cpuinfo.domain.observable.ObservableScreenData].
 */
class DataProviderScreen @Inject constructor(
    private val resources: Resources,
    private val displayManager: DisplayManager,
) {

    fun getScreenData(): ScreenData {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        return ScreenData(
            sizeCategory = getSizeCategory(),
            densityCategory = getDensityCategory(),
            displayInfo = display?.let { getDisplayInfo(it) },
        )
    }

    private fun getDisplayInfo(display: Display): ScreenDisplayInfo {
        val absoluteMetrics = DisplayMetrics().also { display.getMetrics(it) }
        val xyDpiMetrics = getXyDpiMetrics(display)

        return ScreenDisplayInfo(
            realMetrics = getRealMetrics(display),
            absoluteWidthPx = absoluteMetrics.widthPixels,
            absoluteHeightPx = absoluteMetrics.heightPixels,
            refreshRate = display.refreshRate,
            rotation = display.rotation,
            xdpi = xyDpiMetrics.xdpi,
            ydpi = xyDpiMetrics.ydpi,
            supportedModes = display.supportedModes.map {
                DisplayModeData(it.physicalWidth, it.physicalHeight, it.refreshRate)
            },
            hdrTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                @Suppress("DEPRECATION")
                display.hdrCapabilities?.supportedHdrTypes?.toList() ?: emptyList()
            } else {
                null
            },
            isWideColorGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                display.isWideColorGamut
            } else {
                null
            },
            cutoutRectCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                display.cutout?.boundingRects?.size ?: -1
            } else {
                null
            },
        )
    }

    /** Classify screen into three main classes: large, normal, small */
    private fun getSizeCategory(): ScreenSizeCategory {
        val screenSize = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return when (screenSize) {
            Configuration.SCREENLAYOUT_SIZE_LARGE -> ScreenSizeCategory.LARGE
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> ScreenSizeCategory.NORMAL
            Configuration.SCREENLAYOUT_SIZE_SMALL -> ScreenSizeCategory.SMALL
            else -> ScreenSizeCategory.UNKNOWN
        }
    }

    /** Classify screen into density classes: ldpi, mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi */
    private fun getDensityCategory(): ScreenDensityCategory {
        return when (resources.displayMetrics.densityDpi) {
            DisplayMetrics.DENSITY_LOW -> ScreenDensityCategory.LDPI
            DisplayMetrics.DENSITY_MEDIUM -> ScreenDensityCategory.MDPI
            DisplayMetrics.DENSITY_TV, DisplayMetrics.DENSITY_HIGH -> ScreenDensityCategory.HDPI
            DisplayMetrics.DENSITY_XHIGH, DisplayMetrics.DENSITY_280 -> ScreenDensityCategory.XHDPI
            DisplayMetrics.DENSITY_XXHIGH, DisplayMetrics.DENSITY_360, DisplayMetrics.DENSITY_400,
            DisplayMetrics.DENSITY_420 -> ScreenDensityCategory.XXHDPI

            DisplayMetrics.DENSITY_XXXHIGH, DisplayMetrics.DENSITY_560 -> ScreenDensityCategory.XXXHDPI
            else -> ScreenDensityCategory.UNKNOWN
        }
    }

    @Suppress("DEPRECATION")
    private fun getRealMetrics(display: Display): ScreenRealMetrics? = try {
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        val density = metrics.density
        ScreenRealMetrics(
            widthPx = metrics.widthPixels,
            heightPx = metrics.heightPixels,
            dpWidth = (metrics.widthPixels / density).toInt(),
            dpHeight = (metrics.heightPixels / density).toInt(),
            density = density,
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    /**
     * Second, independent `getRealMetrics()` call made solely for xdpi/ydpi (mirrors the
     * pre-migration VM's `getExtendedDisplayInfo()`). Unlike [getRealMetrics] above, the
     * pre-migration call here had no try/catch — deliberately hardened with one, since this is
     * the same theoretically-throwing API and there's no reason a failure here should crash the
     * whole screen tab when [getRealMetrics] already treats it as a recoverable, defaultable case.
     */
    @Suppress("DEPRECATION")
    private fun getXyDpiMetrics(display: Display): DisplayMetrics = try {
        DisplayMetrics().also { display.getRealMetrics(it) }
    } catch (e: Exception) {
        e.printStackTrace()
        DisplayMetrics()
    }
}
