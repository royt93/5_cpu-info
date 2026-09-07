package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.galaxyjoy.cpuinfo.R

/**
 * Single-item [RecyclerView.Adapter] hosting the 3-chart waveform row as item 0 of
 * [FrmSensorsInfo]'s RecyclerView ConcatAdapter, instead of a sticky sibling view above the
 * RecyclerView — so it scrolls away with the sensor value rows below and [shrinkFabOnScroll]
 * (which listens for real RecyclerView scroll deltas) keeps working unchanged.
 *
 * [onViewHolderCreated] runs one-time chart cosmetic setup (axis/legend config) exactly when the
 * chart views are created, since RecyclerView creates view holders lazily — not synchronously
 * when the adapter is attached. [withViews] then pushes live data updates directly onto the
 * retained holder, bypassing `notifyItemChanged` so continuous waveform updates never trigger a
 * full rebind/flicker.
 */
class AdtSensorWaveformHeader(
    private val onViewHolderCreated: (ViewHolder) -> Unit,
) : RecyclerView.Adapter<AdtSensorWaveformHeader.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val accelerometerGroup: LinearLayoutCompat = view.findViewById(R.id.waveformAccelerometerGroup)
        val gyroscopeGroup: LinearLayoutCompat = view.findViewById(R.id.waveformGyroscopeGroup)
        val barometerGroup: LinearLayoutCompat = view.findViewById(R.id.waveformBarometerGroup)
        val chartAccelerometer: LineChart = view.findViewById(R.id.chartAccelerometer)
        val chartGyroscope: LineChart = view.findViewById(R.id.chartGyroscope)
        val chartBarometer: LineChart = view.findViewById(R.id.chartBarometer)
    }

    private var viewHolder: ViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor_waveform_header, parent, false)
        return ViewHolder(view).also(onViewHolderCreated)
    }

    // RecyclerView calls this every time the holder is attached to position 0 — both right after
    // onCreateViewHolder (first creation) AND when the SAME holder is pulled back out of the
    // shared recycled-view pool after scrolling away and back (RecyclerView reuses an existing
    // instance via this callback instead of calling onCreateViewHolder again). withViews() must
    // re-track it here, not only in onCreateViewHolder — otherwise, once onViewRecycled below
    // clears the reference, it never gets a chance to be repopulated, and every later data update
    // silently no-ops forever even though the header is visibly back on screen (the real bug this
    // fixes: e.g. the accelerometer chart freezing after a scroll-away-then-back).
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        viewHolder = holder
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (viewHolder === holder) viewHolder = null
    }

    override fun getItemCount(): Int = 1

    /** Runs [block] against the retained view holder, or no-ops if it isn't attached yet. */
    fun withViews(block: ViewHolder.() -> Unit) {
        viewHolder?.block()
    }
}
