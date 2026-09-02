package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmSensorsInfoBinding
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.feat.infor.base.copyToClipboardAndNotify
import com.galaxyjoy.cpuinfo.feat.infor.base.shrinkFabOnScroll
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FrmSensorsInfo :
    BaseFrm<FrmSensorsInfoBinding>(R.layout.frm_sensors_info),
    AdtInfoItems.OnClickListener {

    private val viewModel: VMSensorsInfo by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rv.layoutManager = LinearLayoutManager(requireContext())
        (binding.rv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.rv.addItemDecoration(DividerItemDecoration(requireContext()))

        val adtInfoItems = AdtInfoItems(
            viewModel.listLiveData,
            AdtInfoItems.LayoutType.VERTICAL_LAYOUT,
            onClickListener = this,
        )
        viewModel.listLiveData.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtInfoItems),
        )
        binding.rv.adapter = adtInfoItems

        binding.fabSensorTest.setOnClickListener {
            SensorTestBottomSheet().show(childFragmentManager, SensorTestBottomSheet.TAG)
        }

        binding.rv.shrinkFabOnScroll(binding.fabSensorTest)

        setUpMultiAxisChart(binding.chartAccelerometer)
        setUpMultiAxisChart(binding.chartGyroscope)
        setUpSingleAxisChart(binding.chartBarometer)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.waveformState.collect { state ->
                binding.waveformAccelerometerGroup.visibility = if (state.hasAccelerometer) View.VISIBLE else View.GONE
                binding.waveformGyroscopeGroup.visibility = if (state.hasGyroscope) View.VISIBLE else View.GONE
                binding.waveformBarometerGroup.visibility = if (state.hasBarometer) View.VISIBLE else View.GONE

                updateMultiAxisChart(binding.chartAccelerometer, state.accelerometer)
                updateMultiAxisChart(binding.chartGyroscope, state.gyroscope)
                updateSingleAxisChart(binding.chartBarometer, state.barometer)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startProvidingData()
    }

    override fun onStop() {
        viewModel.stopProvidingData()
        super.onStop()
    }

    override fun onDestroyView() {
        binding.rv.adapter = null
        super.onDestroyView()
    }

    override fun onItemLongPressed(item: Pair<String, String>) {
        copyToClipboardAndNotify(binding.mainContainer, item.second)
    }

    /** Small side-by-side sparklines, not full interactive charts like Dashboard's — no
     * drag/zoom (3 of these share a row, would fight each other for touch), no x-axis labels
     * (sample index isn't meaningful to the user, unlike Dashboard's elapsed-time axis). */
    private fun baseChartSetup(chart: LineChart) {
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawLabels(false)
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.setTouchEnabled(false)
        chart.setNoDataText(getString(R.string.sensor_waveform_collecting))
    }

    private fun setUpMultiAxisChart(chart: LineChart) {
        baseChartSetup(chart)
        chart.legend.apply {
            isEnabled = true
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(true)
        }
    }

    private fun setUpSingleAxisChart(chart: LineChart) {
        baseChartSetup(chart)
        chart.legend.isEnabled = false
    }

    private fun updateMultiAxisChart(chart: LineChart, axisSamples: List<List<Float>>) {
        if (axisSamples.all { it.isEmpty() }) {
            chart.clear()
            return
        }
        val axisLabels = listOf("X", "Y", "Z")
        val axisColors = listOf(Color.RED, Color.rgb(0, 160, 0), Color.BLUE)
        val dataSets = axisSamples.mapIndexedNotNull { axisIndex, samples ->
            if (samples.isEmpty()) return@mapIndexedNotNull null
            val entries = samples.mapIndexed { i, value -> Entry(i.toFloat(), value) }
            LineDataSet(entries, axisLabels.getOrElse(axisIndex) { "" }).apply {
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 1.5f
                color = axisColors.getOrElse(axisIndex) { Color.GRAY }
            }
        }
        chart.data = LineData(dataSets)
        chart.invalidate()
    }

    private fun updateSingleAxisChart(chart: LineChart, axisSamples: List<List<Float>>) {
        val samples = axisSamples.firstOrNull().orEmpty()
        if (samples.isEmpty()) {
            chart.clear()
            return
        }
        val entries = samples.mapIndexed { i, value -> Entry(i.toFloat(), value) }
        val color = ContextCompat.getColor(requireContext(), R.color.primary)
        val dataSet = LineDataSet(entries, "").apply {
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 1.5f
            setColor(color)
        }
        chart.data = LineData(dataSet)
        chart.invalidate()
    }
}
