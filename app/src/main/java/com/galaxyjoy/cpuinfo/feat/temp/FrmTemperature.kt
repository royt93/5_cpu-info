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
import com.galaxyjoy.cpuinfo.util.isNightMode
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import com.galaxyjoy.cpuinfo.util.resolveAccentColor
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
        // XML's style="@style/TintedProgressBar" pins colorAccent to a static @color/accent
        // inside its own self-contained Theme.AppCompat.Dialog.Alert-derived style — that shadows
        // the Activity's real (DynamicColors-overlaid) theme entirely, so the global dynamic-color
        // initializer can't reach it (Material You widget audit).
        val ctx = requireContext()
        binding.pb.indeterminateTintList = android.content.res.ColorStateList.valueOf(
            ctx.resolveAccentColor(ctx.isNightMode()),
        )
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

    private fun toDisplayItems(state: TemperatureData.Available): List<TemperatureItem> {
        val items = mutableListOf(
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
        // E07 — every other thermal zone the kernel exposes, reusing the CPU icon since there's no
        // per-zone icon set (skin/GPU/modem/... aren't drawables this app ships).
        state.allZones.forEach { zone ->
            items.add(
                TemperatureItem(
                    iconRes = temperatureIconProvider.getIcon(TemperatureIconProvider.Type.CPU),
                    name = zone.zoneName,
                    temperature = zone.tempCelsius,
                ),
            )
        }
        return items
    }
}
