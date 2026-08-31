package com.galaxyjoy.cpuinfo.feat.infor

import android.os.Bundle
import android.view.View
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmInfoBinding
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoContainerState
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment which is base for all hardware and software information fragments
 *
 */
@AndroidEntryPoint
class FrmInfoContainer : BaseFrm<FrmInfoBinding>(R.layout.frm_info) {

    private var tabLayoutMediator: TabLayoutMediator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AdtInfoContainerState(this)
        binding.vp.adapter = adapter
        tabLayoutMediator = TabLayoutMediator(binding.tabs, binding.vp) { tab: TabLayout.Tab, position: Int ->
            tab.text = resources.getText(adapter.getTitleRes(position))
        }.also { it.attach() }
    }

    override fun onDestroyView() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        super.onDestroyView()
    }
}

