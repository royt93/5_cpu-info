package com.galaxyjoy.cpuinfo.widget.progress

import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.ViewHolderCpuFrequencyBinding
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T2.19 regression coverage: [BaseRoundCornerProgressBar.drawBackgroundProgress] used to
 * allocate a new [GradientDrawable] on every call — reachable from the overridden
 * `invalidate()`, which unconditionally triggers a full redraw. Verifies the background
 * drawable instance is now cached/reused instead of reallocated.
 */
@RunWith(AndroidJUnit4::class)
class BaseRoundCornerProgressBarInstrumentedTest {

    private fun inflateProgressBar(): IconRoundCornerProgressBar {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val themedContext = ContextThemeWrapper(appContext, R.style.AppThemeBase)
        val binding = ViewHolderCpuFrequencyBinding.inflate(LayoutInflater.from(themedContext))
        return binding.progressBarFrequency
    }

    @Test
    fun invalidate_reusesCachedBackgroundDrawable_acrossMultipleCalls() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var progressBar: IconRoundCornerProgressBar
        instrumentation.runOnMainSync { progressBar = inflateProgressBar() }

        instrumentation.runOnMainSync { progressBar.invalidate() }
        val firstDrawable = progressBar.layoutBackground.background

        instrumentation.runOnMainSync { progressBar.invalidate() }
        val secondDrawable = progressBar.layoutBackground.background

        assertSame(
            "background GradientDrawable should be reused, not reallocated, across invalidate() calls",
            firstDrawable,
            secondDrawable
        )
    }

    @Test
    fun setProgressBackgroundColor_reusesCachedDrawable_afterColorChange() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var progressBar: IconRoundCornerProgressBar
        instrumentation.runOnMainSync { progressBar = inflateProgressBar() }

        instrumentation.runOnMainSync { progressBar.invalidate() }
        val beforeColorChange = progressBar.layoutBackground.background

        instrumentation.runOnMainSync { progressBar.setProgressBackgroundColor(android.graphics.Color.RED) }
        val afterColorChange = progressBar.layoutBackground.background

        assertSame(
            "changing the background color should mutate the cached drawable, not allocate a new one",
            beforeColorChange,
            afterColorChange
        )
    }
}
