package com.galaxyjoy.cpuinfo.data.provider

import android.app.admin.DevicePolicyManager
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Settings
import com.galaxyjoy.cpuinfo.domain.model.EncryptionStatus
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataProviderAndroidTest {

    private val contentResolver: ContentResolver = mockk()
    private val packageManager: PackageManager = mockk()
    private val devicePolicyManager: DevicePolicyManager = mockk()

    private val provider = DataProviderAndroid(contentResolver, packageManager, devicePolicyManager)

    @Before
    fun setUp() {
        mockkStatic(Settings.Secure::class)
        mockkStatic(Uri::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
        unmockkStatic(Uri::class)
    }

    private fun stubDefaults() {
        every { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) } returns "abc123"
        every { devicePolicyManager.storageEncryptionStatus } returns DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE
        val uri: Uri = mockk()
        every { Uri.parse(any()) } returns uri
        every { contentResolver.query(uri, null, null, any(), null) } returns null
    }

    @Test
    fun `parseSelinuxStatus maps 1 to Enforcing`() {
        assertEquals("Enforcing", DataProviderAndroid.parseSelinuxStatus("1"))
    }

    @Test
    fun `parseSelinuxStatus maps 0 to Permissive`() {
        assertEquals("Permissive", DataProviderAndroid.parseSelinuxStatus("0"))
    }

    @Test
    fun `parseSelinuxStatus trims whitespace before matching`() {
        assertEquals("Enforcing", DataProviderAndroid.parseSelinuxStatus("1\n"))
        assertEquals("Permissive", DataProviderAndroid.parseSelinuxStatus(" 0 "))
    }

    @Test
    fun `parseSelinuxStatus falls back to Unknown for null or unexpected values`() {
        assertEquals("Unknown", DataProviderAndroid.parseSelinuxStatus(null))
        assertEquals("Unknown", DataProviderAndroid.parseSelinuxStatus(""))
        assertEquals("Unknown", DataProviderAndroid.parseSelinuxStatus("garbage"))
    }

    @Test
    fun `androidId reflects Settings Secure ANDROID_ID`() {
        stubDefaults()
        assertEquals("abc123", provider.getAndroidData().androidId)
    }

    @Test
    fun `androidId is null when Settings Secure returns null`() {
        stubDefaults()
        every { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) } returns null
        assertNull(provider.getAndroidData().androidId)
    }

    @Test
    fun `encryptionStatus maps DevicePolicyManager status to ACTIVE`() {
        stubDefaults()
        assertEquals(EncryptionStatus.ACTIVE, provider.getAndroidData().encryptionStatus)
    }

    @Test
    fun `encryptionStatus is null when DevicePolicyManager throws`() {
        stubDefaults()
        every { devicePolicyManager.storageEncryptionStatus } throws SecurityException("no permission")
        assertNull(provider.getAndroidData().encryptionStatus)
    }

    @Test
    fun `hasStrongBox is false below API 28 regardless of PackageManager`() {
        stubDefaults()
        // Build.VERSION.SDK_INT defaults to 0 on JVM tests (see DataProviderGpuTest) — the
        // `SDK_INT >= 28` gate always takes the false branch here, matching real API-27-and-below behavior.
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE) } returns true
        assertEquals(false, provider.getAndroidData().hasStrongBox)
    }

    @Test
    fun `gsfAndroidId is null when contentResolver query returns null`() {
        stubDefaults()
        assertNull(provider.getAndroidData().gsfAndroidId)
    }

    @Test
    fun `gsfAndroidId is parsed as hex from the queried cursor`() {
        stubDefaults()
        val uri: Uri = mockk()
        every { Uri.parse(any()) } returns uri
        val cursor: Cursor = mockk()
        every { contentResolver.query(uri, null, null, any(), null) } returns cursor
        every { cursor.moveToFirst() } returns true
        every { cursor.getString(1) } returns "255"
        every { cursor.close() } just Runs

        assertEquals("ff", provider.getAndroidData().gsfAndroidId)
    }

    @Test
    fun `gsfAndroidId is null when query throws`() {
        stubDefaults()
        val uri: Uri = mockk()
        every { Uri.parse(any()) } returns uri
        every { contentResolver.query(uri, null, null, any(), null) } throws SecurityException("no permission")

        assertNull(provider.getAndroidData().gsfAndroidId)
    }
}
