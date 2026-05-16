package com.galaxyjoy.cpuinfo.data.provider

import android.app.ActivityManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class DataProviderRamTest {

    private val activityManager: ActivityManager = mockk(relaxed = true)
    private val provider = DataProviderRam(activityManager)

    @Test
    fun `getTotalBytes returns totalMem from ActivityManager`() {
        every { activityManager.getMemoryInfo(any()) } answers {
            firstArg<ActivityManager.MemoryInfo>().totalMem = 8_000_000_000L
        }

        assertEquals(8_000_000_000L, provider.getTotalBytes())
    }

    @Test
    fun `getAvailableBytes returns availMem from ActivityManager`() {
        every { activityManager.getMemoryInfo(any()) } answers {
            firstArg<ActivityManager.MemoryInfo>().availMem = 2_000_000_000L
        }

        assertEquals(2_000_000_000L, provider.getAvailableBytes())
    }

    @Test
    fun `getAvailablePercentage returns 25 when 2GB available of 8GB total`() {
        every { activityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.totalMem = 8_000_000_000L
            info.availMem = 2_000_000_000L
        }

        assertEquals(25, provider.getAvailablePercentage())
    }

    @Test
    fun `getThreshold returns threshold from ActivityManager`() {
        every { activityManager.getMemoryInfo(any()) } answers {
            firstArg<ActivityManager.MemoryInfo>().threshold = 500_000_000L
        }

        assertEquals(500_000_000L, provider.getThreshold())
    }
}
