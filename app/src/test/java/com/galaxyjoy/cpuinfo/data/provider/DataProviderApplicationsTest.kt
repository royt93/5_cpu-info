package com.galaxyjoy.cpuinfo.data.provider

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataProviderApplicationsTest {

    private val packageManager: PackageManager = mockk(relaxed = true)
    private val provider = DataProviderApplications(packageManager)

    private fun appInfo(pkg: String, isSystem: Boolean): ApplicationInfo {
        val info = ApplicationInfo()
        info.packageName = pkg
        info.flags = if (isSystem) ApplicationInfo.FLAG_SYSTEM else 0
        return info
    }

    @Test
    fun `withSystemApps=true returns all installed applications`() {
        val apps = listOf(
            appInfo("com.user.one", isSystem = false),
            appInfo("com.android.system", isSystem = true),
            appInfo("com.user.two", isSystem = false),
        )
        every { packageManager.getInstalledApplications(any<Int>()) } returns apps

        val result = provider.getInstalledApplications(withSystemApps = true)

        assertEquals(3, result.size)
    }

    @Test
    fun `withSystemApps=false filters out FLAG_SYSTEM apps`() {
        val apps = listOf(
            appInfo("com.user.one", isSystem = false),
            appInfo("com.android.system", isSystem = true),
            appInfo("com.user.two", isSystem = false),
        )
        every { packageManager.getInstalledApplications(any<Int>()) } returns apps

        val result = provider.getInstalledApplications(withSystemApps = false)

        assertEquals(2, result.size)
        assertTrue(result.all { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 })
    }

    @Test
    fun `empty installed list returns empty regardless of flag`() {
        every { packageManager.getInstalledApplications(any<Int>()) } returns emptyList()

        assertTrue(provider.getInstalledApplications(withSystemApps = true).isEmpty())
        assertTrue(provider.getInstalledApplications(withSystemApps = false).isEmpty())
    }
}
