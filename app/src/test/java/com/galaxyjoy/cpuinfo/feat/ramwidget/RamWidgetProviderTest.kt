package com.galaxyjoy.cpuinfo.feat.ramwidget

import android.app.ActivityManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.galaxyjoy.cpuinfo.domain.action.RamCleanupAction
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class RamWidgetProviderTest {

    private val context: Context = mockk(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `schedulePeriodicRefresh enqueues a unique periodic work request`() {
        val workManager: WorkManager = mockk(relaxed = true)
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        RamWidgetProvider.schedulePeriodicRefresh(context)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                RamWidgetProvider.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any<PeriodicWorkRequest>(),
            )
        }
    }

    @Test
    fun `cancelPeriodicRefresh cancels the same unique work name`() {
        val workManager: WorkManager = mockk(relaxed = true)
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        RamWidgetProvider.cancelPeriodicRefresh(context)

        verify(exactly = 1) { workManager.cancelUniqueWork(RamWidgetProvider.WORK_NAME) }
    }

    /**
     * Regression guard: the widget's "Clean RAM" button must both run the cleanup AND refresh
     * every placed widget afterwards — a bug that ran one but not the other would leave the
     * widget showing stale RAM numbers after cleanup, or silently skip the cleanup itself.
     */
    @Test
    fun `performCleanAndRefresh runs cleanup then refreshes every widget`() = runTest {
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns mockk<ActivityManager>()
        mockkConstructor(RamCleanupAction::class)
        coEvery { anyConstructed<RamCleanupAction>().invoke(Unit) } just Runs
        mockkObject(RamWidgetProvider)
        every { RamWidgetProvider.updateAllWidgets(context) } just Runs

        RamWidgetProvider.performCleanAndRefresh(context)

        verify(exactly = 1) { RamWidgetProvider.updateAllWidgets(context) }
    }
}
