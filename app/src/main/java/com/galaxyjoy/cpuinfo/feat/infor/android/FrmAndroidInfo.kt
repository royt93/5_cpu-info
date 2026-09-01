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
