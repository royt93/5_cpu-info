package com.galaxyjoy.cpuinfo.feat.setting

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import com.galaxyjoy.cpuinfo.util.SystemInfoExporter.Format
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test — renders [ExportFormatContent] directly with hand-built state, same pattern as
 * [com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreContentTest]. Covers the UI layer only: every row
 * (including the new HTML one, 2026-09-21) must exist and invoke [onPicked] with the right [Format]
 * exactly once. The actual share/export wiring is covered separately by
 * [ExportFormatFlowInstrumentedTest].
 */
@RunWith(AndroidJUnit4::class)
class ExportFormatContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun label(format: Format) = appContext.getString(
        when (format) {
            Format.TEXT -> R.string.export_as_text
            Format.JSON -> R.string.export_as_json
            Format.HTML -> R.string.export_as_html
            Format.IMAGE -> R.string.export_as_image
        },
    )

    private fun setContentAndCapturePicks(initial: Format): MutableList<Format> {
        val picked = mutableListOf<Format>()
        composeRule.setContent {
            CpuInfoTheme {
                ExportFormatContent(initial = initial, onPicked = { picked.add(it) })
            }
        }
        return picked
    }

    @Test
    fun allFourFormatRowsAreShown() {
        setContentAndCapturePicks(initial = Format.TEXT)

        for (format in Format.entries) {
            composeRule.onNodeWithText(label(format)).assertExists()
        }
    }

    @Test
    fun tappingTextRow_invokesOnPickedWithTextExactlyOnce() {
        val picked = setContentAndCapturePicks(initial = Format.JSON)

        composeRule.onNodeWithText(label(Format.TEXT)).performClick()

        assert(picked == listOf(Format.TEXT)) { "expected exactly [TEXT], got $picked" }
    }

    @Test
    fun tappingJsonRow_invokesOnPickedWithJsonExactlyOnce() {
        val picked = setContentAndCapturePicks(initial = Format.TEXT)

        composeRule.onNodeWithText(label(Format.JSON)).performClick()

        assert(picked == listOf(Format.JSON)) { "expected exactly [JSON], got $picked" }
    }

    /** Regression guard for the 2026-09-21 addition — must be wired to the real [Format.HTML]. */
    @Test
    fun tappingHtmlRow_invokesOnPickedWithHtmlExactlyOnce() {
        val picked = setContentAndCapturePicks(initial = Format.TEXT)

        composeRule.onNodeWithText(label(Format.HTML)).performClick()

        assert(picked == listOf(Format.HTML)) { "expected exactly [HTML], got $picked" }
    }

    @Test
    fun tappingImageRow_invokesOnPickedWithImageExactlyOnce() {
        val picked = setContentAndCapturePicks(initial = Format.TEXT)

        composeRule.onNodeWithText(label(Format.IMAGE)).performClick()

        assert(picked == listOf(Format.IMAGE)) { "expected exactly [IMAGE], got $picked" }
    }
}
