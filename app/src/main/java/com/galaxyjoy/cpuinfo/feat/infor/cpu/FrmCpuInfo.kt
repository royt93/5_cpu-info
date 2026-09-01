package com.galaxyjoy.cpuinfo.feat.infor.cpu

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmCpuInfoBinding
import com.galaxyjoy.cpuinfo.domain.model.CpuData
import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessBar
import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessBottomSheet
import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessProvider
import com.galaxyjoy.cpuinfo.feat.canmydevice.CanMyDeviceBar
import com.galaxyjoy.cpuinfo.feat.canmydevice.CanMyDeviceBottomSheet
import com.galaxyjoy.cpuinfo.feat.canmydevice.CanMyDeviceProvider
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.feat.infor.base.copyToClipboardAndNotify
import com.galaxyjoy.cpuinfo.feat.infor.base.shrinkFabOnScroll
import com.galaxyjoy.cpuinfo.feat.truth.DeviceTruthBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Displays information about device CPU taken form /proc/cpuinfo file
 *
 * @author galaxyjoy
 */
@AndroidEntryPoint
class FrmCpuInfo : BaseFrm<FrmCpuInfoBinding>(R.layout.frm_cpu_info), AdtInfoItems.OnClickListener {

    private val viewModel: ViewModelCpuInfo by viewModels()

    private val displayItems = ListLiveData<CpuRow>()

    @Inject
    lateinit var clusterTopologyProvider: ClusterTopologyProvider

    @Inject
    lateinit var aiReadinessProvider: AiReadinessProvider

    @Inject
    lateinit var canMyDeviceProvider: CanMyDeviceProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rv.layoutManager = LinearLayoutManager(requireContext())
        (binding.rv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.rv.addItemDecoration(DividerItemDecoration(requireContext()))

        val adtCpuInfo = AdtCpuInfo(displayItems, onClickListener = this)
        displayItems.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtCpuInfo),
        )
        binding.rv.adapter = adtCpuInfo

        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            displayItems.replace(toDisplayItems(state.cpuData))
        }

        binding.fabDeviceTruth.setOnClickListener {
            DeviceTruthBottomSheet().show(childFragmentManager, DeviceTruthBottomSheet.TAG)
        }
        binding.rv.shrinkFabOnScroll(binding.fabDeviceTruth)

        binding.clusterTopologyCompose.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CpuInfoTheme {
                    Column {
                        ClusterTopologyScreen(clusters = clusterTopologyProvider.clusters())
                        AiReadinessBar(result = aiReadinessProvider.evaluate()) {
                            AiReadinessBottomSheet().show(childFragmentManager, AiReadinessBottomSheet.TAG)
                        }
                        CanMyDeviceBar(result = canMyDeviceProvider.evaluate()) {
                            CanMyDeviceBottomSheet().show(childFragmentManager, CanMyDeviceBottomSheet.TAG)
                        }
                    }
                }
            }
        }
    }

    private fun toDisplayItems(data: CpuData): List<CpuRow> {
        val rows = mutableListOf<CpuRow>()

        data.frequencies.forEachIndexed { i, frequency ->
            val currentDescription = if (frequency.current != -1L) {
                getString(R.string.cpu_current_frequency, i, frequency.current.toString())
            } else {
                getString(R.string.cpu_frequency_stopped, i)
            }
            val minDescription = if (frequency.min != -1L) {
                getString(R.string.cpu_frequency, frequency.min.toString())
            } else {
                ""
            }
            val maxDescription = if (frequency.max != -1L) {
                getString(R.string.cpu_frequency, frequency.max.toString())
            } else {
                ""
            }
            rows.add(
                CpuRow.FrequencyRow(
                    current = frequency.current,
                    max = frequency.max,
                    currentDescription = currentDescription,
                    minDescription = minDescription,
                    maxDescription = maxDescription,
                )
            )
        }

        rows.add(CpuRow.ValueRow(getString(R.string.cpu_soc_name), data.processorName))
        rows.add(CpuRow.ValueRow(getString(R.string.cpu_abi), data.abi))
        rows.add(CpuRow.ValueRow(getString(R.string.cpu_cores), data.coreNumber.toString()))
        rows.add(
            CpuRow.ValueRow(
                getString(R.string.cpu_has_neon),
                if (data.hasArmNeon) getString(R.string.yes) else getString(R.string.no),
            )
        )
        if (data.l1dCaches.isNotEmpty()) rows.add(CpuRow.ValueRow(getString(R.string.cpu_l1d), data.l1dCaches))
        if (data.l1iCaches.isNotEmpty()) rows.add(CpuRow.ValueRow(getString(R.string.cpu_l1i), data.l1iCaches))
        if (data.l2Caches.isNotEmpty()) rows.add(CpuRow.ValueRow(getString(R.string.cpu_l2), data.l2Caches))
        if (data.l3Caches.isNotEmpty()) rows.add(CpuRow.ValueRow(getString(R.string.cpu_l3), data.l3Caches))
        if (data.l4Caches.isNotEmpty()) rows.add(CpuRow.ValueRow(getString(R.string.cpu_l4), data.l4Caches))

        return rows
    }

    override fun onItemLongPressed(item: Pair<String, String>) {
        copyToClipboardAndNotify(binding.mainContainer, item.second)
    }
}
