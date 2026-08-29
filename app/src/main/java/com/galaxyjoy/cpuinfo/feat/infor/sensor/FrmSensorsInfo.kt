package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmSensorsInfoBinding
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmSensorsInfo :
    BaseFrm<FrmSensorsInfoBinding>(R.layout.frm_sensors_info),
    AdtInfoItems.OnClickListener {

    private val viewModel: VMSensorsInfo by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rv.layoutManager = LinearLayoutManager(requireContext())
        (binding.rv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.rv.addItemDecoration(DividerItemDecoration(requireContext()))

        val adtInfoItems = AdtInfoItems(
            viewModel.listLiveData,
            AdtInfoItems.LayoutType.VERTICAL_LAYOUT,
            onClickListener = this,
        )
        viewModel.listLiveData.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtInfoItems),
        )
        binding.rv.adapter = adtInfoItems

        binding.fabSensorTest.setOnClickListener {
            SensorTestBottomSheet().show(childFragmentManager, SensorTestBottomSheet.TAG)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startProvidingData()
    }

    override fun onStop() {
        viewModel.stopProvidingData()
        super.onStop()
    }

    override fun onDestroyView() {
        binding.rv.adapter = null
        super.onDestroyView()
    }

    override fun onItemLongPressed(item: Pair<String, String>) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(requireContext().getString(R.string.app_name), item.second)
        clipboard.setPrimaryClip(clip)
        Snackbar.make(binding.mainContainer, R.string.text_copied, Snackbar.LENGTH_SHORT).show()
    }
}
