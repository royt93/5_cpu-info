package com.galaxyjoy.cpuinfo.feat.shieldwidget

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
import org.junit.Assert.assertEquals
import org.junit.Test

class ShieldScoreWidgetProviderTest {

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

        ShieldScoreWidgetProvider.schedulePeriodicRefresh(context)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                ShieldScoreWidgetProvider.WORK_NAME,
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

        ShieldScoreWidgetProvider.cancelPeriodicRefresh(context)

        verify(exactly = 1) { workManager.cancelUniqueWork(ShieldScoreWidgetProvider.WORK_NAME) }
    }

    @Test
    fun `scoreColor matches the same 3-band thresholds as the bottom sheet`() {
        // Mirrors ShieldScoreBottomSheet's private scoreColor() thresholds exactly (0xFF4CAF50/
        // 0xFFFFA726/0xFFE53935) — kept as a separate function only because RemoteViews can't
        // consume a Compose Color, but the bands themselves must never drift apart.
        assertEquals(0xFF4CAF50.toInt(), ShieldScoreWidgetProvider.scoreColor(100))
        assertEquals(0xFF4CAF50.toInt(), ShieldScoreWidgetProvider.scoreColor(80))
        assertEquals(0xFFFFA726.toInt(), ShieldScoreWidgetProvider.scoreColor(79))
        assertEquals(0xFFFFA726.toInt(), ShieldScoreWidgetProvider.scoreColor(50))
        assertEquals(0xFFE53935.toInt(), ShieldScoreWidgetProvider.scoreColor(49))
        assertEquals(0xFFE53935.toInt(), ShieldScoreWidgetProvider.scoreColor(0))
    }
}
