package com.galaxyjoy.cpuinfo.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/**
 * Widget test for B23 — `DraggableBox` previously built its `MutableTransitionState` once via
 * `remember {}` with no key, so it only reacted to `isRevealed` on the very first composition;
 * later toggles (e.g. a `LazyColumn` row reused for a different item) never drove a new transition
 * run. `updateTransition(isRevealed, ...)` fixes that by re-keying on every `isRevealed` change.
 *
 * `DraggableBox` currently has no call site in the app's production UI (Applications moved to a
 * long-press bottom sheet instead of swipe-reveal), so this exercises the composable directly
 * rather than through app navigation.
 */
class DraggableBoxTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun revealedContentShiftsLeftOfCollapsedOnEveryToggle() {
        var isRevealed by mutableStateOf(false)

        composeRule.setContent {
            DraggableBox(
                isRevealed = isRevealed,
                onExpand = {},
                onCollapse = {},
                actionRow = {
                    Box(Modifier.size(width = 80.dp, height = 48.dp).testTag("actionRow"))
                },
                content = {
                    Box(Modifier.size(width = 200.dp, height = 48.dp).testTag("content"))
                },
            )
        }

        // Repeat several times to simulate a LazyColumn row reused across items with different
        // isRevealed values — the exact scenario the bug affected (only the first toggle was
        // reliable before the fix).
        repeat(3) { iteration ->
            isRevealed = true
            composeRule.waitForIdle()
            composeRule.mainClock.advanceTimeBy(1_000L)
            composeRule.waitForIdle()
            val revealedLeft = composeRule.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot.left

            isRevealed = false
            composeRule.waitForIdle()
            composeRule.mainClock.advanceTimeBy(1_000L)
            composeRule.waitForIdle()
            val collapsedLeft = composeRule.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot.left

            check(revealedLeft < collapsedLeft) {
                "toggle #$iteration: expected revealed offset ($revealedLeft) < collapsed offset ($collapsedLeft)"
            }
        }
    }
}
