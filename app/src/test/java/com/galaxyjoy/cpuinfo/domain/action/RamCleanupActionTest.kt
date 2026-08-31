package com.galaxyjoy.cpuinfo.domain.action

import android.app.ActivityManager
import android.content.Context
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RamCleanupActionTest {

    private val context: Context = mockk {
        every { packageName } returns "com.galaxyjoy.cpuinfo"
    }
    private val activityManager: ActivityManager = mockk(relaxed = true)
    private val dispatchersProvider: DispatchersProvider = mockk {
        every { io } returns UnconfinedTestDispatcher()
    }
    private val action = RamCleanupAction(context, activityManager, dispatchersProvider)

    private fun processInfo(name: String) = ActivityManager.RunningAppProcessInfo().apply { processName = name }

    @Test
    fun `does not kill its own process`() = runTest {
        every { activityManager.runningAppProcesses } returns listOf(
            processInfo("com.galaxyjoy.cpuinfo"),
            processInfo("com.other.app"),
        )

        action(Unit)

        verify(exactly = 0) { activityManager.killBackgroundProcesses("com.galaxyjoy.cpuinfo") }
        verify(exactly = 1) { activityManager.killBackgroundProcesses("com.other.app") }
    }

    @Test
    fun `null runningAppProcesses is treated as empty instead of crashing`() = runTest {
        every { activityManager.runningAppProcesses } returns null

        action(Unit)

        verify(exactly = 0) { activityManager.killBackgroundProcesses(any()) }
    }

    @Test
    fun `one process failing to kill does not stop the remaining ones from being attempted`() = runTest {
        every { activityManager.runningAppProcesses } returns listOf(
            processInfo("com.first.app"),
            processInfo("com.second.app"),
            processInfo("com.third.app"),
        )
        every { activityManager.killBackgroundProcesses("com.second.app") } throws SecurityException("no permission")

        action(Unit)

        verify(exactly = 1) { activityManager.killBackgroundProcesses("com.first.app") }
        verify(exactly = 1) { activityManager.killBackgroundProcesses("com.second.app") }
        verify(exactly = 1) { activityManager.killBackgroundProcesses("com.third.app") }
    }

    @Test
    fun `a thrown exception reading runningAppProcesses is swallowed, not propagated`() = runTest {
        every { activityManager.runningAppProcesses } throws RuntimeException("system server dead")

        // Must not throw.
        action(Unit)
    }
}
