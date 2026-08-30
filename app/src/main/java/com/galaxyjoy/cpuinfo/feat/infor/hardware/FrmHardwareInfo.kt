package com.galaxyjoy.cpuinfo.feat.infor.hardware

import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.domain.model.HardwareData
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseRvFragment
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmHardwareInfo : BaseRvFragment() {

    private val viewModel: VMHardwareInfo by viewModels()

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
            displayItems.replace(toDisplayItems(state.hardwareData))
        }
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext()))
        recyclerView.adapter = adtInfoItems
    }

    private fun toDisplayItems(data: HardwareData): List<Pair<String, String>> {
        val yes = getString(R.string.yes)
        val no = getString(R.string.no)
        fun yesNo(value: Boolean) = if (value) yes else no

        val items = mutableListOf<Pair<String, String>>()
        items.add(getString(R.string.wireless) to "")
        items.add(getString(R.string.bluetooth) to yesNo(data.hasBluetooth))
        items.add(getString(R.string.bluetooth_le) to yesNo(data.hasBluetoothLe))
        items.add("GPS" to yesNo(data.hasGps))
        items.add("NFC" to yesNo(data.hasNfc))
        items.add("NFC Card Emulation" to yesNo(data.hasNfcCardEmulation))
        items.add("Wi-Fi" to yesNo(data.hasWifi))
        if (data.hasWifi) {
            items.add("Wi-Fi Aware" to yesNo(data.hasWifiAware))
            items.add("Wi-Fi Direct" to yesNo(data.hasWifiDirect))
            items.add("Wi-Fi Passpoint" to yesNo(data.hasWifiPasspoint))
            items.add("Wi-Fi 5Ghz" to yesNo(data.hasWifi5Ghz))
            items.add("Wi-Fi P2P" to yesNo(data.hasWifiP2p))
        }
        data.bluetoothMac?.let { items.add(getString(R.string.bluetooth_mac) to it) }
        data.wifiMac?.let { items.add(getString(R.string.wifi_mac) to it) }
        items.add(getString(R.string.ir_emitter) to yesNo(data.hasIrEmitter))

        items.add("USB" to "")
        items.add("OTG" to yesNo(data.hasUsbHost))

        return items
    }
}
