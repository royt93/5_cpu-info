package com.galaxyjoy.cpuinfo.feat.infor.media

import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.domain.model.MediaData
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseRvFragment
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmMediaInfo : BaseRvFragment() {

    private val viewModel: VMMediaInfo by viewModels()

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
            displayItems.replace(toDisplayItems(state.mediaData))
        }
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext()))
        recyclerView.adapter = adtInfoItems
    }

    private fun toDisplayItems(data: MediaData): List<Pair<String, String>> {
        val items = mutableListOf<Pair<String, String>>()
        items.add(getString(R.string.media_total_codecs) to data.codecs.size.toString())
        items.add(getString(R.string.media_decoders) to data.codecs.count { !it.isEncoder }.toString())
        items.add(getString(R.string.media_encoders) to data.codecs.count { it.isEncoder }.toString())

        val encoderLabel = getString(R.string.media_role_encoder)
        val decoderLabel = getString(R.string.media_role_decoder)
        val hwLabel = getString(R.string.media_accel_hw)
        val swLabel = getString(R.string.media_accel_sw)

        data.codecs.forEach { codec ->
            val role = if (codec.isEncoder) encoderLabel else decoderLabel
            val accel = if (codec.isHardwareAccelerated) hwLabel else swLabel
            val types = codec.supportedTypes.joinToString(", ")
            val summary = getString(R.string.media_codec_summary, role, accel, types)
            items.add(codec.name to summary)
        }
        return items
    }
}
