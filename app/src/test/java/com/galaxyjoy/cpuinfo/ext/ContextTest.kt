package com.galaxyjoy.cpuinfo.ext

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ContextTest {

    private fun wrapperAround(base: Context): ContextWrapper {
        val wrapper: ContextWrapper = mockk()
        every { wrapper.baseContext } returns base
        return wrapper
    }

    @Test
    fun `findActivity on an Activity returns itself`() {
        val activity: Activity = mockk()

        assertSame(activity, activity.findActivity())
    }

    @Test
    fun `findActivity unwraps a single ContextWrapper layer`() {
        val activity: Activity = mockk()

        assertSame(activity, wrapperAround(activity).findActivity())
    }

    @Test
    fun `findActivity unwraps nested ContextWrapper layers`() {
        // Mirrors the real crash: Hilt's ViewComponentManager$FragmentContextWrapper wrapping the
        // Activity context inside a BottomSheetDialogFragment's ComposeView.
        val activity: Activity = mockk()
        val inner = wrapperAround(activity)
        val outer = wrapperAround(inner)

        assertSame(activity, outer.findActivity())
    }

    @Test
    fun `findActivity throws when no Activity is anywhere in the chain`() {
        val nonActivityBase: Context = mockk()

        assertFailsWith<IllegalStateException> { wrapperAround(nonActivityBase).findActivity() }
    }
}
