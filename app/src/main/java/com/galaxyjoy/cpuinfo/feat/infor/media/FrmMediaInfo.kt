package com.galaxyjoy.cpuinfo.feat.infor.media

import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseRvFragment
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmMediaInfo : BaseRvFragment() {

    private val viewModel: VMMediaInfo by viewModels()

    override fun setupRecyclerViewAdapter() {
        val adtInfoItems = AdtInfoItems(
            viewModel.listLiveData,
            AdtInfoItems.LayoutType.HORIZONTAL_LAYOUT,
            onClickListener = this,
        )
        viewModel.listLiveData.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtInfoItems),
        )
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext()))
        recyclerView.adapter = adtInfoItems
    }
}
