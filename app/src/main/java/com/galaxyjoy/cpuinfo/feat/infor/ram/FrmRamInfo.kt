package com.galaxyjoy.cpuinfo.feat.infor.ram

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmRecyclerViewBinding
import com.galaxyjoy.cpuinfo.domain.model.RamData
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.Utils
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import com.galaxyjoy.cpuinfo.util.runOnApiBelow
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmRamInfo : BaseFrm<FrmRecyclerViewBinding>(R.layout.frm_recycler_view),
    AdtInfoItems.OnClickListener {

    private val viewModel: VMRamInfo by viewModels()

    private val displayItems = ListLiveData<Pair<String, String>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rv.layoutManager = LinearLayoutManager(requireContext())
        (binding.rv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.rv.addItemDecoration(DividerItemDecoration(requireContext()))

        val adtInfoItems = AdtInfoItems(
            itemsObservableList = displayItems,
            layoutType = AdtInfoItems.LayoutType.HORIZONTAL_LAYOUT,
            onClickListener = this,
        )
        displayItems.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtInfoItems),
        )
        binding.rv.adapter = adtInfoItems

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                runOnApiBelow(24) {
                    menuInflater.inflate(R.menu.menu_ram, menu)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menuActionGc -> {
                        viewModel.onClearRamClicked()
                        Snackbar.make(
                            binding.mainContainer, getString(R.string.running_gc),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            displayItems.replace(toDisplayItems(state.ramData))
        }
    }

    private fun toDisplayItems(data: RamData): List<Pair<String, String>> = listOf(
        getString(R.string.total_memory) to Utils.convertBytesToMega(data.total),
        getString(R.string.available_memory) to "${Utils.convertBytesToMega(data.available)} (${data.availablePercentage}%)",
        getString(R.string.threshold) to Utils.convertBytesToMega(data.threshold),
    )

    override fun onItemLongPressed(item: Pair<String, String>) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.app_name), item.second)
        clipboard.setPrimaryClip(clip)
        Snackbar.make(binding.mainContainer, R.string.text_copied, Snackbar.LENGTH_SHORT).show()
    }
}
