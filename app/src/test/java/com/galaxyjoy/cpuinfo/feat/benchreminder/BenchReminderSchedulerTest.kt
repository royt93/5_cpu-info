package com.galaxyjoy.cpuinfo.feat.benchreminder

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
 * [com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertSchedulerTest]. */
class BenchReminderSchedulerTest {

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

        BenchReminderScheduler.schedule(context)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                BenchReminderScheduler.WORK_NAME,
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

        BenchReminderScheduler.cancel(context)

        verify(exactly = 1) { workManager.cancelUniqueWork(BenchReminderScheduler.WORK_NAME) }
    }
}
