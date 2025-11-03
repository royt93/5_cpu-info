package com.galaxyjoy.cpuinfo.feat.app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.DlgAppActionsBottomSheetBinding
import com.galaxyjoy.cpuinfo.databinding.FrmApplicationsBinding
import com.galaxyjoy.cpuinfo.ext.openBrowserPolicy
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.util.DividerItemDecoration
import com.galaxyjoy.cpuinfo.util.Utils
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveDataObserver
import com.galaxyjoy.cpuinfo.util.uninstallApp
import com.galaxyjoy.cpuinfo.util.wrapper.EventObserver
import dagger.hilt.android.AndroidEntryPoint
import moreApp
import rateApp
import shareApp
import java.io.File

@AndroidEntryPoint
class FrmApplications : BaseFrm<FrmApplicationsBinding>(
    R.layout.frm_applications
), AdtApp.ItemClickListener {

    private val viewModel: VMApplications by viewModels()

    private val uninstallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.refreshApplicationsList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerUninstallBroadcast()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.srl.setColorSchemeResources(
            R.color.accent,
            R.color.primaryDark
        )
        setupRecyclerView()
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_app, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menuActionSorting -> {
                        viewModel.changeAppsSorting()
                        true
                    }

                    R.id.menuActionRate -> {
                        activity?.let {
                            it.rateApp(it.packageName)
                        }
                        true
                    }

                    R.id.menuActionMore -> {
                        activity?.moreApp()
                        true
                    }

                    R.id.menuActionShare -> {
                        activity?.shareApp()
                        true
                    }

                    R.id.menuActionPolicy -> {
                        context?.openBrowserPolicy()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        initObservables()
    }

    private fun setupRecyclerView() {
        val adtApp = AdtApp(viewModel.applicationList, this)
        viewModel.applicationList.listStatusChangeNotificator.observe(
            viewLifecycleOwner,
            ListLiveDataObserver(adtApp)
        )

        binding.rv.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = adtApp
            addItemDecoration(DividerItemDecoration(requireContext()))
        }
    }

    /**
     * Register all fields from [VMApplications] which should be observed
     */
    private fun initObservables() {
        viewModel.shouldStartStorageServiceEvent.observe(viewLifecycleOwner, EventObserver {
            ServiceStorageUsage.startService(requireContext(), viewModel.applicationList)
        })
    }

    /**
     * Register broadcast receiver for uninstalling apps
     */
    private fun registerUninstallBroadcast() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addDataScheme("package")
        requireActivity().registerReceiver(uninstallReceiver, intentFilter)
    }


    /**
     * Try to open clicked app. In case of error show [Snackbar].
     */
    override fun appOpenClicked(position: Int) {
        val appInfo = viewModel.applicationList[position]
        // Block self opening
        if (appInfo.packageName == requireContext().packageName) {
            Snackbar.make(
                binding.mainContainer, getString(R.string.cpu_open),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val intent = requireContext().packageManager.getLaunchIntentForPackage(appInfo.packageName)
        if (intent != null) {
            try {
                startActivity(intent)
            } catch (_: Exception) {
                Snackbar.make(
                    binding.mainContainer, getString(R.string.app_open),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        } else {
            Snackbar.make(
                binding.mainContainer, getString(R.string.app_open),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Open settings activity for selected app
     */
    override fun appSettingsClicked(position: Int) {
        val appInfo = viewModel.applicationList[position]
        val uri = Uri.fromParts("package", appInfo.packageName, null)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        startActivity(intent)
    }

    /**
     * Try to uninstall selected app
     */
    override fun appUninstallClicked(position: Int) {
        val appInfo = viewModel.applicationList[position]
        if (appInfo.packageName == requireContext().packageName) {
            Snackbar.make(
                binding.mainContainer, getString(R.string.cpu_uninstall),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        requireActivity().uninstallApp(appInfo.packageName)
    }

    /**
     * Open dialog with native lib list and open google if user taps on it
     */
    override fun appNativeLibsClicked(nativeDir: String) {
        showNativeListDialog(nativeDir)
    }

    /**
     * Create dialog with native libraries list
     */
    @SuppressLint("InflateParams")
    private fun showNativeListDialog(nativeLibsDir: String) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = LayoutInflater.from(context)
        val dialogLayout = inflater.inflate(R.layout.dlg_native_libs, null)
        val nativeDirFile = File(nativeLibsDir)
        val libs = nativeDirFile.listFiles()?.map { it.name } ?: emptyList()

        val listView: ListView = dialogLayout.findViewById(R.id.dialogLv)
        val arrayAdapter = ArrayAdapter(
            requireContext(), R.layout.vi_item_native_libs,
            R.id.nativeNameTv, libs
        )
        listView.adapter = arrayAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Utils.searchInGoogle(requireContext(), libs[position])
        }
        builder.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.cancel()
        }
        builder.setView(dialogLayout)
        val alert = builder.create()
        alert.show()
    }

    /**
     * Show bottom sheet with app actions when long clicked
     */
    override fun appLongClicked(position: Int) {
        val appInfo = viewModel.applicationList[position]
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val binding = DlgAppActionsBottomSheetBinding.inflate(layoutInflater)

        binding.tvAppName.text = appInfo.name

        binding.btnOpen.setOnClickListener {
            bottomSheetDialog.dismiss()
            appOpenClicked(position)
        }

        binding.btnPlayStore.setOnClickListener {
            bottomSheetDialog.dismiss()
            openInPlayStore(appInfo.packageName)
        }

        binding.btnSettings.setOnClickListener {
            bottomSheetDialog.dismiss()
            appSettingsClicked(position)
        }

        binding.btnUninstall.setOnClickListener {
            bottomSheetDialog.dismiss()
            appUninstallClicked(position)
        }

        bottomSheetDialog.setContentView(binding.root)
        bottomSheetDialog.show()
    }

    /**
     * Open app in Play Store
     */
    private fun openInPlayStore(packageName: String) {
        android.util.Log.d("roy93~", "Opening Play Store for package: $packageName")
        try {
            // Try to open Play Store app
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            startActivity(intent)
            android.util.Log.d("roy93~", "Play Store app opened successfully")
        } catch (e: Exception) {
            android.util.Log.e("roy93~", "Failed to open Play Store app, trying browser", e)
            // If Play Store app is not available, open in browser
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                startActivity(intent)
                android.util.Log.d("roy93~", "Play Store browser opened successfully")
            } catch (e: Exception) {
                android.util.Log.e("roy93~", "Failed to open Play Store in browser", e)
                Snackbar.make(
                    binding.mainContainer,
                    "Cannot open Play Store",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        requireActivity().unregisterReceiver(uninstallReceiver)
        super.onDestroy()
    }
}
