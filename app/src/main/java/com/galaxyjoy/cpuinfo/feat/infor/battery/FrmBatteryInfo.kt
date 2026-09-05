package com.galaxyjoy.cpuinfo.feat.infor.battery

import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseRvFragment
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmBatteryInfo : BaseRvFragment() {

    private val viewModel: VMBatteryInfo by viewModels()

    private val displayItems = ListLiveData<Pair<String, String>>()

    override fun setupRecyclerViewAdapter() {
        val adtInfoItems = AdtInfoItems(
            displayItems,
            AdtInfoItems.LayoutType.HORIZONTAL_LAYOUT,
            onClickListener = this,
        )
        displayItems.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtInfoItems),
        )
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext()))
        recyclerView.adapter = adtInfoItems
        viewModel.rows.observe(viewLifecycleOwner) { displayItems.replace(it) }
    }
}
