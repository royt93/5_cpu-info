package com.galaxyjoy.cpuinfo.feat.componentaudit

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [ProviderInfo]/[ActivityInfo]/[PackageInfo]/[ApplicationInfo] are simple field-based classes
 * (not method-stubbed under the JVM unit-test android.jar) — real instances built + fields set
 * directly, same pattern already used elsewhere in this codebase (e.g. `DataProviderStorageTest`).
 */
class FindUnguardedComponentsTest {

    private val packageManager: PackageManager = mockk()

    private fun provider(exported: Boolean, readPermission: String?, writePermission: String?, name: String = "p") =
        ProviderInfo().apply {
            this.exported = exported
            this.readPermission = readPermission
            this.writePermission = writePermission
            this.name = name
        }

    private fun receiver(exported: Boolean, permission: String?, name: String = "r") =
        ActivityInfo().apply {
            this.exported = exported
            this.permission = permission
            this.name = name
        }

    private fun packageInfo(
        packageName: String = "com.example.app",
        providers: Array<ProviderInfo>? = null,
        receivers: Array<ActivityInfo>? = null,
    ) = PackageInfo().apply {
        this.packageName = packageName
        this.providers = providers
        this.receivers = receivers
        this.applicationInfo = ApplicationInfo().apply {
            this.packageName = packageName
            nonLocalizedLabel = "Example App"
        }
    }

    @Test
    fun `exported provider with no read or write permission is flagged`() {
        val pkg = packageInfo(providers = arrayOf(provider(exported = true, readPermission = null, writePermission = null)))

        val result = findUnguardedComponents(pkg, packageManager)

        assertEquals(1, result.size)
        assertEquals(UnguardedComponent.Type.PROVIDER, result[0].type)
        assertEquals("com.example.app", result[0].packageName)
    }

    @Test
    fun `exported provider guarded by either read or write permission is not flagged`() {
        val pkg = packageInfo(
            providers = arrayOf(
                provider(exported = true, readPermission = "some.perm", writePermission = null, name = "p1"),
                provider(exported = true, readPermission = null, writePermission = "some.perm", name = "p2"),
            ),
        )

        assertTrue(findUnguardedComponents(pkg, packageManager).isEmpty())
    }

    @Test
    fun `non-exported provider is never flagged even without permissions`() {
        val pkg = packageInfo(providers = arrayOf(provider(exported = false, readPermission = null, writePermission = null)))

        assertTrue(findUnguardedComponents(pkg, packageManager).isEmpty())
    }

    @Test
    fun `exported receiver with no permission is flagged`() {
        val pkg = packageInfo(receivers = arrayOf(receiver(exported = true, permission = null)))

        val result = findUnguardedComponents(pkg, packageManager)

        assertEquals(1, result.size)
        assertEquals(UnguardedComponent.Type.RECEIVER, result[0].type)
    }

    @Test
    fun `exported receiver guarded by a permission is not flagged`() {
        val pkg = packageInfo(receivers = arrayOf(receiver(exported = true, permission = "some.perm")))

        assertTrue(findUnguardedComponents(pkg, packageManager).isEmpty())
    }

    @Test
    fun `null providers and receivers arrays do not crash`() {
        val pkg = packageInfo(providers = null, receivers = null)

        assertTrue(findUnguardedComponents(pkg, packageManager).isEmpty())
    }

    @Test
    fun `falls back to package name when there is no ApplicationInfo to load a label from`() {
        // ApplicationInfo.loadLabel() itself can't be meaningfully exercised under the JVM
        // unit-test android.jar stub (isReturnDefaultValues=true replaces its body, so it always
        // returns null here regardless of nonLocalizedLabel — same class of stub limitation as
        // android.os.Bundle noted elsewhere in this codebase). This test instead proves the outer
        // null-safety: a package with no ApplicationInfo at all must still fall back to the
        // package name rather than crashing or producing a blank label.
        val pkg = packageInfo(providers = arrayOf(provider(exported = true, readPermission = null, writePermission = null)))
        pkg.applicationInfo = null

        val result = findUnguardedComponents(pkg, packageManager)

        assertEquals("com.example.app", result[0].appLabel)
    }
}
