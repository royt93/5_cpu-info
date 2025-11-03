package com.galaxyjoy.cpuinfo.feat.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import com.galaxyjoy.cpuinfo.util.ApkExtractor
import com.galaxyjoy.cpuinfo.util.Utils
import com.galaxyjoy.cpuinfo.util.uninstallApp
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FrmNewApplications : Fragment() {

    private val viewModel: VMNewApplications by viewModels()

    @Inject
    lateinit var apkExtractor: ApkExtractor

    private var pendingExtractPackageName: String? = null

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingExtractPackageName?.let { packageName ->
                viewModel.onExtractApk(packageName)
                pendingExtractPackageName = null
            }
        } else {
            Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
            pendingExtractPackageName = null
        }
    }

    private val uninstallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.onRefreshApplications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerUninstallBroadcast()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CpuInfoTheme {
                    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
                    ApplicationsScreen(
                        uiState = uiState,
                        onAppClicked = viewModel::onApplicationClicked,
                        onRefreshApplications = viewModel::onRefreshApplications,
                        onSnackbarDismissed = viewModel::onSnackbarDismissed,
                        onCardExpanded = viewModel::onCardExpanded,
                        onCardCollapsed = viewModel::onCardCollapsed,
                        onAppUninstallClicked = viewModel::onAppUninstallClicked,
                        onAppSettingsClicked = viewModel::onAppSettingsClicked,
                        onNativeLibsClicked = viewModel::onNativeLibsClicked,
                        onSystemAppsSwitched = viewModel::onSystemAppsSwitched,
                        onOpenPlayStore = viewModel::onOpenPlayStore,
                        onExtractApk = ::onExtractApkWithPermissionCheck,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerObservers()
    }

    override fun onDestroy() {
        requireActivity().unregisterReceiver(uninstallReceiver)
        super.onDestroy()
    }

    private fun registerObservers() {
        viewModel.events.observe(viewLifecycleOwner, ::handleEvent)
    }

    @SuppressLint("InflateParams")
    private fun handleEvent(event: VMNewApplications.Event) {
        when (event) {
            is VMNewApplications.Event.OpenApp -> {
                val intent = requireContext().packageManager.getLaunchIntentForPackage(
                    event.packageName
                )
                if (intent != null) {
                    try {
                        startActivity(intent)
                    } catch (_: Exception) {
                        viewModel.onCannotOpenApp()
                    }
                } else {
                    viewModel.onCannotOpenApp()
                }
            }

            is VMNewApplications.Event.OpenAppSettings -> {
                val uri = Uri.fromParts("package", event.packageName, null)
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                    Timber.e("Can't open app settings")
                }
            }

            is VMNewApplications.Event.UninstallApp -> {
                requireActivity().uninstallApp(event.packageName)
            }

            is VMNewApplications.Event.OpenPlayStore -> {
                val uri = Uri.parse("market://details?id=${event.packageName}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                    // If Play Store not installed, open in browser
                    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=${event.packageName}")
                    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                    try {
                        startActivity(webIntent)
                    } catch (_: Exception) {
                        Timber.e("Can't open Play Store")
                    }
                }
            }

            is VMNewApplications.Event.ApkExtracted -> {
                Toast.makeText(
                    requireContext(),
                    "APK saved to Downloads/CPUInfo_APKs/${event.file.name}",
                    Toast.LENGTH_LONG
                ).show()

                // Show dialog to share APK
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("APK Extracted")
                    .setMessage("${event.appName} APK has been extracted successfully.")
                    .setPositiveButton("Share") { dialog, _ ->
                        apkExtractor.shareApk(event.file)
                        dialog.dismiss()
                    }
                    .setNegativeButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

            is VMNewApplications.Event.ShowNativeLibraries -> {
                val dialogLayout = LayoutInflater.from(context)
                    .inflate(R.layout.dlg_native_libs, null)
                val arrayAdapter = ArrayAdapter(
                    requireContext(),
                    R.layout.vi_item_native_libs,
                    R.id.nativeNameTv,
                    event.nativeLibs,
                )
                dialogLayout.findViewById<ListView>(R.id.dialogLv).apply {
                    adapter = arrayAdapter
                    onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                        Utils.searchInGoogle(requireContext(), event.nativeLibs[position])
                    }
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.cancel() }
                    .setView(dialogLayout)
                    .create()
                    .show()
            }
        }
    }

    private fun onExtractApkWithPermissionCheck(packageName: String) {
        // Android 10+ doesn't need storage permission for Downloads folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewModel.onExtractApk(packageName)
            return
        }

        // Android < 10 needs WRITE_EXTERNAL_STORAGE
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                viewModel.onExtractApk(packageName)
            }
            else -> {
                // Request permission
                pendingExtractPackageName = packageName
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun registerUninstallBroadcast() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addDataScheme("package")
        requireActivity().registerReceiver(uninstallReceiver, intentFilter)
    }
}
