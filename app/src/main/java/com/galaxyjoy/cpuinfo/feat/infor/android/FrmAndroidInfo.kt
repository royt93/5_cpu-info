package com.galaxyjoy.cpuinfo.feat.infor.android

import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.domain.model.AndroidData
import com.galaxyjoy.cpuinfo.domain.model.EncryptionStatus
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseRvFragment
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmAndroidInfo : BaseRvFragment() {

    private val viewModel: VMAndroidInfo by viewModels()

    private val displayItems = ListLiveData<Pair<String, String>>()

    override fun setupRecyclerViewAdapter() {
        val adtInfoItems = AdtInfoItems(
            itemsObservableList = displayItems,
            layoutType = AdtInfoItems.LayoutType.HORIZONTAL_LAYOUT, onClickListener = this
        )
        displayItems.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtInfoItems)
        )
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            displayItems.replace(toDisplayItems(state.androidData))
        }
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext()))
        recyclerView.adapter = adtInfoItems
    }

    private fun toDisplayItems(data: AndroidData): List<Pair<String, String>> {
        val yes = getString(R.string.yes)
        val no = getString(R.string.no)
        val unknown = getString(R.string.unknown)
        fun yesNo(value: Boolean) = if (value) yes else no

        val items = mutableListOf<Pair<String, String>>()
        items.add(getString(R.string.version) to data.versionRelease)
        items.add("SDK" to "${data.sdkInt}")
        items.add(getString(R.string.codename) to data.codename)
        items.add("Bootloader" to data.bootloader)
        items.add(getString(R.string.brand) to data.brand)
        items.add(getString(R.string.model) to data.model)
        items.add(getString(R.string.manufacturer) to data.manufacturer)
        items.add(getString(R.string.board) to data.board)
        items.add("VM" to "ART")
        items.add("Kernel" to data.kernelVersion)
        items.add(getString(R.string.serial) to data.serial)

        data.androidId?.let { items.add("Android ID" to it) }
        data.gsfAndroidId?.let { items.add("Google Services Framework ID" to it) }

        items.add(getString(R.string.rooted) to yesNo(data.isRooted))

        data.encryptionStatus?.let { status ->
            items.add(getString(R.string.encrypted_storage) to encryptionStatusLabel(status))
        }

        items.add("StrongBox" to yesNo(data.hasStrongBox))
        items.add(getString(R.string.security_patch_level) to data.securityPatch)
        items.add(getString(R.string.selinux_status) to data.selinuxStatus)
        items.add(getString(R.string.hardware_keystore) to yesNo(data.hasHardwareKeystore))

        if (data.securityProviders.isNotEmpty()) {
            items.add(getString(R.string.security_providers) to "")
            data.securityProviders.forEach { items.add(it.name to it.version) }
        }

        fun yesNoOrUnsupported(value: Boolean?) = value?.let { yesNo(it) } ?: getString(R.string.not_supported)

        items.add(getString(R.string.biometric_section) to "")
        items.add(getString(R.string.biometric_fingerprint_hardware) to yesNo(data.hasFingerprintHardware))
        items.add(getString(R.string.biometric_face_hardware) to yesNo(data.hasFaceHardware))
        items.add(getString(R.string.biometric_iris_hardware) to yesNo(data.hasIrisHardware))
        items.add(getString(R.string.biometric_strong_enrolled) to yesNoOrUnsupported(data.biometricStrongEnrolled))
        items.add(getString(R.string.biometric_weak_enrolled) to yesNoOrUnsupported(data.biometricWeakEnrolled))
        items.add(getString(R.string.biometric_device_credential_set) to yesNoOrUnsupported(data.deviceCredentialSet))
        items.add(getString(R.string.screen_lock_set) to yesNo(data.isDeviceSecure))

        if (data.imeList.isNotEmpty()) {
            items.add(getString(R.string.ime_section) to "")
            data.imeList.forEach { ime -> items.add(ime.label to yesNo(ime.requestsInternet)) }
        }

        items.add(getString(R.string.privacy_security_section) to "")
        items.add(getString(R.string.device_admin_active) to yesNo(data.hasActiveDeviceAdmin))
        items.add(getString(R.string.camera_disabled_by_policy) to yesNo(data.isCameraDisabledByPolicy))
        items.add(getString(R.string.screen_capture_disabled_by_policy) to yesNo(data.isScreenCaptureDisabledByPolicy))
        items.add(getString(R.string.vpn_active) to yesNo(data.isVpnActive))
        items.add(getString(R.string.proxy_active) to yesNo(data.isProxyActive))
        items.add(getString(R.string.allows_cleartext_traffic) to yesNo(data.allowsCleartextTraffic))

        // Both settings keys store colon-separated `package/ServiceClassName` component strings —
        // split into (package, class) so the row renders as a normal label/value pair instead of
        // an empty-value row (AdtInfoItems treats an empty 2nd value as a section-header style).
        fun componentRow(flattened: String): Pair<String, String> {
            val slashIndex = flattened.indexOf('/')
            return if (slashIndex > 0) {
                flattened.substring(0, slashIndex) to flattened.substring(slashIndex + 1)
            } else {
                // A malformed entry (no '/') would otherwise pair with an empty 2nd value, which
                // AdtInfoItems renders as section-header styling instead of a normal row.
                flattened to unknown
            }
        }

        if (data.notificationListenerPackages.isNotEmpty()) {
            items.add(getString(R.string.notification_listener_section) to "")
            data.notificationListenerPackages.forEach { items.add(componentRow(it)) }
        }
        if (data.accessibilityServices.isNotEmpty()) {
            items.add(getString(R.string.accessibility_service_section) to "")
            data.accessibilityServices.forEach { items.add(componentRow(it)) }
        }
        if (data.restrictedActions.isNotEmpty()) {
            items.add(getString(R.string.restricted_actions_section) to "")
            data.restrictedActions.forEach { items.add(it to yes) }
        }

        return items
    }

    private fun encryptionStatusLabel(status: EncryptionStatus): String = when (status) {
        EncryptionStatus.UNSUPPORTED -> "UNSUPPORTED"
        EncryptionStatus.INACTIVE -> "INACTIVE"
        EncryptionStatus.ACTIVE -> "ACTIVE"
        EncryptionStatus.ACTIVE_PER_USER -> "ACTIVE_PER_USER"
        EncryptionStatus.UNKNOWN -> getString(R.string.unknown)
    }
}
