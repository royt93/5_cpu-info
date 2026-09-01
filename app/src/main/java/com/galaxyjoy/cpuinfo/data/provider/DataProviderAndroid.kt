package com.galaxyjoy.cpuinfo.data.provider

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import com.galaxyjoy.cpuinfo.domain.model.AndroidData
import com.galaxyjoy.cpuinfo.domain.model.EncryptionStatus
import com.galaxyjoy.cpuinfo.domain.model.SecurityProviderData
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.KeyStore
import java.security.Security
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.inject.Inject

/**
 * Android OS / security posture info — all static for the life of the process, read once by
 * [com.galaxyjoy.cpuinfo.domain.observable.ObservableAndroidData].
 */
class DataProviderAndroid @Inject constructor(
    private val contentResolver: ContentResolver,
    private val packageManager: PackageManager,
    private val devicePolicyManager: DevicePolicyManager,
) {

    // Build.* fields are Kotlin platform types (String!) — never actually null on a real device,
    // but `?: ""` guards against both a theoretical null and the JVM unit-test stub returning null.
    @SuppressLint("HardwareIds", "DEPRECATION")
    fun getAndroidData(): AndroidData = AndroidData(
        versionRelease = Build.VERSION.RELEASE ?: "",
        sdkInt = Build.VERSION.SDK_INT,
        codename = Build.VERSION.CODENAME ?: "",
        bootloader = Build.BOOTLOADER ?: "",
        brand = Build.BRAND ?: "",
        model = Build.MODEL ?: "",
        manufacturer = Build.MANUFACTURER ?: "",
        board = Build.BOARD ?: "",
        kernelVersion = System.getProperty("os.version") ?: "",
        serial = Build.SERIAL ?: "",
        androidId = getAndroidId(),
        isRooted = isDeviceRooted(),
        encryptionStatus = getEncryptionStatus(),
        securityProviders = Security.getProviders().map { SecurityProviderData(it.name, it.version.toString()) },
        gsfAndroidId = getGsfAndroidId(),
        hasStrongBox = getHasStrongBox(),
        securityPatch = Build.VERSION.SECURITY_PATCH ?: "",
        selinuxStatus = readSelinuxStatus(),
        hasHardwareKeystore = isHardwareBackedKeystoreAvailable(),
    )

    /** Keep in mind that from Android O it is unique per app. */
    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String? = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

    private fun getEncryptionStatus(): EncryptionStatus? = try {
        when (devicePolicyManager.storageEncryptionStatus) {
            DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> EncryptionStatus.UNSUPPORTED
            DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> EncryptionStatus.INACTIVE
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE -> EncryptionStatus.ACTIVE
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> EncryptionStatus.ACTIVE_PER_USER
            else -> EncryptionStatus.UNKNOWN
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Check if device is rooted. Source:
     * https://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device
     */
    private fun isDeviceRooted(): Boolean =
        checkRootMethod1() || checkRootMethod2() || checkRootMethod3()

    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su"
        )
        return paths.any { File(it).exists() }
    }

    // Root check #3: shells out to `which su` on the device's own /system/xbin (Android's busybox
    // path), not a host/CI process — pre-existing behavior ported as-is from VMAndroidInfo.
    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val br = BufferedReader(InputStreamReader(process.inputStream))
            return br.readLine() != null
        } catch (_: Throwable) {
            return false
        } finally {
            process?.destroy()
        }
    }

    private fun getGsfAndroidId(): String? {
        val uri = Uri.parse("content://com.google.android.gsf.gservices")
        val idKey = "android_id"
        val params = arrayOf(idKey)
        return try {
            contentResolver.query(uri, null, null, params, null)?.use {
                it.moveToFirst()
                java.lang.Long.toHexString(it.getString(1).toLong())
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getHasStrongBox(): Boolean = if (Build.VERSION.SDK_INT >= 28) {
        packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    } else {
        false
    }

    /**
     * SELinux enforcing status (F04). No public framework API exists for this (`android.os.SELinux`
     * is a hidden/restricted class, unreliable across API levels) — read the sysfs node directly,
     * same approach the codebase already uses for CPU temperature (see [com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider]).
     */
    private fun readSelinuxStatus(): String = try {
        val raw = File(SELINUX_ENFORCE_PATH).takeIf { it.canRead() }
            ?.bufferedReader()?.use { it.readLine() }
        parseSelinuxStatus(raw)
    } catch (_: Exception) {
        SELINUX_UNKNOWN
    }

    /**
     * Hardware-backed Keystore check (F04) — StrongBox (above) only covers the dedicated secure
     * chip; most devices instead back keys with a TEE, which [KeyInfo.isInsideSecureHardware]
     * detects. Generates and immediately deletes a throwaway key — no lasting side effect.
     */
    private fun isHardwareBackedKeystoreAvailable(): Boolean {
        return try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    HARDWARE_KEYSTORE_PROBE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            val secretKey = keyGenerator.generateKey()
            val factory = SecretKeyFactory.getInstance(secretKey.algorithm, "AndroidKeyStore")
            val keyInfo = factory.getKeySpec(secretKey, KeyInfo::class.java) as KeyInfo
            keyInfo.isInsideSecureHardware
        } catch (_: Exception) {
            false
        } finally {
            try {
                KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(HARDWARE_KEYSTORE_PROBE_ALIAS)
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val SELINUX_ENFORCE_PATH = "/sys/fs/selinux/enforce"
        private const val SELINUX_ENFORCING = "Enforcing"
        private const val SELINUX_PERMISSIVE = "Permissive"
        private const val SELINUX_UNKNOWN = "Unknown"
        private const val HARDWARE_KEYSTORE_PROBE_ALIAS = "cpuinfo_hw_keystore_probe"

        @VisibleForTesting
        fun parseSelinuxStatus(raw: String?): String = when (raw?.trim()) {
            "1" -> SELINUX_ENFORCING
            "0" -> SELINUX_PERMISSIVE
            else -> SELINUX_UNKNOWN
        }
    }
}
