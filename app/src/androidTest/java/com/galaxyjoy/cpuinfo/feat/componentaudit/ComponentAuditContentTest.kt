package com.galaxyjoy.cpuinfo.feat.componentaudit

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
 * Widget test — renders [ComponentAuditContent] directly with hand-built state, same pattern as
 * [com.galaxyjoy.cpuinfo.feat.foldable.FoldableDisplayContentTest].
 * [ComponentAuditFlowInstrumentedTest] covers the real scan against this device's actual visible
 * apps.
 */
@RunWith(AndroidJUnit4::class)
class ComponentAuditContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun noFindings_showsNoFindingsMessage() {
        composeRule.setContent {
            CpuInfoTheme { ComponentAuditContent(ComponentAuditResult(scannedPackageCount = 5, findings = emptyList())) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.component_audit_no_findings)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.component_audit_summary, 0, 5)).assertExists()
    }

    @Test
    fun withProviderFinding_showsFormattedRow() {
        val findings = listOf(
            UnguardedComponent("com.example.app", "Example App", ".MyProvider", UnguardedComponent.Type.PROVIDER),
        )
        composeRule.setContent {
            CpuInfoTheme { ComponentAuditContent(ComponentAuditResult(scannedPackageCount = 3, findings = findings)) }
        }

        val providerLabel = appContext.getString(R.string.component_audit_type_provider)
        val expectedRow = appContext.getString(R.string.component_audit_row, "Example App", ".MyProvider", providerLabel)
        composeRule.onNodeWithText(expectedRow).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.component_audit_summary, 1, 3)).assertExists()
    }

    @Test
    fun withReceiverFinding_showsFormattedRow() {
        val findings = listOf(
            UnguardedComponent("com.example.app", "Example App", ".MyReceiver", UnguardedComponent.Type.RECEIVER),
        )
        composeRule.setContent {
            CpuInfoTheme { ComponentAuditContent(ComponentAuditResult(scannedPackageCount = 1, findings = findings)) }
        }

        val receiverLabel = appContext.getString(R.string.component_audit_type_receiver)
        val expectedRow = appContext.getString(R.string.component_audit_row, "Example App", ".MyReceiver", receiverLabel)
        composeRule.onNodeWithText(expectedRow).assertExists()
    }

    @Test
    fun alwaysShowsScopeDisclaimer() {
        composeRule.setContent {
            CpuInfoTheme { ComponentAuditContent(ComponentAuditResult(scannedPackageCount = 0, findings = emptyList())) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.component_audit_scope_disclaimer)).assertExists()
    }
}
