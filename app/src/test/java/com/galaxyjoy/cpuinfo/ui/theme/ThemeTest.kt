package com.galaxyjoy.cpuinfo.ui.theme

import android.os.Build
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ThemeTest {

    @Test
    fun `dynamic color enabled on API31+ when requested`() {
        assertTrue(shouldUseDynamicColor(dynamicColor = true, sdkInt = Build.VERSION_CODES.S))
        assertTrue(shouldUseDynamicColor(dynamicColor = true, sdkInt = Build.VERSION_CODES.S + 1))
    }

    @Test
    fun `dynamic color disabled below API31 even when requested`() {
        assertFalse(shouldUseDynamicColor(dynamicColor = true, sdkInt = Build.VERSION_CODES.S - 1))
        assertFalse(shouldUseDynamicColor(dynamicColor = true, sdkInt = Build.VERSION_CODES.N))
    }

    @Test
    fun `dynamic color disabled on API31+ when not requested`() {
        assertFalse(shouldUseDynamicColor(dynamicColor = false, sdkInt = Build.VERSION_CODES.S))
    }

    @Test
    fun `dynamic color disabled below API31 when not requested`() {
        assertFalse(shouldUseDynamicColor(dynamicColor = false, sdkInt = Build.VERSION_CODES.N))
    }
}
