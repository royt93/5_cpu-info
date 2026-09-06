package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

enum class EncryptionStatus { UNSUPPORTED, INACTIVE, ACTIVE, ACTIVE_PER_USER, UNKNOWN }

@Keep
data class SecurityProviderData(val name: String, val version: String)

@Keep
data class ImeInfo(val label: String, val requestsInternet: Boolean)

@Keep
data class AndroidData(
    val versionRelease: String,
    val sdkInt: Int,
    val codename: String,
    val bootloader: String,
    val brand: String,
    val model: String,
    val manufacturer: String,
    val board: String,
    val kernelVersion: String,
    val serial: String,
    val androidId: String?,
    val isRooted: Boolean,
    /** Null when `DevicePolicyManager.storageEncryptionStatus` itself threw — row omitted, mirrors pre-migration VM. */
    val encryptionStatus: EncryptionStatus?,
    val securityProviders: List<SecurityProviderData>,
    val gsfAndroidId: String?,
    val hasStrongBox: Boolean,
    val securityPatch: String,
    val selinuxStatus: String,
    val hasHardwareKeystore: Boolean,
    /** E06 biometric hardware inventory — presence of the sensor class, independent of whether
     * the user has enrolled anything. */
    val hasFingerprintHardware: Boolean,
    val hasFaceHardware: Boolean,
    val hasIrisHardware: Boolean,
    /** E15 screen lock / enrollment posture. Null = `BiometricManager` unavailable (API<29) or the
     * typed `canAuthenticate(int)` overload unavailable (API<30) — "not supported on this Android
     * version", not "not enrolled". */
    val biometricStrongEnrolled: Boolean?,
    val biometricWeakEnrolled: Boolean?,
    val deviceCredentialSet: Boolean?,
    val isDeviceSecure: Boolean,
    /** E16 IME inventory. */
    val imeList: List<ImeInfo>,
)
