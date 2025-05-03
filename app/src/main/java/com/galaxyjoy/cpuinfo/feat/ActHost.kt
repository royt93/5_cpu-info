package com.galaxyjoy.cpuinfo.feat

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.galaxyjoy.cpuinfo.BaseActivity
import com.galaxyjoy.cpuinfo.BuildConfig
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.ActHostLayoutBinding
import com.galaxyjoy.cpuinfo.rateAppInApp
import com.galaxyjoy.cpuinfo.util.runOnApiAbove
import com.galaxyjoy.cpuinfo.util.setupEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

/**
 * Base activity which is a host for whole application.
 **/
@AndroidEntryPoint
class ActHost : BaseActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActHostLayoutBinding
    //TODO roy93~ admob banner
//    private var adView: MaxAdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppThemeBase)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.act_host_layout)
        setupEdgeToEdge()
//        enableEdgeToEdge()
        setupNavigation()
        setSupportActionBar(binding.toolbar)
        runOnApiAbove(Build.VERSION_CODES.M) {
            // Processes cannot be listed above M
            val menu = binding.bottomNavigation.menu
            menu.findItem(R.id.menuProcesses).isVisible = false
        }

        //TODO roy93~ admob
//        adView = this.createAdBanner(
//            logTag = ActHost::class.simpleName,
//            viewGroup = binding.flAd,
//            isAdaptiveBanner = true,
//        )
    }

    override fun onSupportNavigateUp() = navController.navigateUp()

    private fun setupNavigation() {
        navController =
            (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment)
                .navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            setToolbarTitleAndElevation(destination.label.toString())
//            Log.d("roy93~", "addOnDestinationChangedListener ${destination.label.toString()}")
            rateAppInApp(BuildConfig.DEBUG)
        }
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
        // binding.toolbar.isVisible = navController.currentDestination?.id != R.id.applications
        if (navController.currentDestination?.id == R.id.menuHardware) {
            binding.toolbar.elevation = 0f
        } else {
            binding.toolbar.elevation = resources.getDimension(R.dimen.elevation_height)
        }
    }

    override fun onDestroy() {
        //TODO roy93~ admob
//        with(binding) {
//            flAd.destroyAdBanner(adView)
//        }
        super.onDestroy()
    }
}
