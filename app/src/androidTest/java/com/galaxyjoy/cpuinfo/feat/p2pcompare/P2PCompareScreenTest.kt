package com.galaxyjoy.cpuinfo.feat.p2pcompare

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test for [P2PCompareScreen] — same "render bare Compose UI with plain lambda params, no
 * Hilt" pattern as [com.galaxyjoy.cpuinfo.feat.allbench.AllBenchScreenTest]. The screen never
 * touches a real ViewModel here, only [VMP2PCompare.UiState] values passed directly.
 */
@RunWith(AndroidJUnit4::class)
class P2PCompareScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val localPayload = DeviceComparePayload.create("Local Phone", 8_000_000_000L, 128_000_000_000L)
    private val remotePayload = DeviceComparePayload.create("Remote Phone", 16_000_000_000L, 256_000_000_000L)

    @Test
    fun exportState_showsShareButtonAndPasteField() {
        composeRule.setContent {
            CpuInfoTheme {
                P2PCompareScreen(VMP2PCompare.UiState.Export(), {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.p2p_compare_share_button)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.p2p_compare_paste_label)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.p2p_compare_compare_button)).assertExists()
    }

    @Test
    fun exportState_withParseError_showsErrorText() {
        composeRule.setContent {
            CpuInfoTheme {
                P2PCompareScreen(VMP2PCompare.UiState.Export(pastedCode = "garbage", parseError = true), {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.p2p_compare_parse_error)).assertExists()
    }

    @Test
    fun exportState_withoutParseError_doesNotShowErrorText() {
        composeRule.setContent {
            CpuInfoTheme {
                P2PCompareScreen(VMP2PCompare.UiState.Export(), {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.p2p_compare_parse_error)).assertDoesNotExist()
    }

    @Test
    fun exportState_shareButtonClick_invokesCallback() {
        var shareClicked = false
        composeRule.setContent {
            CpuInfoTheme {
                P2PCompareScreen(VMP2PCompare.UiState.Export(), {}, {}, { shareClicked = true }, {})
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.p2p_compare_share_button)).performClick()

        assert(shareClicked) { "expected onShareClicked to be invoked" }
    }

    @Test
    fun exportState_compareButtonClick_invokesCallback() {
        var compareClicked = false
        composeRule.setContent {
            CpuInfoTheme {
                P2PCompareScreen(VMP2PCompare.UiState.Export(), {}, { compareClicked = true }, {}, {})
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.p2p_compare_compare_button)).performClick()

        assert(compareClicked) { "expected onCompareClicked to be invoked" }
    }

    @Test
    fun exportState_typingIntoPasteField_invokesOnPastedCodeChangedWithTypedText() {
        var typed: String? = null
        composeRule.setContent {
            CpuInfoTheme {
                P2PCompareScreen(VMP2PCompare.UiState.Export(), { typed = it }, {}, {}, {})
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.p2p_compare_paste_label)).performTextInput("hello")

        assert(typed == "hello") { "expected onPastedCodeChanged to receive the typed text, got: $typed" }
    }

    @Test
    fun resultState_showsBothDeviceModelsAndBackButton() {
        val comparison = DeviceCompareEvaluator.compare(localPayload, remotePayload)
        composeRule.setContent {
            CpuInfoTheme {
                P2PCompareScreen(VMP2PCompare.UiState.Result(comparison), {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText("Local Phone").assertExists()
        composeRule.onNodeWithText("Remote Phone").assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.p2p_compare_back_button)).assertExists()
    }

    @Test
    fun resultState_backButtonClick_invokesCallback() {
        var backClicked = false
        val comparison = DeviceCompareEvaluator.compare(localPayload, remotePayload)
        composeRule.setContent {
            CpuInfoTheme {
                P2PCompareScreen(VMP2PCompare.UiState.Result(comparison), {}, {}, {}, { backClicked = true })
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.p2p_compare_back_button)).performClick()

        assert(backClicked) { "expected onBackClicked to be invoked" }
    }
}
