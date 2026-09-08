package com.galaxyjoy.cpuinfo.feat.setting

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class FrmSettingsTest {

    @Test
    fun `matches when text color equals the static accent color`() {
        assertTrue(isLikelyCategoryTitle(currentTextColor = 0xFF36C2CE.toInt(), staticAccentColor = 0xFF36C2CE.toInt()))
    }

    @Test
    fun `does not match a regular preference title color`() {
        assertFalse(isLikelyCategoryTitle(currentTextColor = 0xFF191C1D.toInt(), staticAccentColor = 0xFF36C2CE.toInt()))
    }

    @Test
    fun `does not match a color that merely looks similar`() {
        // Off-by-one in a single channel must not be treated as a match.
        assertFalse(isLikelyCategoryTitle(currentTextColor = 0xFF36C2CF.toInt(), staticAccentColor = 0xFF36C2CE.toInt()))
    }

    @Test
    fun `matches regardless of which concrete color values are used`() {
        // Dark-theme static accent is white (#FFFFFF), not teal — the function must not
        // hardcode the light-theme accent value.
        assertTrue(isLikelyCategoryTitle(currentTextColor = 0xFFFFFFFF.toInt(), staticAccentColor = 0xFFFFFFFF.toInt()))
    }
}
