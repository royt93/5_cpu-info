package com.galaxyjoy.cpuinfo.feat

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.galaxyjoy.cpuinfo.BaseActivity
import com.galaxyjoy.cpuinfo.BuildConfig
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.data.local.UserPreferencesRepository
import com.galaxyjoy.cpuinfo.databinding.ActHostLayoutBinding
import com.galaxyjoy.cpuinfo.feat.setting.ExportFormatBottomSheet
import com.galaxyjoy.cpuinfo.feat.setting.LanguagePickerBottomSheet
import com.galaxyjoy.cpuinfo.rateAppInApp
import com.galaxyjoy.cpuinfo.util.LocaleManager
import com.galaxyjoy.cpuinfo.util.SystemInfoExporter
import com.galaxyjoy.cpuinfo.util.runOnApiAbove
import com.galaxyjoy.cpuinfo.util.setupEdgeToEdge
import com.roy.sdkadbmob.AdManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Base activity which is a host for whole application.
 **/
@AndroidEntryPoint
class ActHost : BaseActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActHostLayoutBinding

    @Inject
    lateinit var systemInfoExporter: SystemInfoExporter

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private var adView: View? = null // AdmobWrapper — giữ reference để lifecycle hoạt động đúng

    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            setToolbarTitleAndElevation(destination.label.toString())
            rateAppInApp(BuildConfig.DEBUG)
        }

    override fun onResume() {
        super.onResume()
        AdManager.bannerResume(adView)
    }

    override fun onPause() {
        AdManager.bannerPause(adView)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppThemeBase)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.act_host_layout)
        setupEdgeToEdge()
        setupNavigation()
        setSupportActionBar(binding.toolbar)
        runOnApiAbove(Build.VERSION_CODES.M) {
            val menu = binding.bottomNavigation.menu
            menu.findItem(R.id.menuProcesses).isVisible = false
        }

        // Banner Ad
        adView = AdManager.loadBanner(
            context   = this,
            container = binding.layoutAdBanner.bannerContainer,
            tvLabelAd = binding.layoutAdBanner.tvLabelAd,
        )

        // Preload Interstitial ngầm — sẵn sàng cho khi user trigger
        AdManager.loadInterstitial(this)

        registerExportFormatResult()
        registerLanguagePickResult()
        maybeShowFirstLaunchLanguagePicker()
    }

    private fun registerExportFormatResult() {
        supportFragmentManager.setFragmentResultListener(
            ExportFormatBottomSheet.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val name = bundle.getString(ExportFormatBottomSheet.ARG_FORMAT) ?: return@setFragmentResultListener
            val format = runCatching { SystemInfoExporter.Format.valueOf(name) }.getOrNull()
                ?: SystemInfoExporter.Format.TEXT
            lifecycleScope.launch { userPreferencesRepository.setExportFormat(format.name) }
            systemInfoExporter.exportSystemInfo(this, format)
        }
    }

    private fun registerLanguagePickResult() {
        supportFragmentManager.setFragmentResultListener(
            LanguagePickerBottomSheet.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val tag = bundle.getString(LanguagePickerBottomSheet.ARG_TAG) ?: return@setFragmentResultListener
            // No need to setLanguagePicked here — maybeShow already persists it before showing,
            // so flag survives any path (pick, swipe-dismiss, app kill).
            LocaleManager.apply(tag)
        }
    }

    private fun maybeShowFirstLaunchLanguagePicker() {
        lifecycleScope.launch {
            val picked = userPreferencesRepository.hasPickedLanguageFlow.first()
            if (picked) return@launch

            // Persist BEFORE show — guarantees we don't re-prompt even if user
            // swipes the sheet away, force-kills the app, or activity is recreated
            // by locale change mid-coroutine. Settings still offers a re-open.
            userPreferencesRepository.setLanguagePicked()

            val fm = supportFragmentManager
            if (fm.isStateSaved) return@launch
            if (fm.findFragmentByTag(LanguagePickerBottomSheet.TAG) != null) return@launch
            LanguagePickerBottomSheet().show(fm, LanguagePickerBottomSheet.TAG)
        }
    }

    override fun onSupportNavigateUp() = navController.navigateUp()

    private fun setupNavigation() {
        navController =
            (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment)
                .navController
        navController.addOnDestinationChangedListener(destinationChangedListener)
        binding.bottomNavigation.apply {
            setupWithNavController(navController)
            setOnItemReselectedListener {
                // Do nothing - TODO: scroll to top
            }
        }
    }

    /**
     * Set toolbar title and manage elevation in case of L+ devices and TabLayout
     */
    @SuppressLint("NewApi")
    private fun setToolbarTitleAndElevation(title: String) {
        binding.toolbar.title = title
        if (navController.currentDestination?.id == R.id.menuHardware) {
            binding.toolbar.elevation = 0f
        } else {
            binding.toolbar.elevation = resources.getDimension(R.dimen.elevation_height)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuActionShare -> {
                showExportFormatSheet()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showExportFormatSheet() {
        val fm = supportFragmentManager
        if (fm.isStateSaved) return
        if (fm.findFragmentByTag(ExportFormatBottomSheet.TAG) != null) return

        lifecycleScope.launch {
            val storedName = userPreferencesRepository.exportFormatFlow.first()
            val initial = runCatching { SystemInfoExporter.Format.valueOf(storedName.orEmpty()) }
                .getOrDefault(SystemInfoExporter.Format.TEXT)
            if (fm.isStateSaved) return@launch
            if (fm.findFragmentByTag(ExportFormatBottomSheet.TAG) != null) return@launch
            ExportFormatBottomSheet.newInstance(initial)
                .show(fm, ExportFormatBottomSheet.TAG)
        }
    }

    override fun onDestroy() {
        navController.removeOnDestinationChangedListener(destinationChangedListener)
        AdManager.bannerDestroy(adView)
        super.onDestroy()
    }
}
