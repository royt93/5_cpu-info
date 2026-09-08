package com.galaxyjoy.cpuinfo.util

import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for `View.hideSkeletonAfterFirstData()` (Material You audit — every
 * `feat/infor` tab was flashing blank/empty instead of any loading affordance). Real device
 * only: `ObjectAnimator`/`ColorStateList` don't resolve meaningfully on the JVM unit test stub
 * (`isReturnDefaultValues=true`), same reasoning as `BaseRoundCornerProgressBarInstrumentedTest`.
 *
 * Covers the exact bug caught and fixed mid-implementation: `FrmCpuInfo`/`FrmGpuInfo` wrap their
 * data-bearing adapter in a `ConcatAdapter` with an always-present Compose header — the *outer*
 * `ConcatAdapter.itemCount` is never 0 (the header alone counts as 1), so the function takes the
 * adapter explicitly rather than reading `recyclerView.adapter` (see [concatAdapterCase_watchesInnerAdapterNotOuterConcat]).
 */
@RunWith(AndroidJUnit4::class)
class SkeletonLoadingInstrumentedTest {

    private fun themedContext() = ContextThemeWrapper(
        InstrumentationRegistry.getInstrumentation().targetContext,
        R.style.AppThemeBase,
    )

    private fun newSkeletonView(): View = FrameLayout(themedContext())

    /** Minimal adapter with a mutable, externally-controlled item count. */
    private class FakeAdapter(initialCount: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var count = initialCount
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount() = count
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
    }

    @Test
    fun adapterAlreadyHasData_skeletonNeverShows() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var skeleton: View
        instrumentation.runOnMainSync {
            skeleton = newSkeletonView()
            skeleton.hideSkeletonAfterFirstData(FakeAdapter(initialCount = 3))
        }
        assertEquals(View.GONE, skeleton.visibility)
    }

    @Test
    fun adapterStartsEmpty_skeletonShowsAndPulses() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var skeleton: View
        instrumentation.runOnMainSync {
            skeleton = newSkeletonView()
            skeleton.hideSkeletonAfterFirstData(FakeAdapter(initialCount = 0))
        }
        assertEquals(View.VISIBLE, skeleton.visibility)
        assertTrue("skeleton should have a running pulse animator tagged on it", skeleton.tag != null)
    }

    @Test
    fun adapterEmptyThenNotifyDataSetChanged_hidesSkeleton() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var skeleton: View
        lateinit var adapter: FakeAdapter
        instrumentation.runOnMainSync {
            skeleton = newSkeletonView()
            adapter = FakeAdapter(initialCount = 0)
            skeleton.hideSkeletonAfterFirstData(adapter)
        }
        assertEquals(View.VISIBLE, skeleton.visibility)

        instrumentation.runOnMainSync { adapter.count = 5 } // triggers notifyDataSetChanged()

        assertEquals(View.GONE, skeleton.visibility)
    }

    @Test
    fun adapterEmptyThenNotifyItemRangeInserted_hidesSkeleton() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var skeleton: View
        lateinit var adapter: FakeAdapter
        instrumentation.runOnMainSync {
            skeleton = newSkeletonView()
            adapter = FakeAdapter(initialCount = 0)
            skeleton.hideSkeletonAfterFirstData(adapter)
        }

        instrumentation.runOnMainSync {
            adapter.count = 1
            adapter.notifyItemRangeInserted(0, 1)
        }

        assertEquals(View.GONE, skeleton.visibility)
    }

    @Test
    fun adapterNotifiesButStillEmpty_skeletonStaysVisible() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var skeleton: View
        lateinit var adapter: FakeAdapter
        instrumentation.runOnMainSync {
            skeleton = newSkeletonView()
            adapter = FakeAdapter(initialCount = 0)
            skeleton.hideSkeletonAfterFirstData(adapter)
        }

        // notifyDataSetChanged() with itemCount still 0 (e.g. list replaced with another empty
        // list) — must not be mistaken for "data arrived".
        instrumentation.runOnMainSync { adapter.notifyDataSetChanged() }

        assertEquals(View.VISIBLE, skeleton.visibility)
    }

    @Test
    fun concatAdapterCase_watchesInnerAdapterNotOuterConcat() {
        // Regression for the exact bug caught during this round's self-review: FrmCpuInfo/
        // FrmGpuInfo wrap the real data adapter in a ConcatAdapter alongside an always-present
        // Compose header adapter. If hideSkeletonAfterFirstData were ever called with the OUTER
        // ConcatAdapter instead of the inner one, itemCount would already be >=1 (the header)
        // and the skeleton would never show at all, even though the real list is still empty.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var skeleton: View
        lateinit var innerAdapter: FakeAdapter
        instrumentation.runOnMainSync {
            skeleton = newSkeletonView()
            val headerAdapter = FakeAdapter(initialCount = 1) // always-present header, like ComposeHeaderAdapter
            innerAdapter = FakeAdapter(initialCount = 0) // no real data yet
            val concat = ConcatAdapter(headerAdapter, innerAdapter)
            assertTrue("sanity: outer ConcatAdapter already has items from the header alone", concat.itemCount > 0)

            // Correct usage — pass the inner adapter, not `concat`.
            skeleton.hideSkeletonAfterFirstData(innerAdapter)
        }
        assertEquals(
            "skeleton must still show — the real list (innerAdapter) has no data yet, " +
                "regardless of the header making the outer ConcatAdapter non-empty",
            View.VISIBLE,
            skeleton.visibility,
        )

        instrumentation.runOnMainSync { innerAdapter.count = 4 }

        assertEquals(View.GONE, skeleton.visibility)
    }

    @Test
    fun stopSkeletonPulse_resetsAlphaAndCancelsAnimator() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var skeleton: View
        instrumentation.runOnMainSync {
            skeleton = newSkeletonView()
            skeleton.startSkeletonPulse()
        }
        assertEquals(View.VISIBLE, skeleton.visibility)

        instrumentation.runOnMainSync { skeleton.stopSkeletonPulse() }

        assertEquals(View.GONE, skeleton.visibility)
        assertEquals(1f, skeleton.alpha)
        assertFalse("tag (the animator reference) should be cleared", skeleton.tag != null)
    }
}
