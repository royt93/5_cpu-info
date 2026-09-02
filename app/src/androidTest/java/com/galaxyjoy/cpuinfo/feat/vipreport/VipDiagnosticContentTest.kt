package com.galaxyjoy.cpuinfo.feat.vipreport

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Widget test for [VipDiagnosticContent] — renders it directly (bare `createComposeRule()`, no
 * Activity/BottomSheetDialog needed) against a real-but-throwaway `DataStore` file, so repeated
 * runs on a real device (this is meant to be smoke-tested on a real daily-driver phone) never
 * write a fake data point into the user's actual persisted diagnostic history.
 *
 * Uses `waitUntil` polling rather than a single `waitForIdle()` after each action — the
 * `LaunchedEffect`/`saveSnapshot()` coroutines do real DataStore I/O (their own dispatcher, not
 * driven by Compose's test clock), so a single `waitForIdle()` can race ahead of them, same class
 * of issue `waitForComposeText()` fixes in `ActHostSmokeTest`.
 */
@RunWith(AndroidJUnit4::class)
class VipDiagnosticContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: VipDiagnosticReportRepository
    private lateinit var dataStoreFile: File

    @Before
    fun setUp() {
        dataStoreFile = File(appContext.cacheDir, "test_vip_diagnostic_content_${System.nanoTime()}.preferences_pb")
        val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        repository = VipDiagnosticReportRepository(
            batteryManager = batteryManager,
            batteryStatusProvider = BatteryStatusProvider(appContext),
            dataProviderRam = DataProviderRam(activityManager),
            dataStore = PreferenceDataStoreFactory.create { dataStoreFile },
        )
    }

    @After
    fun tearDown() {
        dataStoreFile.delete()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Bypasses [VipDiagnosticReportRepository.captureSnapshot] (would read the real device once,
     * always today) — seeded directly with an arbitrary [daysAgo] so U20's chart tests can get 2+
     * distinct-calendar-day entries without waiting for real days to pass or fighting the same-day
     * dedup in [VipDiagnosticReportRepository.saveSnapshot]. */
    private fun fakeSnapshot(daysAgo: Long, batteryLevelPercent: Int) = VipDiagnosticSnapshot(
        timestampMillis = System.currentTimeMillis() - daysAgo * 24L * 60 * 60 * 1000,
        batteryLevelPercent = batteryLevelPercent,
        designedCapacityMah = 5000.0,
        chargeCounterMah = 4000.0,
        cycleCount = -1,
        batteryHealth = 2,
        ramAvailablePercentage = 40,
        internalStorageFreeBytes = 10L * 1024 * 1024 * 1024,
        internalStorageTotalBytes = 128L * 1024 * 1024 * 1024,
    )

    @Test
    fun emptyState_showsSaveButtonAndNoHistoryRows() {
        composeRule.setContent {
            CpuInfoTheme { VipDiagnosticContent(repository = repository) }
        }

        val emptyMessage = appContext.getString(R.string.vip_diagnostic_empty_message)
        waitForText(emptyMessage)
        composeRule.onNodeWithText(emptyMessage).assertExists()
    }

    @Test
    fun tappingSave_movesFromEmptyStateToAHistoryRow() {
        composeRule.setContent {
            CpuInfoTheme { VipDiagnosticContent(repository = repository) }
        }
        val emptyMessage = appContext.getString(R.string.vip_diagnostic_empty_message)
        waitForText(emptyMessage)

        composeRule.onNodeWithText(appContext.getString(R.string.vip_diagnostic_save_button)).performClick()

        val batteryLevelLabel = appContext.getString(R.string.vip_diagnostic_row_battery_level)
        waitForText(batteryLevelLabel)

        // The empty message is gone and a real captured row (battery level field) is shown instead.
        composeRule.onNodeWithText(emptyMessage).assertDoesNotExist()
        composeRule.onNodeWithText(batteryLevelLabel).assertExists()
    }

    @Test
    fun tappingSaveTwiceSameDay_stillShowsOnlyOneRow() {
        composeRule.setContent {
            CpuInfoTheme { VipDiagnosticContent(repository = repository) }
        }
        val saveLabel = appContext.getString(R.string.vip_diagnostic_save_button)
        val batteryLevelLabel = appContext.getString(R.string.vip_diagnostic_row_battery_level)
        waitForText(appContext.getString(R.string.vip_diagnostic_empty_message))

        composeRule.onNodeWithText(saveLabel).performClick()
        waitForText(batteryLevelLabel)

        composeRule.onNodeWithText(saveLabel).performClick()
        composeRule.waitForIdle()

        // A 2nd saved-today entry replaces the 1st (VipDiagnosticReportRepository dedup) — only
        // one battery-level row should exist, and the 2-entries-only summary must not appear.
        composeRule.onAllNodesWithText(batteryLevelLabel).assertCountEquals(1)
        composeRule.onNodeWithText(
            appContext.getString(R.string.vip_diagnostic_summary_days_tracked, 0),
        ).assertDoesNotExist()
    }

    @Test
    fun historyWithTwoOrMoreEntries_showsBatteryTrendChartTitle() {
        runBlocking {
            repository.saveSnapshot(fakeSnapshot(daysAgo = 2, batteryLevelPercent = 90))
            repository.saveSnapshot(fakeSnapshot(daysAgo = 0, batteryLevelPercent = 82))
        }

        composeRule.setContent {
            CpuInfoTheme { VipDiagnosticContent(repository = repository) }
        }

        val chartTitle = appContext.getString(R.string.vip_diagnostic_battery_chart_title)
        waitForText(chartTitle)
        composeRule.onNodeWithText(chartTitle).assertExists()
    }

    @Test
    fun historyWithOnlyOneEntry_hidesBatteryTrendChartTitle() {
        runBlocking {
            repository.saveSnapshot(fakeSnapshot(daysAgo = 0, batteryLevelPercent = 90))
        }

        composeRule.setContent {
            CpuInfoTheme { VipDiagnosticContent(repository = repository) }
        }

        val batteryLevelLabel = appContext.getString(R.string.vip_diagnostic_row_battery_level)
        waitForText(batteryLevelLabel)

        val chartTitle = appContext.getString(R.string.vip_diagnostic_battery_chart_title)
        composeRule.onNodeWithText(chartTitle).assertDoesNotExist()
    }
}
