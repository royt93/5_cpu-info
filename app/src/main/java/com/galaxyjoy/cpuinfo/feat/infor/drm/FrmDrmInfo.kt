package com.galaxyjoy.cpuinfo.feat.infor.drm

import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.domain.model.DrmData
import com.galaxyjoy.cpuinfo.domain.model.DrmSchemeData
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseRvFragment
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmDrmInfo : BaseRvFragment() {

    private val viewModel: VMDrmInfo by viewModels()

    private val displayItems = ListLiveData<Pair<String, String>>()

    override fun setupRecyclerViewAdapter() {
        val adtInfoItems = AdtInfoItems(
            displayItems,
            AdtInfoItems.LayoutType.HORIZONTAL_LAYOUT,
            onClickListener = this,
        )
        displayItems.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtInfoItems),
        )
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            displayItems.replace(toDisplayItems(state.drmData))
        }
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext()))
        recyclerView.adapter = adtInfoItems
    }

    private fun toDisplayItems(data: DrmData): List<Pair<String, String>> =
        data.schemes.flatMap { toDisplayRows(it) }

    private fun toDisplayRows(scheme: DrmSchemeData): List<Pair<String, String>> {
        val supportedLabel = getString(R.string.drm_supported, scheme.name)
        if (!scheme.supported) {
            return listOf(supportedLabel to getString(R.string.no))
        }

        val rows = mutableListOf<Pair<String, String>>()
        rows.add(supportedLabel to getString(R.string.yes))
        scheme.securityLevel?.let { rows.add(getString(R.string.drm_security_level, scheme.name) to it) }
        scheme.hdcpLevel?.let { rows.add(getString(R.string.drm_hdcp_level, scheme.name) to it) }
        scheme.maxHdcpLevel?.let { rows.add(getString(R.string.drm_max_hdcp, scheme.name) to it) }
        scheme.version?.let { rows.add(getString(R.string.drm_version, scheme.name) to it) }
        return rows
    }
}
