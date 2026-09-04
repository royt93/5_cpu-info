package com.galaxyjoy.cpuinfo.util

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class WidgetSizeClassTest {

    @Test
    fun `below the threshold is not large`() {
        assertFalse(WidgetSizeClass.isLarge(WidgetSizeClass.LARGE_MIN_HEIGHT_DP - 1))
    }

    @Test
    fun `exactly at the threshold is large`() {
        assertTrue(WidgetSizeClass.isLarge(WidgetSizeClass.LARGE_MIN_HEIGHT_DP))
    }

    @Test
    fun `well above the threshold is large`() {
        assertTrue(WidgetSizeClass.isLarge(400))
    }

    @Test
    fun `the default just-placed height is not large`() {
        // Matches shield_score_widget_info.xml / last_bench_widget_info.xml's minHeight=90dp —
        // a freshly placed widget must keep showing the compact layout, not jump to detailed.
        assertFalse(WidgetSizeClass.isLarge(90))
    }

    @Test
    fun `zero or negative height is not large`() {
        assertFalse(WidgetSizeClass.isLarge(0))
        assertFalse(WidgetSizeClass.isLarge(-1))
    }
}
