package com.galaxyjoy.cpuinfo.feat.foldable

import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import javax.inject.Inject

data class DisplayInfo(
    val id: Int,
    val name: String,
    val isDefault: Boolean,
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val state: String,
)

internal fun displayStateLabel(state: Int): String = when (state) {
    Display.STATE_ON -> "ON"
    Display.STATE_OFF -> "OFF"
    Display.STATE_DOZE -> "DOZE"
    Display.STATE_DOZE_SUSPEND -> "DOZE_SUSPEND"
    Display.STATE_ON_SUSPEND -> "ON_SUSPEND"
    Display.STATE_VR -> "VR"
    else -> "UNKNOWN"
}

/** E08 — every [DisplayManager.getDisplays] entry (internal panel, USB-C DP output, Chromecast...),
 * not just the default one [com.galaxyjoy.cpuinfo.feat.infor.screen.VMScreenInfo] already covers. */
class DisplayInventoryProvider @Inject constructor(private val displayManager: DisplayManager) {

    @Suppress("DEPRECATION")
    fun listDisplays(): List<DisplayInfo> = displayManager.displays.map { display ->
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        DisplayInfo(
            id = display.displayId,
            name = display.name,
            isDefault = display.displayId == Display.DEFAULT_DISPLAY,
            widthPx = metrics.widthPixels,
            heightPx = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            state = displayStateLabel(display.state),
        )
    }
}
