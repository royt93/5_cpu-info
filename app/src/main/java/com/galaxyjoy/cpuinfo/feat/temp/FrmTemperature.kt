package com.galaxyjoy.cpuinfo.feat.temp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmTemperatureBinding
import com.galaxyjoy.cpuinfo.domain.model.TemperatureData
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.feat.temp.list.AdtTemperature
import com.galaxyjoy.cpuinfo.feat.temp.list.TemperatureItem
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FrmTemperature : BaseFrm<FrmTemperatureBinding>(
    R.layout.frm_temperature
) {

    private val viewModel: TemperatureVM by viewModels()

    @Inject
    lateinit var temperatureFormatter: TemperatureFormatter

    @Inject
    lateinit var temperatureIconProvider: TemperatureIconProvider

    private val displayItems = ListLiveData<TemperatureItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        setupRecycleView()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startTemperatureRefreshing()
    }

    override fun onStop() {
        viewModel.stopTemperatureRefreshing()
        super.onStop()
    }

    private fun setupRecycleView() {
        val adtTemperature = AdtTemperature(temperatureFormatter, displayItems)
        displayItems.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtTemperature)
        )
        viewModel.temperatureData.observe(viewLifecycleOwner) { state ->
            displayItems.replace(toDisplayItems(state))
        }
        binding.apply {
            tempRv.layoutManager = LinearLayoutManager(requireContext())
            tempRv.adapter = adtTemperature
            (tempRv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun toDisplayItems(state: TemperatureData.Available): List<TemperatureItem> = listOf(
        TemperatureItem(
            iconRes = temperatureIconProvider.getIcon(TemperatureIconProvider.Type.CPU),
            name = getString(R.string.cpu),
            temperature = state.cpuTemp,
        ),
        TemperatureItem(
            iconRes = temperatureIconProvider.getIcon(TemperatureIconProvider.Type.BATTERY),
            name = getString(R.string.battery),
            temperature = state.batteryTemp,
        ),
    )
}
