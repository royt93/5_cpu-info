package com.galaxyjoy.cpuinfo.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

/**
 * U18 — small trend line for a single benchmark metric across its saved run history (oldest
 * first), shared by all 4 `feat.*bench`/`feat.throttle` "Done" screens rather than duplicated 4
 * times. X axis is just run index (1st, 2nd, ...) — benchmarks run sparsely and irregularly
 * (unlike Dashboard's continuous polling, which plots real elapsed seconds), so a run-count axis
 * is simpler and just as meaningful here. Renders nothing below 2 points — a single point isn't a
 * trend.
 */
@Composable
fun BenchTrendChart(values: List<Double>, modifier: Modifier = Modifier) {
    if (values.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.setDrawGridLines(false)
                // Default auto-labeling packs in too many near-duplicate labels when a metric
                // barely moves between runs (e.g. 120.1 vs 120.2 FPS) — cap it low since this is
                // a small trend glance, not a precision readout.
                axisLeft.setLabelCount(3, false)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f
                setTouchEnabled(false)
            }
        },
        update = { chart ->
            val entries = values.mapIndexed { index, value -> Entry(index.toFloat(), value.toFloat()) }
            val dataSet = LineDataSet(entries, "").apply {
                setDrawCircles(true)
                circleRadius = 3f
                setDrawCircleHole(false)
                setCircleColor(lineColor)
                setDrawValues(false)
                lineWidth = 2f
                color = lineColor
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
    )
}
