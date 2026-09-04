package com.galaxyjoy.cpuinfo.feat.healthalert

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test

/** Same MockK-a-static-`WorkManager.getInstance()` pattern as
 * [com.galaxyjoy.cpuinfo.feat.shieldwidget.ShieldScoreWidgetProviderTest]. */
class HealthAlertSchedulerTest {

    private val context: Context = mockk(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `schedule enqueues a unique periodic work request`() {
        val workManager: WorkManager = mockk(relaxed = true)
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        HealthAlertScheduler.schedule(context)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                HealthAlertScheduler.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any<PeriodicWorkRequest>(),
            )
        }
    }

    @Test
    fun `cancel cancels the same unique work name`() {
        val workManager: WorkManager = mockk(relaxed = true)
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        HealthAlertScheduler.cancel(context)

        verify(exactly = 1) { workManager.cancelUniqueWork(HealthAlertScheduler.WORK_NAME) }
    }

    /** U26 — same toggle also schedules/cancels the weekly digest job. */
    @Test
    fun `schedule also enqueues the weekly digest under its own unique work name`() {
        val workManager: WorkManager = mockk(relaxed = true)
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        HealthAlertScheduler.schedule(context)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                HealthAlertScheduler.WEEKLY_DIGEST_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any<PeriodicWorkRequest>(),
            )
        }
    }

    @Test
    fun `cancel also cancels the weekly digest unique work name`() {
        val workManager: WorkManager = mockk(relaxed = true)
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        HealthAlertScheduler.cancel(context)

        verify(exactly = 1) { workManager.cancelUniqueWork(HealthAlertScheduler.WEEKLY_DIGEST_WORK_NAME) }
    }
}
