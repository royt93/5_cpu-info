package com.galaxyjoy.cpuinfo.feat.setting

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.ActHost
import com.galaxyjoy.cpuinfo.util.isNightMode
import com.galaxyjoy.cpuinfo.util.resolveAccentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for `FrmSettings.applyDynamicAccentToPreferenceViews()` (Material You
 * audit) — androidx.preference has no per-instance color hook for either `PreferenceCategory`
 * title text or `SwitchPreferenceCompat`'s track/thumb, so that method walks the real
 * `RecyclerView` children at runtime. Real device only: `ColorStateList`/`CompoundButtonCompat`
 * resolution isn't reliable on the JVM unit test stub (`isReturnDefaultValues=true` returns
 * defaults, not real resolved colors) — same reasoning as `BaseRoundCornerProgressBarInstrumentedTest`.
 */
@RunWith(AndroidJUnit4::class)
class FrmSettingsAccentTintInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ActHost>()

    @Before
    fun dismissFirstLaunchLanguagePickerIfShown() {
        composeRule.waitForIdle()
        val systemDefaultLabel = composeRule.activity.getString(R.string.language_system_default)
        val appeared = runCatching {
            composeRule.waitUntil(timeoutMillis = 2_000) {
                composeRule.onAllNodesWithText(systemDefaultLabel).fetchSemanticsNodes().isNotEmpty()
            }
        }.isSuccess
        if (appeared) {
            // Dismiss via back press, NOT by picking an option: picking any option (even the
            // already-active "system default") calls LocaleManager.apply() ->
            // AppCompatDelegate.setApplicationLocales(), which recreates ActHost. On a true
            // first-launch device state this race loses ActivityScenario's tracking of the new
            // Activity instance ("Activity lifecycle changed event received but ignored because
            // the activity instance does not match" in logcat), and every later
            // composeRule.activity access throws "Cannot run onActivity since Activity has been
            // destroyed already". The bottom sheet has no isCancelable override (defaults to
            // true), so back-press dismisses it without touching locale state at all.
            pressBack()
            composeRule.waitForIdle()
        }
    }

    private fun openSettings() {
        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()
    }

    private fun preferenceRecyclerView(): RecyclerView {
        // androidx.preference builds this RecyclerView programmatically at its own id — not a
        // stable id in this app's own R class.
        val id = androidx.preference.R.id.recycler_view
        var rv: RecyclerView? = null
        // Activity.runOnUiThread() posts and returns immediately (this test thread is not the
        // main thread) — a plain call here races the assertions below against the still-pending
        // Runnable, leaving the lateinit var never assigned. runOnMainSync() blocks until the
        // Runnable actually finishes (bug found + fixed during this round's audit).
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            rv = composeRule.activity.findViewById(id)
        }
        composeRule.waitForIdle()
        return requireNotNull(rv) { "Settings PreferenceFragmentCompat's RecyclerView not found" }
    }

    private fun findAll(view: View, predicate: (View) -> Boolean, out: MutableList<View> = mutableListOf()): List<View> {
        if (predicate(view)) out.add(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) findAll(view.getChildAt(i), predicate, out)
        }
        return out
    }

    /**
     * RecyclerView only inflates on-screen rows — a preference several categories down (like
     * the health-alert switch) has no View at all until scrolled into view. Bug found + fixed
     * during this round's audit: [findAll] silently returned nothing for such rows.
     */
    private fun scrollToRowWithText(text: String) {
        onView(withId(androidx.preference.R.id.recycler_view)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(text))),
        )
        composeRule.waitForIdle()
    }

    @Test
    fun categoryTitle_isTintedAwayFromOldStaticAccent_notLeftCyan() {
        openSettings()
        val rv = preferenceRecyclerView()
        val staticAccent = ContextCompat.getColor(composeRule.activity, R.color.accent)
        val expectedAccent = composeRule.activity.resolveAccentColor(composeRule.activity.isNightMode())

        val vipCategoryTitle = composeRule.activity.getString(R.string.vip_menu_title)
        lateinit var categoryTextColors: List<Int>
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            categoryTextColors = findAll(rv, { it is TextView && it.text == vipCategoryTitle })
                .filterIsInstance<TextView>()
                .map { it.currentTextColor }
        }
        assertTrue("expected to find the '$vipCategoryTitle' category header row", categoryTextColors.isNotEmpty())
        categoryTextColors.forEach { color ->
            // resolveAccentColor() (Ext.kt) only computes a DIFFERENT-from-static dynamic color on
            // API 31+ (Build.VERSION_CODES.S) — below that it deliberately falls back to returning
            // the same static @color/accent (no Dynamic Color API exists pre-S). "not the old
            // static accent" is only a meaningful assertion where a dynamic alternative exists;
            // asserting it unconditionally fails by design on API < 31 (found 2026-09-09 running
            // this suite on a real Android 11 device for the first time this project — every prior
            // run was on Android 13+ devices, so this never surfaced before).
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                assertNotEquals(
                    "category header should no longer be painted with the old static @color/accent",
                    staticAccent,
                    color,
                )
            }
            // Exact match, not just "not the old cyan" — a flat "not equals staticAccent" check
            // alone previously passed even while the real bug painted these gray instead of
            // accent-colored (bug found + fixed during this round's audit, see FrmSettings.kt's
            // tintPreferenceView). Still meaningful on API < 31: proves tintPreferenceView applies
            // whatever resolveAccentColor() actually returns (here, correctly, the static fallback).
            assertEquals(
                "category header should be painted with the resolved dynamic accent color, not some other color",
                expectedAccent,
                color,
            )
        }
    }

    @Test
    fun regularPreferenceTitle_isNotAccidentallyTintedAsCategory() {
        openSettings()
        val rv = preferenceRecyclerView()

        // A regular Preference row's title (not a category) must keep its normal text color —
        // proves the "only re-color text that used to be the static accent color" heuristic in
        // FrmSettings.tintPreferenceView doesn't over-broadly repaint unrelated titles.
        val healthAlertTitle = composeRule.activity.getString(R.string.health_alert_pref_title)
        val staticAccent = ContextCompat.getColor(composeRule.activity, R.color.accent)
        scrollToRowWithText(healthAlertTitle)
        lateinit var titleColors: List<Int>
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            titleColors = findAll(rv, { it is TextView && it.text == healthAlertTitle })
                .filterIsInstance<TextView>()
                .map { it.currentTextColor }
        }
        assertTrue("expected to find the '$healthAlertTitle' preference row", titleColors.isNotEmpty())
        titleColors.forEach { color ->
            assertNotEquals(
                "a regular preference title was never the static accent color to begin with — " +
                    "if it now IS, the category-detection heuristic over-matched something it shouldn't have",
                staticAccent,
                color,
            )
        }
    }

    @Test
    fun switchPreference_checkedAndUncheckedTints_areDifferentColors() {
        openSettings()
        val rv = preferenceRecyclerView()
        scrollToRowWithText(composeRule.activity.getString(R.string.health_alert_pref_title))

        // pref.xml's app:widgetLayout swaps SwitchPreferenceCompat's default widget (plain
        // SwitchCompat — Material 2/AppCompat's thin-track/small-thumb style, not real Material
        // You) for com.google.android.material.materialswitch.MaterialSwitch (real M3 shape).
        // MaterialSwitch IS-A SwitchCompat (thumb/track tint APIs still apply), but must be
        // matched specifically here to guard against the widgetLayout override silently not
        // taking effect and falling back to the plain SwitchCompat again.
        lateinit var switches: List<com.google.android.material.materialswitch.MaterialSwitch>
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            switches = findAll(rv, { it is com.google.android.material.materialswitch.MaterialSwitch })
                .filterIsInstance<com.google.android.material.materialswitch.MaterialSwitch>()
        }
        assertTrue("expected at least one SwitchPreferenceCompat row using MaterialSwitch (health alert / bench reminder)", switches.isNotEmpty())

        switches.forEach { switch ->
            listOf(switch.thumbTintList to "thumb", switch.trackTintList to "track").forEach { (tintList, part) ->
                assertNotNull("switch $part should have a non-null tint list after Material You audit fix", tintList)
                val checkedColor = tintList!!.getColorForState(intArrayOf(android.R.attr.state_checked), 0)
                val uncheckedColor = tintList.getColorForState(intArrayOf(-android.R.attr.state_checked), 0)
                assertNotEquals(
                    "checked vs unchecked switch $part tint must differ, or the on/off state becomes " +
                        "visually indistinguishable (bug found + fixed during self-review this round)",
                    checkedColor,
                    uncheckedColor,
                )
            }
        }
    }
}
