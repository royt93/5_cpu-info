package com.galaxyjoy.cpuinfo.feat.infor.android

import android.annotation.SuppressLint
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.KeyStore
import java.security.Security
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.inject.Inject

/**
 * ViewModel for Android OS info. It is simple container for a lot of static data from Android so
 * it won't required any public methods.
 *
 */
@HiltViewModel
class VMAndroidInfo @Inject constructor(
    application: Application,
    private val resources: Resources,
    private val contentResolver: ContentResolver,
    private val devicePolicyManager: DevicePolicyManager
) : AndroidViewModel(application) {

    val listLiveData = ListLiveData<Pair<String, String>>()

    init {
        getData()
    }

    /**
     * Get all data connected with Android OS
     */
    private fun getData() {
        if (listLiveData.isNotEmpty()) {
            return
        }
        getBuildData()
        getAndroidIdData()
        getGsfAndroidId()
        getRootData()
        getDeviceEncryptionStatus()
        getStrongBoxData()
        getSecurityPatchData()
        getSelinuxData()
        getHardwareKeystoreData()
        getSecurityData()
    }

    /**
     * Retrieve data from static Build class and system property "java.vm.version"
     */
    @SuppressLint("HardwareIds")
    private fun getBuildData() {
        listLiveData.add(Pair(resources.getString(R.string.version), Build.VERSION.RELEASE))
        listLiveData.add(Pair("SDK", Build.VERSION.SDK_INT.toString()))
        listLiveData.add(Pair(resources.getString(R.string.codename), Build.VERSION.CODENAME))
        listLiveData.add(Pair("Bootloader", Build.BOOTLOADER))
        listLiveData.add(Pair(resources.getString(R.string.brand), Build.BRAND))
        listLiveData.add(Pair(resources.getString(R.string.model), Build.MODEL))
        listLiveData.add(Pair(resources.getString(R.string.manufacturer), Build.MANUFACTURER))
        listLiveData.add(Pair(resources.getString(R.string.board), Build.BOARD))
        // minSdk=24 → runtime is always ART (Dalvik was removed in Android 5.0/API 21).
        listLiveData.add(Pair("VM", "ART"))
        listLiveData.add(Pair("Kernel", System.getProperty("os.version") ?: ""))
        @Suppress("DEPRECATION")
        listLiveData.add(Pair(resources.getString(R.string.serial), Build.SERIAL))
    }

    /**
     * Get AndroidID. Keep in mind that from Android O it is unique per app.
     */
    @SuppressLint("HardwareIds")
    private fun getAndroidIdData() {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        if (androidId != null) {
            listLiveData.add(Pair("Android ID", androidId))
        }
    }

    /**
     * Add information if device is rooted
     */
    private fun getRootData() {
        val isRootedStr = if (isDeviceRooted()) resources.getString(R.string.yes) else
            resources.getString(R.string.no)
        listLiveData.add(Pair(resources.getString(R.string.rooted), isRootedStr))
    }

    /**
     * Add information about device encrypted storage status
     */
    private fun getDeviceEncryptionStatus() {
        try {
            val statusText = when (devicePolicyManager.storageEncryptionStatus) {
                DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> ENCRYPTION_STATUS_UNSUPPORTED
                DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> ENCRYPTION_STATUS_INACTIVE
//                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING -> ENCRYPTION_STATUS_ACTIVATING
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE -> ENCRYPTION_STATUS_ACTIVE
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER ->
                    ENCRYPTION_STATUS_ACTIVE_PER_USER

                else -> resources.getString(R.string.unknown)
            }
            listLiveData.add(Pair(resources.getString(R.string.encrypted_storage), statusText))
        } catch (_: Exception) {
        }
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

    /**
     * Get information about security providers
     */
    private fun getSecurityData() {
        val securityProviders = Security.getProviders().map { Pair(it.name, it.version.toString()) }
        if (securityProviders.isNotEmpty()) {
            listLiveData.add(Pair(resources.getString(R.string.security_providers), ""))
            listLiveData.addAll(securityProviders)
        }
    }

    private fun getGsfAndroidId() {
        val uri = Uri.parse("content://com.google.android.gsf.gservices")
        val idKey = "android_id"
        val params = arrayOf(idKey)
        try {
            getApplication<Application>().contentResolver.query(
                uri, null, null, params, null
            )?.use {
                it.moveToFirst()
                val hexId = java.lang.Long.toHexString(it.getString(1).toLong())
                listLiveData.add(Pair("Google Services Framework ID", hexId))
            }
        } catch (_: Exception) {
            // Do nothing
        }
    }

    private fun getStrongBoxData() {
        val hasStrongBox = if (Build.VERSION.SDK_INT >= 28) {
            getApplication<Application>().packageManager
                .hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } else {
            false
        }
        listLiveData.add(Pair("StrongBox", getYesNoString(hasStrongBox)))
    }

    private fun getYesNoString(value: Boolean) = if (value) {
        resources.getString(R.string.yes)
    } else {
        resources.getString(R.string.no)
    }

    /**
     * Security patch level (F04) — public API since 23, always present at minSdk 24.
     */
    private fun getSecurityPatchData() {
        listLiveData.add(Pair(resources.getString(R.string.security_patch_level), Build.VERSION.SECURITY_PATCH))
    }

    /**
     * SELinux enforcing status (F04). No public framework API exists for this (`android.os.SELinux`
     * is a hidden/restricted class, unreliable across API levels) — read the sysfs node directly,
     * same approach the codebase already uses for CPU temperature (see [com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider]).
     */
    private fun getSelinuxData() {
        listLiveData.add(Pair(resources.getString(R.string.selinux_status), readSelinuxStatus()))
    }

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
    private fun getHardwareKeystoreData() {
        listLiveData.add(Pair(resources.getString(R.string.hardware_keystore), getYesNoString(isHardwareBackedKeystoreAvailable())))
    }

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
        private const val ENCRYPTION_STATUS_UNSUPPORTED = "UNSUPPORTED"
        private const val ENCRYPTION_STATUS_INACTIVE = "INACTIVE"
        private const val ENCRYPTION_STATUS_ACTIVATING = "ACTIVATING"
        private const val ENCRYPTION_STATUS_ACTIVE = "ACTIVE"
        private const val ENCRYPTION_STATUS_ACTIVE_PER_USER = "ACTIVE_PER_USER"

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
