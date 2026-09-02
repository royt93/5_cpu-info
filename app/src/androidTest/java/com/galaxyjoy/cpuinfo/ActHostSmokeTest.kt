package com.galaxyjoy.cpuinfo

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.feat.ActHost
import com.galaxyjoy.cpuinfo.feat.app.APPLICATIONS_LIST_TAG
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder
import com.galaxyjoy.cpuinfo.widget.progress.IconRoundCornerProgressBar
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke tests covering the Sprint 1+2 changes end to end on a real device/emulator:
 * native lib init (B01/B02), Applications screen migration (T2.1/B03/B03b), and the Sort/menu
 * parity work done before switching screens.
 */
@RunWith(AndroidJUnit4::class)
class ActHostSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ActHost>()

    /**
     * `ActHost` auto-shows a one-time language picker on a genuinely fresh install (no
     * `hasPickedLanguage` DataStore flag yet — see `maybeShowFirstLaunchLanguagePicker()`). Every
     * test below assumes it's landing on the normal toolbar/tab UI, so dismiss it first if the
     * install this test runs against happens to be fresh (real device installs aren't always
     * fresh across runs, so this only fires when the sheet is actually present).
     *
     * Dismiss via back-press, NOT by selecting a language (even "System default"): root-caused a
     * flake on 2026-09-02 where selecting anything in that sheet calls
     * `LocaleManager.applyNoFlicker()`, a deliberate finish()+startActivity() locale-apply
     * mechanism (see its kdoc) that creates a genuinely NEW ActHost instance. ComposeTestRule's
     * ActivityScenario only ever tracks the ONE instance it originally launched, so it doesn't
     * adopt the replacement — every later composeRule call then throws "Cannot run onActivity
     * since Activity has been destroyed already". Confirmed unrelated to any UI change by
     * reproducing identically against `git stash` (unmodified baseline). Back-press cancels the
     * BottomSheetDialogFragment without invoking the selection callback, so no locale gets
     * applied and no Activity swap happens — the flag also never gets persisted this way, so
     * this can fire again on a later test in the same run; that's fine, this hook handles it
     * every time regardless.
     */
    @Before
    fun dismissFirstLaunchLanguagePickerIfShown() {
        composeRule.waitForIdle()
        val systemDefaultLabel = composeRule.activity.getString(R.string.language_system_default)

        // maybeShowFirstLaunchLanguagePicker() reads the "already picked" flag from DataStore
        // asynchronously (lifecycleScope.launch { ... .first() }), so the sheet can still be a
        // beat away from actually showing right after launch — poll briefly instead of checking
        // only once, otherwise it can pop up mid-test and cover whatever the test clicks next.
        val appeared = runCatching {
            composeRule.waitUntil(timeoutMillis = 2_000) {
                composeRule.onAllNodesWithText(systemDefaultLabel).fetchSemanticsNodes().isNotEmpty()
            }
        }.isSuccess

        if (appeared) {
            pressBack()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun appLaunchesToHardwareTabWithoutCrashing() {
        composeRule.waitForIdle()
        // Reaching here without an exception already proves onCreate()/Hilt DI/native cpuinfo
        // lib init (B01/B02) succeeded on a real device — the failure mode for those bugs was a
        // crash before any UI could render.
    }

    @Test
    fun clusterTopologyCardsShowAtLeastOneCacheLevelOnDefaultCpuTab() {
        // Integration guard for U06's cache-per-cluster addition: real device data (this is the
        // one thing ClusterTopologyProviderTest-style unit tests can't cover — MockK can't mock
        // `external fun` native methods, see doc/task/epic-01-bugfix.md — so real
        // processor_start/processor_count values from libcpuinfo only get exercised here). CPU is
        // the default tab, no navigation needed. Substring-matches "L1i"/"L1d"/"L2"/"L3" rather
        // than a full formatted string since exact size/shared-count varies by real device.
        composeRule.waitForIdle()

        val hasAnyCacheLevelLabel = ClusterTopologyBuilder.CacheLevel.entries.any { level ->
            composeRule.onAllNodesWithText(level.label, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "Expected at least one cache-level label (L1i/L1d/L2/L3) on the default CPU tab",
            hasAnyCacheLevelLabel,
        )
    }

    @Test
    fun navigatingToApplicationsTabLoadsWithoutCrashing() {
        onView(withId(R.id.menuApplications)).perform(click())
        composeRule.waitForIdle()

        // Regression guard for T2.1: proves FrmNewApplications (Compose) is what's actually
        // wired into nav_graph.xml and inflates successfully — not the deleted FrmApplications.
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.apps_more_options)
        ).assertExists()
    }

    @Test
    fun applicationsOverflowMenuShowsSortAndAllFourPreviouslyMissingActions() {
        onView(withId(R.id.menuApplications)).perform(click())
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.apps_more_options)
        ).performClick()

        // Before this fix, the Compose screen was missing Sort (dead onClick) and all 4 of these
        // actions compared to the FrmApplications it replaced — see doc/task/epic-01-bugfix.md B03.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.apps_sort_order)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.rate_app)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.more_app)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.share_app)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.term_policy)).assertExists()
    }

    @Test
    fun shieldScoreBadgeOpensBottomSheetWithScoreAndStreak() {
        // The toolbar badge is a plain XML View (action_shield_score.xml), not a Compose node —
        // must drive it via Espresso. The resulting BottomSheetDialogFragment content IS Compose,
        // so that part is verified via ComposeTestRule below.
        onView(withId(R.id.actionShieldScoreBadge)).perform(click())
        composeRule.waitForIdle()

        // Regression guard for U10: the sheet must actually compute+render a score (not crash on
        // a real device's RAM/storage/battery state) and show the U09 streak section in the same
        // sheet.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.shield_score_title)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.streak_title)).assertExists()
    }

    @Test
    fun navigatingThroughAllBottomTabsDoesNotCrash() {
        onView(withId(R.id.menuApplications)).perform(click())
        composeRule.waitForIdle()
        onView(withId(R.id.menuTemperature)).perform(click())
        composeRule.waitForIdle()
        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()
        // No assertion beyond "didn't throw" — this is the regression guard for deleting
        // feat/processes (B11) and its nav_graph/menu_nav entries: the remaining tabs must
        // still all resolve and render.
    }

    @Test
    fun temperatureTabNeverShowsTheOldHardcodedUnsupportedSentence() {
        // Regression guard: AdtTemperature.kt used to hardcode a verbose, non-localized English
        // sentence for a row whose reading isn't available on this device/API level (shipped
        // since 2024-12-11 per git blame, unrelated to any recent change). Replaced with a short,
        // localized string + a non-bold, smaller text style — the old bold TextH7 heading style
        // (sized for a short reading like "32°C") rendered this sentence as a giant bold
        // paragraph, which is the actual bug report this guards against.
        onView(withId(R.id.menuTemperature)).perform(click())
        composeRule.waitForIdle()

        onView(
            withText(
                "Your device does not support providing the necessary information, so I am unable to retrieve the temperature.",
            ),
        ).check(doesNotExist())
    }

    /**
     * `com.google.android.material.tabs.TabLayout` extends `HorizontalScrollView`, so Espresso's
     * built-in `scrollTo()` (designed for ScrollView/HorizontalScrollView ancestors) brings the
     * target tab fully into view regardless of label width or locale — no need to guess a swipe
     * count or direction, which proved unreliable across devices (a fixed swipe count landed the
     * tab only partially visible on some screens, failing click()'s ≥90%-visible requirement).
     */
    private fun clickTabByText(label: String) {
        onView(withText(label)).perform(scrollTo(), click())
    }

    /**
     * `ViewPager2`'s tab-switch and Espresso's `withText(...).perform(click())` are plain-View
     * operations, not driven by Compose's clock — `composeRule.waitForIdle()` alone doesn't
     * reliably wait for a newly-swapped-in tab's Fragment to finish attaching its ComposeView (a
     * genuine, reproducible race, not device flakiness: confirmed via logcat showing zero
     * external app interference yet still hitting "No compose hierarchies found" right after a
     * tab switch). Poll for the expected label instead of a single fixed wait.
     */
    private fun waitForComposeText(label: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun throttleTestTabRunsFullCycleAndShowsResult() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        clickTabByText(composeRule.activity.getString(R.string.throttle))
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.throttle_start_button))
            .performClick()

        // Real hard-capped 30s stress test (U02) — waits for the actual run to finish on device
        // rather than faking it, so this is a genuine regression guard for the runner + verdict
        // pipeline, not just the idle-state UI.
        composeRule.waitUntil(timeoutMillis = 35_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.throttle_done_button))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.throttle_done_button)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.throttle_share_button)).assertExists()
    }

    @Test
    fun thermalStatusCardShowsOnThrottleTab() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        clickTabByText(composeRule.activity.getString(R.string.throttle))
        composeRule.waitForIdle()

        // F02 — passive PowerManager thermal status, independent of the active stress test below
        // it. Real device is API 29+, so the card must render rather than stay hidden.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.thermal_status_title)).assertExists()
    }

    @Test
    fun androidTabShowsSecurityChecklistRows() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        clickTabByText(composeRule.activity.getString(R.string.android))
        composeRule.waitForIdle()

        // F04 — regression guard for the 3 new security checklist rows added to VMAndroidInfo.
        // They sit below Build/root/encryption/StrongBox data in the plain (non-Compose)
        // RecyclerView — RecyclerViewActions.scrollTo() scrolls each into view precisely instead
        // of guessing a swipe count (which previously either under- or over-shot the target).
        scrollAndroidInfoListTo(composeRule.activity.getString(R.string.security_patch_level))
        onView(withText(composeRule.activity.getString(R.string.security_patch_level))).check(matches(isDisplayed()))
        scrollAndroidInfoListTo(composeRule.activity.getString(R.string.selinux_status))
        onView(withText(composeRule.activity.getString(R.string.selinux_status))).check(matches(isDisplayed()))
        scrollAndroidInfoListTo(composeRule.activity.getString(R.string.hardware_keystore))
        onView(withText(composeRule.activity.getString(R.string.hardware_keystore))).check(matches(isDisplayed()))
    }

    private fun scrollAndroidInfoListTo(label: String) {
        onView(withId(R.id.rv)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(label)))
        )
    }

    @Test
    fun deviceTruthFabOpensSheetWithEvidenceRows() {
        // CPU is the default tab, no navigation needed.
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        // Regression guard for U01: proves the native JNI additions (vendor/uarch/MIDR + the
        // signal-guarded MPIDR/REVIDR reads) don't crash on a real device and produce a rendered
        // verdict + evidence list — not just that they compile.
        onView(withId(R.id.fabDeviceTruth)).perform(click())
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.device_truth_title)).assertExists()
        composeRule.onNodeWithText("MIDR_EL1").assertExists()
        composeRule.onNodeWithText("MPIDR_EL1").assertExists()
        composeRule.onNodeWithText("REVIDR_EL1").assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.device_truth_share_button)).assertExists()
    }

    @Test
    fun aiReadinessBarOpensSheetWithScoreAndFlagRows() {
        // CPU is the default tab, no navigation needed.
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        // Regression guard for F10/U12: proves the 6 new ISA-flag JNI getters don't crash on a
        // real device and the score/tier + flag rows render — not just that they compile.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_bar_label))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_title)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_flag_neon_dot)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_flag_i8mm)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_flag_bf16)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_flag_fp16)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_flag_sve)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_disclaimer)).assertExists()
    }

    @Test
    fun hardwareSnapshotPrefOpensSheetWithoutCrashing() {
        // Regression guard for U03: proves the preference-screen entry point actually shows
        // HardwareSnapshotBottomSheet and it renders (via DataStore read + live hardware capture)
        // without crashing — either the empty-baseline or the diff state, depending on whatever a
        // prior run/device already persisted, so only the title (present in both states) is
        // asserted.
        onView(withId(R.id.menuSettings)).perform(click())
        Espresso.onIdle()

        // Same class of fix as languagePickerPrefOpensBottomSheetWithAllSupportedLocales(): the
        // Settings PreferenceFragmentCompat's RecyclerView doesn't necessarily have this row bound
        // yet if it sits below the fold — worse on a device with a large font_scale (e.g. TECNO
        // KJ7 at 1.45), where every row is taller and pushes later items further off-screen.
        val hardwareSnapshotLabel = composeRule.activity.getString(R.string.hardware_snapshot_pref_title)
        onView(isAssignableFrom(RecyclerView::class.java)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(hardwareSnapshotLabel)))
        )
        onView(withText(hardwareSnapshotLabel)).perform(click())
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.hardware_snapshot_title)).assertExists()
    }

    @Test
    fun vipDiagnosticHistoryPrefRoutesSomewhereWithoutCrashing() {
        // Regression guard for U07: the pref click branches on this test device's real
        // AdManager.isVipByKeyActive() — non-VIP routes to ActVip, a SEPARATE Activity that
        // replaces ActHost entirely. Deliberately does not call composeRule.waitForIdle() after
        // the click: composeRule is bound to ActHost specifically, and once ActVip launches and
        // destroys it, waitForIdle() throws "Cannot run onActivity since Activity has been
        // destroyed already" — a predictable consequence of the navigation succeeding, not a
        // crash. Espresso.onIdle() is activity-agnostic and safe either way. The sheet's own
        // save/history/dedup behavior (the VIP branch) is covered by VipDiagnosticContentTest +
        // VipDiagnosticReportRepositoryInstrumentedTest, both against a throwaway DataStore file
        // rather than this real device's actual history.
        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()

        onView(withText(composeRule.activity.getString(R.string.vip_diagnostic_pref_title))).perform(click())
        Espresso.onIdle()

        // Return to ActHost regardless of which branch fired, so later tests in this class don't
        // inherit a backgrounded/replaced Activity.
        pressBack()
    }

    @Test
    fun appPermissionsActionShowsPermissionsSheet() {
        // Regression guard for F05: proves long-pressing an app row -> "App Permissions" ->
        // reads real PackageManager permission data for that app without crashing. Uses this
        // app's own row (always present) since the installed-app list order is device-dependent.
        // A real daily-driver phone (unlike a lean dev/test device) has far more installed apps
        // to enumerate via PackageManager, so VMNewApplications' list load is slower and this
        // row isn't necessarily in the data set (let alone composed) within a short fixed wait —
        // confirmed on a real S24 Ultra: the list was still empty (maxValue=0 scroll range) when
        // a 5s wait + single performScrollToNode() attempt ran. Retry the scroll-to-node itself
        // inside waitUntil so it self-heals regardless of whether the row is merely off-screen or
        // the list is still loading.
        onView(withId(R.id.menuApplications)).perform(click())
        val appName = composeRule.activity.getString(R.string.app_name)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(APPLICATIONS_LIST_TAG).performScrollToNode(hasText(appName))
            }.isSuccess
        }

        composeRule.onNodeWithText(appName).performTouchInput { longClick() }
        val permissionsAction = composeRule.activity.getString(R.string.app_permissions_action)
        waitForComposeText(permissionsAction)

        composeRule.onNodeWithText(permissionsAction).performClick()

        val title = composeRule.activity.getString(R.string.app_permissions_title, appName)
        waitForComposeText(title)
        composeRule.onNodeWithText(title).assertExists()
    }

    @Test
    fun sensorTestFabRunsThroughAllStepsToADoneScreen() {
        // Regression guard for F07: proves the guided test flow actually registers real
        // SensorEventListeners per step (not just renders static UI) and reaches a Done screen
        // without crashing. Skips every step immediately rather than waiting for a real physical
        // action/timeout — this test only needs to prove the state machine advances and
        // terminates, not that detection itself works (that's SensorTestEvaluatorTest's job).
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        clickTabByText(composeRule.activity.getString(R.string.sensors))
        composeRule.waitForIdle()

        onView(withId(R.id.fabSensorTest)).perform(click())
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sensor_test_start_button))
            .performClick()
        composeRule.waitForIdle()

        val skipLabel = composeRule.activity.getString(R.string.sensor_test_skip_button)
        val doneLabel = composeRule.activity.getString(R.string.sensor_test_done_title)
        composeRule.waitUntil(timeoutMillis = 60_000) {
            val done = composeRule.onAllNodesWithText(doneLabel).fetchSemanticsNodes().isNotEmpty()
            if (!done) {
                val skipNodes = composeRule.onAllNodesWithText(skipLabel).fetchSemanticsNodes()
                if (skipNodes.isNotEmpty()) {
                    composeRule.onNodeWithText(skipLabel).performClick()
                }
            }
            done
        }

        composeRule.onNodeWithText(doneLabel).assertExists()
    }

    @Test
    fun languagePickerPrefOpensBottomSheetWithAllSupportedLocales() {
        onView(withId(R.id.menuSettings)).perform(click())
        Espresso.onIdle()

        // Settings is a plain PreferenceFragmentCompat backed by a lazily-bound RecyclerView — the
        // "Change language" row isn't necessarily laid out yet if it sits below the fold (real
        // root cause found via view-hierarchy dump: the row genuinely wasn't among the ~8 bound
        // children at failure time), same class of fix as scrollAndroidInfoListTo() elsewhere in
        // this file.
        val changeLabel = composeRule.activity.getString(R.string.language_change)
        onView(isAssignableFrom(RecyclerView::class.java)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(changeLabel)))
        )
        onView(withText(changeLabel)).perform(click())
        val title = composeRule.activity.getString(R.string.language_picker_title)
        waitForComposeText(title)

        composeRule.onNodeWithText(title).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.language_system_default)).assertExists()
        composeRule.onNodeWithText("English").assertExists()
        composeRule.onNodeWithText("Tiếng Việt").assertExists()
        composeRule.onNodeWithText("Čeština").assertExists()
        composeRule.onNodeWithText("Deutsch").assertExists()
        composeRule.onNodeWithText("Polski").assertExists()
        composeRule.onNodeWithText("繁體中文").assertExists()
    }

    @Test
    fun canMyDeviceBarOpensSheetWithRulesAndDisclaimer() {
        // CanMyDeviceBar lives on the CPU tab (FrmCpuInfo.kt), which is the default sub-tab — no
        // tab switch needed. The old `clickTabByText(drm)` here was a real bug: it navigated away
        // to a tab that never contains this bar at all, so the node search could never succeed.
        onView(withId(R.id.menuHardware)).perform(click())
        val barLabel = composeRule.activity.getString(R.string.can_my_device_bar_label)
        waitForComposeText(barLabel)

        composeRule.onNodeWithText(barLabel).performClick()

        val title = composeRule.activity.getString(R.string.can_my_device_title)
        waitForComposeText(title)
        composeRule.onNodeWithText(title).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.can_my_device_disclaimer)).assertExists()
    }

    @Test
    fun cpuFrequencyProgressBarsRenderOnDefaultTab() {
        // Regression guard for B20: `IconRoundCornerProgressBar`'s old `max/progress` formula
        // could divide by zero and produce NaN/Infinity width when a core reports max freq = 0
        // (unreadable cpufreq sysfs on some OEMs) — this exercises the bar with this real
        // device's actual frequency values, not a synthetic 0/0 case, since that specific reading
        // is device/kernel-state dependent and can't be forced from a test.
        composeRule.waitForIdle()
        onView(withId(R.id.rv))
            .check(matches(hasDescendant(isAssignableFrom(IconRoundCornerProgressBar::class.java))))
    }

    @Test
    fun hardwareTabAndBackRepeatedlyDoesNotCrash() {
        // Regression guard for B16: `FrmInfoContainer` now detaches its `TabLayoutMediator` in
        // onDestroyView() instead of leaking it. Each Hardware <-> Applications round trip
        // destroys/recreates FrmInfoContainer's view — repeating it several times is a stress
        // proxy for the leak fix (an actual leak isn't observable via Espresso, only via a
        // memory profiler, so this only proves the fix doesn't regress crash-free navigation).
        repeat(4) {
            onView(withId(R.id.menuHardware)).perform(click())
            composeRule.waitForIdle()
            onView(withId(R.id.menuApplications)).perform(click())
            composeRule.waitForIdle()
        }
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()
        onView(withId(R.id.rv))
            .check(matches(hasDescendant(isAssignableFrom(IconRoundCornerProgressBar::class.java))))
    }

    @Test
    fun vipRedeemFieldFocusThenImmediateBackDoesNotCrash() {
        // Regression guard for B21: `scrollToRedeemSection()`'s postDelayed(300ms) used to leak
        // its Runnable past onDestroyView(). Focusing the field posts that delayed scroll; back
        // then destroys the fragment's view before the 300ms elapses — exactly the race the fix
        // cancels via removeCallbacks() in onDestroyView(). Two presses because the first one
        // only dismisses the soft keyboard the focus click opened (standard Android behavior),
        // the second actually finishes ActVip and returns to ActHost.
        onView(withId(R.id.actionVipIcon)).perform(click())
        onView(withId(R.id.etRedeemKey)).perform(click())
        pressBack()
        pressBack()
        composeRule.waitForIdle()

        // Back on ActHost without a crash — the CPU tab (default) is still there.
        onView(withId(R.id.rv))
            .check(matches(hasDescendant(isAssignableFrom(IconRoundCornerProgressBar::class.java))))
    }

    // No instrumented test for the Dashboard (F01) or Storage Benchmark (F06) tabs: navigating to
    // either reproducibly trips test-harness-level failures, not app crashes. Confirmed via live
    // logcat capture on 2 different devices: TECNO KJ7 throws "Cannot run onActivity since
    // Activity has been destroyed already" from ActivityScenario's own teardown for BOTH tabs
    // (no FATAL EXCEPTION; process exits 0, normal "make process inactive" transition), while a
    // Pixel 7 Pro throws a completely different error for the same test
    // (NoSuchMethodException: android.hardware.input.InputManager.getInstance, a known
    // Espresso/Android-API-version compatibility issue in Espresso.onIdle() internals) — two
    // devices, two unrelated harness failures, zero app crashes, which rules out the app code.
    // Coverage for both features instead comes from their unit tests (VMDashboardTest/
    // HistoryBufferTest, StorageBenchmarkTest) and a clean assembleDevDebug/lintDevDebug with
    // MPAndroidChart wired in.
}

