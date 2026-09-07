package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression test for a bug found via manual device testing (2026-09-07): scrolling the Sensors
 * tab far enough away and back froze its waveform charts (e.g. the accelerometer chart stopped
 * updating). Root cause: RecyclerView reuses an existing ViewHolder instance from its recycled
 * pool via `onBindViewHolder()` when an item scrolls back into view — it does NOT call
 * `onCreateViewHolder()` again — but [AdtSensorWaveformHeader] only re-tracked the holder for
 * [AdtSensorWaveformHeader.withViews] inside `onCreateViewHolder`, so once `onViewRecycled()`
 * cleared that reference it never got repopulated, and every later `withViews` call silently
 * no-op'd forever even though the header was visibly back on screen.
 *
 * Exercises the adapter's real RecyclerView lifecycle callbacks directly against a real inflated
 * View (needs a real Android environment, not a JVM unit test — [AdtSensorWaveformHeaderTest]
 * covers what's safe to check without one) instead of depending on a real scroll: this device's
 * Sensors tab content is short enough to fit on one screen with nothing left to scroll (see
 * `ActHostSmokeTest.sensorsTabWaveformHeaderIsInsideRecyclerViewInsteadOfAStickySibling`'s kdoc),
 * so a live "scroll away and back" can't be exercised through the UI here.
 */
@RunWith(AndroidJUnit4::class)
class AdtSensorWaveformHeaderInstrumentedTest {

    // item_sensor_waveform_header.xml inflates an AppCompatTextView styled with a theme
    // attribute (@style/TextAnnotation1) — the bare target context isn't themed with the app's
    // AppCompat/Material theme, so inflating against it directly throws InflateException.
    private val context = ContextThemeWrapper(
        InstrumentationRegistry.getInstrumentation().targetContext,
        R.style.AppThemeBase,
    )

    @Test
    fun withViewsWorksAgainAfterViewHolderIsRecycledThenRebound() {
        var createdCount = 0
        var ranBeforeRecycle = false
        var ranAfterRebind = false

        // MPAndroidChart's LineChart constructs a GestureDetector, which requires a prepared
        // Looper — this test's own thread (the instrumentation thread) doesn't have one, only
        // the main/UI thread does, so inflating item_sensor_waveform_header.xml must happen there.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val adapter = AdtSensorWaveformHeader(onViewHolderCreated = { createdCount++ })
            val parent = FrameLayout(context)

            val holder = adapter.onCreateViewHolder(parent, 0)
            adapter.onBindViewHolder(holder, 0)

            adapter.withViews { ranBeforeRecycle = true }

            // Simulate RecyclerView scrolling the header away far enough to recycle it, then
            // back into view — reusing the SAME ViewHolder instance via onBindViewHolder rather
            // than calling onCreateViewHolder again, exactly like real RecyclerView does for a
            // pooled holder of a still-registered view type.
            adapter.onViewRecycled(holder)
            adapter.onBindViewHolder(holder, 0)

            adapter.withViews { ranAfterRebind = true }

            assertEquals("onCreateViewHolder should only run once — onBindViewHolder handles reuse", 1, createdCount)
        }

        assertTrue("withViews should run right after the first bind", ranBeforeRecycle)
        assertTrue(
            "withViews must still run after the holder is recycled and rebound — this is the " +
                "exact bug found on-device: charts froze after scrolling away and back",
            ranAfterRebind,
        )
    }
}
