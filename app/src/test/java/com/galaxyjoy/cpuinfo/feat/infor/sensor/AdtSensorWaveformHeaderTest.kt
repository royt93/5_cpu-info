package com.galaxyjoy.cpuinfo.feat.infor.sensor

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Pure-logic contract for [AdtSensorWaveformHeader]: always exactly one header row, and
 * [AdtSensorWaveformHeader.withViews] must no-op (never throw, never run its block) before
 * RecyclerView has actually created the view holder — a real device only ever calls
 * `onCreateViewHolder` after the first layout pass, but `withViews` can be invoked earlier (e.g.
 * the very first `waveformState` emission racing view creation). The real chart/view behavior
 * (one-time setup firing, live updates landing on the retained holder, scrolling away with the
 * rest of the list) needs a real Android environment and is covered by
 * `ActHostSmokeTest.sensorsTabWaveformHeaderScrollsAwayInsteadOfStayingSticky` instead — this
 * project doesn't use Robolectric, so constructing a real inflated View isn't safe here.
 */
class AdtSensorWaveformHeaderTest {

    @Test
    fun itemCountIsAlwaysExactlyOne() {
        val adapter = AdtSensorWaveformHeader(onViewHolderCreated = {})

        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun withViewsNoOpsBeforeViewHolderIsCreated() {
        val adapter = AdtSensorWaveformHeader(onViewHolderCreated = {})
        var ran = false

        adapter.withViews { ran = true }

        assertFalse(ran, "withViews must not run its block before onCreateViewHolder has fired")
    }
}
