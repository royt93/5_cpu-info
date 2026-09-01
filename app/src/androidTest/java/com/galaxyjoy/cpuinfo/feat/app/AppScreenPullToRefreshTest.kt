package com.galaxyjoy.cpuinfo.feat.app

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression guard for the Material2 -> Material3 pull-to-refresh migration in [ApplicationsScreen]
 * (rememberPullRefreshState/PullRefreshIndicator -> rememberPullToRefreshState/PullToRefreshContainer).
 * Verifies the `uiState.isLoading -> pullToRefreshState.startRefresh()/endRefresh()` wiring — the
 * half of the migration driven by real ViewModel state rather than a raw drag gesture.
 *
 * No automated test simulates the physical swipe-to-refresh gesture: a raw down()/moveBy()/up()
 * (and the swipeDown() helper) on the nestedScroll-connected container reproducibly fails to
 * trigger PullToRefreshState's onPreScroll/onPostScroll consumption under `createComposeRule()`
 * (confirmed via instrumented logcat — state's progress/isRefreshing never changes even though the
 * gesture lands on the correctly-measured node). The same synthetic-injection limitation was also
 * reproduced via raw `adb shell input swipe`/`draganddrop` on the real TECNO KJ7 device — neither
 * reliably drives this specific nested-scroll-overscroll interaction, a known class of difficulty
 * for touch-injection tooling in general, not a defect in this screen. Confirmed instead via a real
 * human-finger swipe on a TECNO KJ7 (2026-09-01): the refresh indicator appears and dismisses
 * correctly, alongside this file's exact match to the official M3 1.2.x `PullToRefreshState`
 * API/pattern and the two tests above proving the ViewModel-state-driven half.
 */
@RunWith(AndroidJUnit4::class)
class AppScreenPullToRefreshTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun isLoadingTrueShowsTheRefreshIndicator() {
        composeRule.setContent {
            CpuInfoTheme {
                ApplicationsScreen(
                    uiState = VMNewApplications.UiState(
                        isLoading = true,
                        applications = persistentListOf(),
                    ),
                    onAppClicked = {},
                    onRefreshApplications = {},
                    onSnackbarDismissed = {},
                    onCardExpanded = {},
                    onCardCollapsed = {},
                    onAppUninstallClicked = {},
                    onAppSettingsClicked = {},
                    onNativeLibsClicked = {},
                    onSystemAppsSwitched = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun isLoadingFalseHidesTheRefreshIndicator() {
        composeRule.setContent {
            CpuInfoTheme {
                ApplicationsScreen(
                    uiState = VMNewApplications.UiState(
                        isLoading = false,
                        applications = persistentListOf(),
                    ),
                    onAppClicked = {},
                    onRefreshApplications = {},
                    onSnackbarDismissed = {},
                    onCardExpanded = {},
                    onCardCollapsed = {},
                    onAppUninstallClicked = {},
                    onAppSettingsClicked = {},
                    onNativeLibsClicked = {},
                    onSystemAppsSwitched = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }
}
