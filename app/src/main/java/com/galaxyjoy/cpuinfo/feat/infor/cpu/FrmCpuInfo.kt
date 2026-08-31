package com.galaxyjoy.cpuinfo.feat.infor.cpu

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmCpuInfoBinding
import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessBar
import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessBottomSheet
import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessProvider
import com.galaxyjoy.cpuinfo.feat.canmydevice.CanMyDeviceBar
import com.galaxyjoy.cpuinfo.feat.canmydevice.CanMyDeviceBottomSheet
import com.galaxyjoy.cpuinfo.feat.canmydevice.CanMyDeviceProvider
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.feat.truth.DeviceTruthBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Displays information about device CPU taken form /proc/cpuinfo file
 *
 * @author galaxyjoy
 */
@AndroidEntryPoint
class FrmCpuInfo : BaseFrm<FrmCpuInfoBinding>(R.layout.frm_cpu_info) {

    private val viewModel: ViewModelCpuInfo by viewModels()

    @Inject
    lateinit var clusterTopologyProvider: ClusterTopologyProvider

    @Inject
    lateinit var aiReadinessProvider: AiReadinessProvider

    @Inject
    lateinit var canMyDeviceProvider: CanMyDeviceProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val controller = CpuInfoEpoxyController(requireContext())
        binding.rv.adapter = controller.adapter
        viewModel.viewState.observe(viewLifecycleOwner) { controller.setData(it) }

        binding.fabDeviceTruth.setOnClickListener {
            DeviceTruthBottomSheet().show(childFragmentManager, DeviceTruthBottomSheet.TAG)
        }

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
}
