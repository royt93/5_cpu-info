package com.galaxyjoy.cpuinfo.feat.infor.sensor

/**
 * F12 — fixed-size (not time-windowed, unlike Dashboard's [com.galaxyjoy.cpuinfo.feat.infor.dashboard.HistoryBuffer])
 * rolling buffer per sensor axis, capped at [maxSamples] most-recent readings. Sensor events arrive
 * at whatever rate the OS delivers them (`SENSOR_DELAY_NORMAL`), not on a fixed clock, so "last N
 * samples" (not "last N seconds") is the natural unit for a lightweight waveform sparkline.
 */
class SensorWaveformBuffer(private val axisCount: Int, private val maxSamples: Int = 50) {

    private val axisBuffers = List(axisCount) { ArrayDeque<Float>() }

    /** @param values must have at least [axisCount] elements — only the first [axisCount] are read. */
    fun record(values: FloatArray) {
        for (axis in 0 until axisCount) {
            val buffer = axisBuffers[axis]
            buffer.addLast(values[axis])
            if (buffer.size > maxSamples) buffer.removeFirst()
        }
    }

    /** One list per axis, oldest-first. Empty lists (not null) when nothing recorded yet. */
    fun snapshot(): List<List<Float>> = axisBuffers.map { it.toList() }
}
