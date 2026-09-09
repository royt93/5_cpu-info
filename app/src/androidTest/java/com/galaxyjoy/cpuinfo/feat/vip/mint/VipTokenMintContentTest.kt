package com.galaxyjoy.cpuinfo.feat.vip.mint

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test for the debug-only VIP token minting tool (2026-09-08, VIP ECDSA token migration).
 * `onGenerate` is a fake here — the REAL `AdManager.generateVipToken` wiring is covered by
 * [VipTokenMintFlowInstrumentedTest] (mint + redeem round trip against the real SDK). This file
 * only proves the UI: input validation, and that a click forwards the typed private key/days and
 * renders whatever the callback returns.
 */
@RunWith(AndroidJUnit4::class)
class VipTokenMintContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blankPrivateKey_tappingGenerate_showsValidationErrorInsteadOfCallingGenerate() {
        var generateCalls = 0
        composeRule.setContent {
            CpuInfoTheme { VipTokenMintContent(onGenerate = { _, _ -> generateCalls++; "should-not-be-called" }) }
        }

        composeRule.onNodeWithText("Đúc token").performClick()

        composeRule.onNodeWithText("Nhập private key + số ngày hợp lệ (> 0)").assertExists()
        assert(generateCalls == 0) { "onGenerate must not fire when private key is blank" }
    }

    @Test
    fun validInput_tappingGenerate_forwardsPrivateKeyAndDays_showsReturnedToken() {
        var capturedKey: String? = null
        var capturedDays: Int? = null
        composeRule.setContent {
            CpuInfoTheme {
                VipTokenMintContent(
                    onGenerate = { key, days ->
                        capturedKey = key
                        capturedDays = days
                        "fake-generated-token-xyz"
                    },
                )
            }
        }

        composeRule.onNodeWithText("Private key (Base64)").performTextInput("my-private-key")
        composeRule.onNodeWithText("Số ngày").performTextInput("7")
        // Digits get appended to the pre-filled default "30" -> "307"; assert the callback saw
        // exactly what the field actually held, not what the test intended to type.
        composeRule.onNodeWithText("Đúc token").performClick()

        assert(capturedKey == "my-private-key") { "expected private key 'my-private-key', got '$capturedKey'" }
        assert(capturedDays != null) { "expected a parsed days value, got null" }
        composeRule.onNodeWithText("fake-generated-token-xyz").assertExists()
        composeRule.onNodeWithText("Copy").assertExists()
    }

    @Test
    fun generateReturnsBlank_showsFailureError_notTheTokenSection() {
        composeRule.setContent {
            CpuInfoTheme { VipTokenMintContent(onGenerate = { _, _ -> "" }) }
        }

        composeRule.onNodeWithText("Private key (Base64)").performTextInput("wrong-key")
        composeRule.onNodeWithText("Đúc token").performClick()

        composeRule.onNodeWithText("Đúc token thất bại — sai private key, hoặc build này không phải debug")
            .assertExists()
        composeRule.onNodeWithText("Copy").assertDoesNotExist()
    }
}
