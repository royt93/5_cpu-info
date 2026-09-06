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
    /** E12 — packages holding `BIND_NOTIFICATION_LISTENER_SERVICE` / active accessibility
     * services. A non-empty list here isn't inherently malicious (screen readers, Bixby-style
     * assistants, and some legitimate utility apps use these) but it's the classic
     * stalkerware/spyware persistence vector, so surfacing it is the point. */
    val notificationListenerPackages: List<String>,
    val accessibilityServices: List<String>,
    /** E14 device-admin/MDM transparency — [DevicePolicyManager.getActiveAdmins]/
     * [DevicePolicyManager.getCameraDisabled]/[DevicePolicyManager.getScreenCaptureDisabled] all
     * return the aggregate policy across every active admin, callable by any app with no
     * permission (this is public info about the device's *current* lock-down state, not the
     * admin app's identity). */
    val hasActiveDeviceAdmin: Boolean,
    val isCameraDisabledByPolicy: Boolean,
    val isScreenCaptureDisabledByPolicy: Boolean,
    /** Raw `UserManager.getUserRestrictions()` keys currently set to `true` — untranslated
     * constant names (e.g. `no_debugging_features`); shown as-is rather than guessing a friendly
     * label for restrictions this app doesn't otherwise care about. */
    val restrictedActions: List<String>,
    /** E17 — true if the current default network is a VPN, or the device has a system-wide
     * default proxy configured. */
    val isVpnActive: Boolean,
    val isProxyActive: Boolean,
    /** E18 — this app's own resolved `usesCleartextTraffic`/`networkSecurityConfig` base setting,
     * read back from [android.content.pm.ApplicationInfo.flags]. Deprecated flag (API28+ docs
     * point at `networkSecurityConfig`), but the OS still populates it from whichever mechanism
     * is actually in effect, so it remains an accurate self-audit signal. */
    val allowsCleartextTraffic: Boolean,
)
