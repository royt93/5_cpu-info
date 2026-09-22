package com.galaxyjoy.cpuinfo.feat.foldable

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test — renders [FoldableDisplayContent] directly with hand-built state, same pattern as
 * [com.galaxyjoy.cpuinfo.feat.touchdiag.TouchDiagContentTest]. [FoldableDisplayFlowInstrumentedTest]
 * covers the real live `WindowInfoTracker` collection against this device's actual displays.
 */
@RunWith(AndroidJUnit4::class)
class FoldableDisplayContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun singleDefaultDisplay_showsRowWithDefaultSuffix() {
        val displays = listOf(DisplayInfo(0, "Built-in Screen", isDefault = true, 1080, 2436, 420, "ON"))
        composeRule.setContent {
            CpuInfoTheme { FoldableDisplayContent(displays = displays, foldPostures = emptyList()) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.foldable_displays_section, 1)).assertExists()
        val expectedRow = appContext.getString(R.string.foldable_display_row, "Built-in Screen", 1080, 2436, 420, "ON") +
            " " + appContext.getString(R.string.foldable_default_display_suffix)
        composeRule.onNodeWithText(expectedRow).assertExists()
    }

    @Test
    fun secondaryDisplay_showsRowWithoutDefaultSuffix() {
        val displays = listOf(DisplayInfo(1, "USB-C DP", isDefault = false, 1920, 1080, 160, "OFF"))
        composeRule.setContent {
            CpuInfoTheme { FoldableDisplayContent(displays = displays, foldPostures = emptyList()) }
        }

        val expectedRow = appContext.getString(R.string.foldable_display_row, "USB-C DP", 1920, 1080, 160, "OFF")
        composeRule.onNodeWithText(expectedRow).assertExists()
    }

    @Test
    fun noFoldPostures_showsNotFoldableMessage() {
        composeRule.setContent {
            CpuInfoTheme { FoldableDisplayContent(displays = emptyList(), foldPostures = emptyList()) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.foldable_no_hinge)).assertExists()
    }

    @Test
    fun withFoldPosture_showsHingeRow() {
        val postures = listOf(FoldPosture(isSeparating = true, occlusionType = "NONE", orientation = "VERTICAL", state = "HALF_OPENED"))
        composeRule.setContent {
            CpuInfoTheme { FoldableDisplayContent(displays = emptyList(), foldPostures = postures) }
        }

        val yes = appContext.getString(R.string.yes)
        val expectedRow = appContext.getString(R.string.foldable_hinge_row, "HALF_OPENED", "VERTICAL", "NONE", yes)
        composeRule.onNodeWithText(expectedRow).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.foldable_no_hinge)).assertDoesNotExist()
    }
}
