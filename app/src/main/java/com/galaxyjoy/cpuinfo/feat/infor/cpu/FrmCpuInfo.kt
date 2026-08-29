package com.galaxyjoy.cpuinfo.feat.infor.cpu

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmCpuInfoBinding
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.feat.truth.DeviceTruthBottomSheet
import dagger.hilt.android.AndroidEntryPoint

/**
 * Displays information about device CPU taken form /proc/cpuinfo file
 *
 * @author galaxyjoy
 */
@AndroidEntryPoint
class FrmCpuInfo : BaseFrm<FrmCpuInfoBinding>(R.layout.frm_cpu_info) {

    private val viewModel: ViewModelCpuInfo by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val controller = CpuInfoEpoxyController(requireContext())
        binding.rv.adapter = controller.adapter
        viewModel.viewState.observe(viewLifecycleOwner) { controller.setData(it) }

        binding.fabDeviceTruth.setOnClickListener {
            DeviceTruthBottomSheet().show(childFragmentManager, DeviceTruthBottomSheet.TAG)
        }
    }
}
