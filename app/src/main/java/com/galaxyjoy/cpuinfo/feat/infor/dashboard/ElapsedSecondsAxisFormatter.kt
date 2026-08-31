package com.galaxyjoy.cpuinfo.feat.infor.dashboard

import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Locale

/** X axis values are seconds elapsed since the first sample — render as "Xm Ys ago". */
class ElapsedSecondsAxisFormatter : ValueFormatter() {

    override fun getFormattedValue(value: Float): String {
        val totalSeconds = value.toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) {
            String.format(Locale.US, "%dm%02ds", minutes, seconds)
        } else {
            String.format(Locale.US, "%ds", seconds)
        }
    }
}
