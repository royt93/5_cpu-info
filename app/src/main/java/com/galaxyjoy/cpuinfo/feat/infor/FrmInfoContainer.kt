package com.galaxyjoy.cpuinfo.feat.infor

import android.os.Bundle
import android.view.View
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmInfoBinding
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoContainerState
import com.galaxyjoy.cpuinfo.util.isNightMode
import com.galaxyjoy.cpuinfo.util.resolveActionBarColor
import com.galaxyjoy.cpuinfo.util.resolveActionBarContentColor
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

        // XML's Widget.MaterialComponents.TabLayout.Colored reads static ?attr/colorPrimary (bg)
        // and ?attr/colorOnPrimary (tab text, hardcoded white) — override both with the resolved
        // dynamic-or-static colors so this tab bar (sits right below ActHost's toolbar) matches it
        // exactly, and text stays readable even when dynamic dark's primary is a light/pastel tone
        // (see resolveActionBarContentColor kdoc).
        val nightMode = requireContext().isNightMode()
        val contentColor = requireContext().resolveActionBarContentColor(nightMode)
        binding.tabs.setBackgroundColor(requireContext().resolveActionBarColor(nightMode))
        binding.tabs.setTabTextColors(
            androidx.core.graphics.ColorUtils.setAlphaComponent(contentColor, 179), // ~70%, unselected
            contentColor, // selected
        )
        binding.tabs.setSelectedTabIndicatorColor(contentColor)

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

