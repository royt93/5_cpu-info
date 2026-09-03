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

        pendingTabPosition?.let { pos ->
            binding.vp.setCurrentItem(pos, false)
            pendingTabPosition = null
        }
    }

    override fun onDestroyView() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        super.onDestroyView()
    }

    companion object {
        /**
         * One-shot tab position set by [com.galaxyjoy.cpuinfo.feat.ActHost] (from the U21 "last
         * benchmark result" widget's tap intent) before `setContentView` inflates the
         * `NavHostFragment` — this fragment's `onViewCreated` runs synchronously during that
         * inflation (app:navGraph on the layout's `<fragment>` tag), before `ActHost.onCreate` can
         * reach any code after `setContentView`. Consumed (nulled) immediately above so a later
         * plain bottom-nav reselect doesn't jump back to the same old tab.
         */
        var pendingTabPosition: Int? = null
    }
}

