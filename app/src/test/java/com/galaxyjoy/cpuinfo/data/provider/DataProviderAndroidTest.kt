package com.galaxyjoy.cpuinfo.data.provider

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.Uri
import android.os.UserManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
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

    private val appContext: Context = mockk(relaxed = true)
    private val contentResolver: ContentResolver = mockk()
    private val packageManager: PackageManager = mockk(relaxed = true)
    private val devicePolicyManager: DevicePolicyManager = mockk(relaxed = true)
    private val keyguardManager: KeyguardManager = mockk(relaxed = true)
    private val inputMethodManager: InputMethodManager = mockk(relaxed = true)
    private val userManager: UserManager = mockk(relaxed = true)
    private val connectivityManager: ConnectivityManager = mockk(relaxed = true)

    private val provider = DataProviderAndroid(
        appContext, contentResolver, packageManager, devicePolicyManager,
        biometricManager = null, keyguardManager = keyguardManager, inputMethodManager = inputMethodManager,
        userManager = userManager, connectivityManager = connectivityManager,
    )

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
        every { Settings.Secure.getString(contentResolver, "enabled_notification_listeners") } returns null
        every { Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) } returns null
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
    fun `biometric enrollment fields are null when BiometricManager is unavailable`() {
        stubDefaults()
        val data = provider.getAndroidData()

        assertNull(data.biometricStrongEnrolled)
        assertNull(data.biometricWeakEnrolled)
        assertNull(data.deviceCredentialSet)
    }

    @Test
    fun `imeList is empty when getEnabledInputMethodList throws`() {
        stubDefaults()
        every { inputMethodManager.enabledInputMethodList } throws SecurityException("no permission")

        assertEquals(emptyList(), provider.getAndroidData().imeList)
    }

    @Test
    fun `gsfAndroidId is null when query throws`() {
        stubDefaults()
        val uri: Uri = mockk()
        every { Uri.parse(any()) } returns uri
        every { contentResolver.query(uri, null, null, any(), null) } throws SecurityException("no permission")

        assertNull(provider.getAndroidData().gsfAndroidId)
    }

    @Test
    fun `notificationListenerPackages parses colon-separated component list`() {
        stubDefaults()
        every { Settings.Secure.getString(contentResolver, "enabled_notification_listeners") } returns
            "com.a/com.a.Service:com.b/com.b.Service"

        assertEquals(
            listOf("com.a/com.a.Service", "com.b/com.b.Service"),
            provider.getAndroidData().notificationListenerPackages,
        )
    }

    @Test
    fun `notificationListenerPackages is empty when the setting is unset`() {
        stubDefaults()
        assertEquals(emptyList(), provider.getAndroidData().notificationListenerPackages)
    }

    @Test
    fun `accessibilityServices parses colon-separated component list`() {
        stubDefaults()
        every { Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) } returns
            "com.a11y/com.a11y.Service"

        assertEquals(listOf("com.a11y/com.a11y.Service"), provider.getAndroidData().accessibilityServices)
    }

    @Test
    fun `hasActiveDeviceAdmin is true when there is at least one active admin`() {
        stubDefaults()
        every { devicePolicyManager.activeAdmins } returns listOf(mockk())

        assertEquals(true, provider.getAndroidData().hasActiveDeviceAdmin)
    }

    @Test
    fun `hasActiveDeviceAdmin is false when there are no active admins`() {
        stubDefaults()
        every { devicePolicyManager.activeAdmins } returns emptyList()

        assertEquals(false, provider.getAndroidData().hasActiveDeviceAdmin)
    }

    @Test
    fun `hasActiveDeviceAdmin is false when DevicePolicyManager throws`() {
        stubDefaults()
        every { devicePolicyManager.activeAdmins } throws SecurityException("no permission")

        assertEquals(false, provider.getAndroidData().hasActiveDeviceAdmin)
    }

    @Test
    fun `camera and screen capture policy flags reflect DevicePolicyManager`() {
        stubDefaults()
        every { devicePolicyManager.getCameraDisabled(null) } returns true
        every { devicePolicyManager.getScreenCaptureDisabled(null) } returns true

        val data = provider.getAndroidData()
        assertEquals(true, data.isCameraDisabledByPolicy)
        assertEquals(true, data.isScreenCaptureDisabledByPolicy)
    }

    @Test
    fun `restrictedActions returns only keys whose restriction is true`() {
        stubDefaults()
        val restrictions: android.os.Bundle = mockk()
        every { restrictions.keySet() } returns setOf("no_debugging_features", "no_factory_reset")
        every { restrictions.getBoolean("no_debugging_features") } returns true
        every { restrictions.getBoolean("no_factory_reset") } returns false
        every { userManager.getUserRestrictions() } returns restrictions

        assertEquals(listOf("no_debugging_features"), provider.getAndroidData().restrictedActions)
    }

    @Test
    fun `isVpnActive is true when the active network has the VPN transport`() {
        stubDefaults()
        val network: android.net.Network = mockk()
        val capabilities: android.net.NetworkCapabilities = mockk()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) } returns true

        assertEquals(true, provider.getAndroidData().isVpnActive)
    }

    @Test
    fun `isVpnActive is false when there is no active network`() {
        stubDefaults()
        every { connectivityManager.activeNetwork } returns null

        assertEquals(false, provider.getAndroidData().isVpnActive)
    }

    @Test
    fun `isProxyActive is true when defaultProxy has a real host`() {
        stubDefaults()
        val proxy: android.net.ProxyInfo = mockk()
        every { proxy.host } returns "proxy.example.com"
        every { connectivityManager.defaultProxy } returns proxy

        assertEquals(true, provider.getAndroidData().isProxyActive)
    }

    @Test
    fun `isProxyActive is false when defaultProxy is null`() {
        stubDefaults()
        every { connectivityManager.defaultProxy } returns null

        assertEquals(false, provider.getAndroidData().isProxyActive)
    }

    @Test
    fun `isProxyActive is false when defaultProxy is non-null but has a blank host`() {
        // Regression: some Android versions/network configs return a non-null ProxyInfo with a
        // blank host even when no proxy is actually configured — a null-check alone false-positives.
        stubDefaults()
        val proxy: android.net.ProxyInfo = mockk()
        every { proxy.host } returns ""
        every { connectivityManager.defaultProxy } returns proxy

        assertEquals(false, provider.getAndroidData().isProxyActive)
    }

    @Test
    fun `allowsCleartextTraffic reads the FLAG_USES_CLEARTEXT_TRAFFIC bit`() {
        stubDefaults()
        every { appContext.packageName } returns "com.galaxyjoy.cpuinfo"
        val applicationInfo = android.content.pm.ApplicationInfo().apply {
            flags = android.content.pm.ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC
        }
        every { packageManager.getApplicationInfo("com.galaxyjoy.cpuinfo", 0) } returns applicationInfo

        assertEquals(true, provider.getAndroidData().allowsCleartextTraffic)
    }

    @Test
    fun `allowsCleartextTraffic is false when the flag is not set`() {
        stubDefaults()
        every { appContext.packageName } returns "com.galaxyjoy.cpuinfo"
        every { packageManager.getApplicationInfo("com.galaxyjoy.cpuinfo", 0) } returns android.content.pm.ApplicationInfo()

        assertEquals(false, provider.getAndroidData().allowsCleartextTraffic)
    }
}
