package com.galaxyjoy.cpuinfo.feat

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.galaxyjoy.cpuinfo.BaseActivity
import com.galaxyjoy.cpuinfo.BuildConfig
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.ActHostLayoutBinding
import com.galaxyjoy.cpuinfo.rateAppInApp
import com.galaxyjoy.cpuinfo.util.SystemInfoExporter
import com.galaxyjoy.cpuinfo.util.runOnApiAbove
import com.galaxyjoy.cpuinfo.util.setupEdgeToEdge
import com.roy.sdkadbmob.AdManager
import dagger.hilt.android.AndroidEntryPoint
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
                systemInfoExporter.exportSystemInfo(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        navController.removeOnDestinationChangedListener(destinationChangedListener)
        AdManager.bannerDestroy(adView)
        super.onDestroy()
    }
}
