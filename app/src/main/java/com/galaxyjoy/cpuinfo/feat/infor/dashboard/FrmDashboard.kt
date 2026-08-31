package com.galaxyjoy.cpuinfo.feat.infor.dashboard

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmDashboardBinding
import com.galaxyjoy.cpuinfo.domain.model.TimeSeriesPoint
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * F01 — realtime dashboard: session-only history charts for CPU utilization, RAM usage, and
 * battery temperature. See [VMDashboard] for the rolling-buffer collection logic.
 */
@AndroidEntryPoint
class FrmDashboard : BaseFrm<FrmDashboardBinding>(R.layout.frm_dashboard) {

    private val viewModel: VMDashboard by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpChart(binding.chartCpuLoad)
        setUpChart(binding.chartRamUsed)
        setUpChart(binding.chartBatteryTemp)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                val hasAnyData = state.cpuLoadPoints.isNotEmpty() ||
                    state.ramUsedPoints.isNotEmpty() ||
                    state.batteryTempPoints.isNotEmpty()
                binding.tvDashboardEmptyState.visibility = if (hasAnyData) View.GONE else View.VISIBLE

                updateChart(binding.chartCpuLoad, state.cpuLoadPoints, R.color.primary)
                updateChart(binding.chartRamUsed, state.ramUsedPoints, R.color.primary)
                updateChart(binding.chartBatteryTemp, state.batteryTempPoints, R.color.primary)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startCollecting()
    }

    override fun onStop() {
        viewModel.stopCollecting()
        super.onStop()
    }

    private fun setUpChart(chart: LineChart) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.valueFormatter = ElapsedSecondsAxisFormatter()
        chart.setNoDataText(getString(R.string.dashboard_empty_state))
        // Preserve the user's own pinch-zoom/pan viewport across live data updates — this is the
        // interactive substitute for a manual 1/5/15-minute window picker (see VMDashboard doc).
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
    }

    private fun updateChart(chart: LineChart, points: List<TimeSeriesPoint>, colorRes: Int) {
        if (points.isEmpty()) {
            chart.clear()
            return
        }
        val firstTimestamp = points.first().timestampMs
        val entries = points.map { point ->
            Entry((point.timestampMs - firstTimestamp) / 1000f, point.value)
        }
        val color = ContextCompat.getColor(requireContext(), colorRes)
        val dataSet = LineDataSet(entries, "").apply {
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            setColor(color)
        }
        chart.data = LineData(dataSet)
        chart.invalidate()
    }
}
