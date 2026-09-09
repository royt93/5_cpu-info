package com.galaxyjoy.cpuinfo.feat.shield

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test — renders [ShieldScoreContent] directly with hand-built state, same pattern as
 * [com.galaxyjoy.cpuinfo.feat.throttle.ThrottleScreenTest]. Covers only the UI layer: tapping the
 * claim/double-up button must invoke the right callback exactly once. The callback's actual
 * `AdManager.grantVipDays(...)` wiring (2026-09-08 migration off the deprecated
 * `activateVipByKey(secret, days)` trick) is covered separately by
 * [com.galaxyjoy.cpuinfo.feat.vip.VipRedeemFlowInstrumentedTest] — no real AdManager call happens
 * in this file.
 */
@RunWith(AndroidJUnit4::class)
class ShieldScoreContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val score = ShieldScoreCalculator.Result(overall = 80, ramScore = 80, storageScore = 80, batteryScore = 80)

    @Test
    fun hasUnclaimedMilestone_tappingClaimButton_invokesOnClaimBaseClickedExactlyOnce() {
        var claimCount = 0

        composeRule.setContent {
            CpuInfoTheme {
                ShieldScoreContent(
                    score = score,
                    recordsBrokenCount = 0,
                    rebootCount = 0,
                    streak = 3,
                    hasUnclaimedMilestone = true,
                    justClaimedBase = false,
                    onClaimBaseClicked = { claimCount++ },
                    onDoubleUpClicked = { },
                )
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.streak_claim_button)).performClick()

        assert(claimCount == 1) { "expected onClaimBaseClicked to fire exactly once, fired $claimCount times" }
    }

    @Test
    fun justClaimedBase_tappingDoubleUpButton_invokesOnDoubleUpClickedExactlyOnce() {
        var doubleUpCount = 0

        composeRule.setContent {
            CpuInfoTheme {
                ShieldScoreContent(
                    score = score,
                    recordsBrokenCount = 0,
                    rebootCount = 0,
                    streak = 3,
                    hasUnclaimedMilestone = false,
                    justClaimedBase = true,
                    onClaimBaseClicked = { },
                    onDoubleUpClicked = { doubleUpCount++ },
                )
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.streak_claim_button_double)).performClick()

        assert(doubleUpCount == 1) { "expected onDoubleUpClicked to fire exactly once, fired $doubleUpCount times" }
    }

    @Test
    fun neitherState_showsNoMilestoneMessage_insteadOfEitherButton() {
        composeRule.setContent {
            CpuInfoTheme {
                ShieldScoreContent(
                    score = score,
                    recordsBrokenCount = 0,
                    rebootCount = 0,
                    streak = 1,
                    hasUnclaimedMilestone = false,
                    justClaimedBase = false,
                    onClaimBaseClicked = { },
                    onDoubleUpClicked = { },
                )
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.streak_no_milestone_yet)).assertExists()
    }
}
