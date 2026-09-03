package com.galaxyjoy.cpuinfo.feat.lastbenchwidget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoContainerState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class LastBenchWidgetProviderTest {

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

        LastBenchWidgetProvider.schedulePeriodicRefresh(context)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                LastBenchWidgetProvider.WORK_NAME,
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

        LastBenchWidgetProvider.cancelPeriodicRefresh(context)

        verify(exactly = 1) { workManager.cancelUniqueWork(LastBenchWidgetProvider.WORK_NAME) }
    }

    @Test
    fun `tabPositionFor maps each kind to its real ViewPager2 tab position`() {
        assertEquals(AdtInfoContainerState.THROTTLE_POS, LastBenchWidgetProvider.tabPositionFor(LastBenchPicker.Kind.THROTTLE))
        assertEquals(AdtInfoContainerState.STORAGE_BENCH_POS, LastBenchWidgetProvider.tabPositionFor(LastBenchPicker.Kind.STORAGE))
        assertEquals(AdtInfoContainerState.RAM_BENCH_POS, LastBenchWidgetProvider.tabPositionFor(LastBenchPicker.Kind.RAM))
        assertEquals(AdtInfoContainerState.GPU_BENCH_POS, LastBenchWidgetProvider.tabPositionFor(LastBenchPicker.Kind.GPU))
    }
}
