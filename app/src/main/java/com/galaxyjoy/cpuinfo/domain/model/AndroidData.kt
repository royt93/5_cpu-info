package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

enum class EncryptionStatus { UNSUPPORTED, INACTIVE, ACTIVE, ACTIVE_PER_USER, UNKNOWN }

@Keep
data class SecurityProviderData(val name: String, val version: String)

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
)
