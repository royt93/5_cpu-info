package com.galaxyjoy.cpuinfo.feat.infor.storage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.domain.model.StorageData
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseRvFragment
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrmStorageInfo : BaseRvFragment() {

    private var receiverRegistered = false

    private val handler = Handler(Looper.getMainLooper())
    private val mountedReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ viewModel.refreshSdCard() }, 2000)
        }
    }

    private val viewModel: StorageInfoViewModel by viewModels()

    private val displayItems = ListLiveData<StorageItem>()

    override fun onResume() {
        super.onResume()

        // Register events connected with inserting SD card
        if (!receiverRegistered) {
            receiverRegistered = true
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            filter.addAction(Intent.ACTION_MEDIA_CHECKING)
            filter.addAction(Intent.ACTION_MEDIA_EJECT)
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED)
            filter.addAction(Intent.ACTION_MEDIA_NOFS)
            filter.addAction(Intent.ACTION_MEDIA_REMOVED)
            filter.addAction(Intent.ACTION_MEDIA_SHARED)
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE)
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            filter.addDataScheme("file")
            requireActivity().registerReceiver(mountedReceiver, filter)
        }
    }

    override fun onPause() {
        if (receiverRegistered) {
            receiverRegistered = false
            requireActivity().unregisterReceiver(mountedReceiver)
            handler.removeCallbacksAndMessages(null)
        }

        super.onPause()
    }

    override fun setupRecyclerViewAdapter() {
        val adtStorage = AdtStorage(displayItems)
        displayItems.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtStorage)
        )
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            displayItems.replace(toDisplayItems(state.storageData))
        }
        recyclerView.adapter = adtStorage
    }

    private fun toDisplayItems(data: StorageData): List<StorageItem> {
        val items = mutableListOf<StorageItem>()
        items.add(
            StorageItem(getString(R.string.internal), R.drawable.ic_root, data.internal.totalBytes, data.internal.usedBytes)
        )
        data.external?.let {
            items.add(StorageItem(getString(R.string.external), R.drawable.ic_folder, it.totalBytes, it.usedBytes))
        }
        data.sdCard?.let {
            items.add(StorageItem(getString(R.string.external), R.drawable.ic_sdcard, it.totalBytes, it.usedBytes))
        }
        return items
    }
}
