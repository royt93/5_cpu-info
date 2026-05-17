package com.galaxyjoy.cpuinfo.feat

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.galaxyjoy.cpuinfo.BaseActivity
import com.galaxyjoy.cpuinfo.BuildConfig
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.data.local.UserPreferencesRepository
import com.galaxyjoy.cpuinfo.databinding.ActHostLayoutBinding
import com.galaxyjoy.cpuinfo.feat.setting.ExportFormatBottomSheet
import com.galaxyjoy.cpuinfo.feat.setting.LanguagePickerBottomSheet
import com.galaxyjoy.cpuinfo.feat.vip.ActVip
import com.galaxyjoy.cpuinfo.feat.vip.FVipManagement
import com.galaxyjoy.cpuinfo.rateAppInApp
import com.galaxyjoy.cpuinfo.util.LocaleManager
import com.galaxyjoy.cpuinfo.util.SystemInfoExporter
import com.galaxyjoy.cpuinfo.util.runOnApiAbove
import com.galaxyjoy.cpuinfo.util.setupEdgeToEdge
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
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

    private companion object {
        private const val TAG = "roy93~ActHost"
    }

    private lateinit var navController: NavController
    private lateinit var binding: ActHostLayoutBinding

    @Inject
    lateinit var systemInfoExporter: SystemInfoExporter

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private var menuMain: Menu? = null
    private var vipBadge: BadgeDrawable? = null
    private var vipActionView: View? = null
    private var vipIconView: ImageView? = null
    private var vipIconPulse: ObjectAnimator? = null
    private var vipIconWiggle: ObjectAnimator? = null
    private var vipIconTint: android.animation.ValueAnimator? = null

    /**
     * Reference banner view để có thể destroy khi user activate VIP mid-session.
     * Lib v1.1.5 auto-manage lifecycle (pause/resume/destroy theo Activity), nhưng KHÔNG
     * tự destroy khi VIP toggle — vì khi `loadBanner` được gọi user chưa VIP, view đã
     * instantiated. Phải manual destroy sau khi VIP active để banner mất ngay.
     */
    private var adView: View? = null

    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            setToolbarTitleAndElevation(destination.label.toString())
            rateAppInApp(BuildConfig.DEBUG)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppThemeBase)
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = DataBindingUtil.setContentView(this, R.layout.act_host_layout)
        setupEdgeToEdge()
        setupNavigation()
        setSupportActionBar(binding.toolbar)
        runOnApiAbove(Build.VERSION_CODES.M) {
            val menu = binding.bottomNavigation.menu
            menu.findItem(R.id.menuProcesses).isVisible = false
        }

        // Banner Ad — apply state ngay tuỳ VIP. Free → load. VIP → skip + hide container.
        applyVipBannerState()

        // Preload Interstitial ngầm — sẵn sàng cho khi user trigger. Lib tự check VIP.
        AdManager.loadInterstitial(this)
        // Preload Rewarded để nút "Watch ad → 3d" trong VIP screen có ad sẵn. Lib KHÔNG
        // tự auto-load rewarded (khác Banner/Interstitial). Phải preload thủ công.
        AdManager.loadRewarded(this)

        // Listen VIP state thay đổi từ FVipManagement (redeem / watch ad / revoke).
        supportFragmentManager.setFragmentResultListener(
            FVipManagement.KEY_VIP_CHANGED,
            this,
        ) { _, _ ->
            Log.d(TAG, "VIP state changed event → reapply banner + badge")
            applyVipBannerState()
            refreshVipBadgeAndPulse()
        }

        registerExportFormatResult()
        registerLanguagePickResult()
        maybeShowFirstLaunchLanguagePicker()
    }

    /**
     * Show/hide banner theo VIP state. Khi VIP → destroy adView + hide container.
     * Khi free → load lại nếu chưa có.
     */
    private fun applyVipBannerState() {
        val isVip = AdManager.isVipByKeyActive()
        Log.d(TAG, "applyVipBannerState: isVip=$isVip, adView=${adView != null}")
        if (isVip) {
            adView?.let { AdManager.bannerDestroy(it) }
            adView = null
            binding.layoutAdBanner.bannerContainer.removeAllViews()
            binding.flAd.visibility = View.GONE
        } else {
            binding.flAd.visibility = View.VISIBLE
            if (adView == null) {
                adView = AdManager.loadBanner(
                    context   = this,
                    container = binding.layoutAdBanner.bannerContainer,
                    tvLabelAd = binding.layoutAdBanner.tvLabelAd,
                )
                Log.d(TAG, "applyVipBannerState: loaded banner, adView=${adView != null}")
            }
        }
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
            LocaleManager.applyNoFlicker(this, tag)
        }
    }

    private fun maybeShowFirstLaunchLanguagePicker() {
        lifecycleScope.launch {
            val picked = userPreferencesRepository.hasPickedLanguageFlow.first()
            if (picked) return@launch

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
        menuMain = menu
        val vipItem = menu.findItem(R.id.menuActionVip)
        vipActionView = vipItem?.actionView
        vipIconView = vipActionView?.findViewById(R.id.actionVipIcon)
        // actionLayout không trigger onOptionsItemSelected → wire click trực tiếp.
        vipActionView?.setOnClickListener { navigateToVip() }
        refreshVipBadgeAndPulse()
        return true
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume → refresh banner state + VIP badge")
        applyVipBannerState()
        refreshVipBadgeAndPulse()
    }

    private fun navigateToVip() {
        Log.d(TAG, "navigateToVip → start ActVip")
        ActVip.start(this)
    }

    /**
     * Khi VIP active → attach BadgeDrawable (vàng) anchor lên actionView + pulse icon.
     * Free → detach + stop pulse. Cache instance, tránh duplicate khi onResume spam.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun refreshVipBadgeAndPulse() {
        val anchor = vipActionView ?: return
        // BadgeUtils.attachBadgeDrawable() chain → BadgeDrawable.updateAnchorParentToNotClip()
        // gọi anchor.parent.setClipChildren(false). Nếu anchor chưa attach vào ActionMenuView
        // (hợp lệ khi onCreateOptionsMenu vừa fire, view tạo nhưng chưa add vào toolbar) → NPE.
        // Workaround: defer 1 frame tới khi parent ready.
        if (anchor.parent == null) {
            Log.d(TAG, "refreshVipBadgeAndPulse: anchor not yet attached → post retry")
            anchor.post { refreshVipBadgeAndPulse() }
            return
        }
        val active = AdManager.isVipByKeyActive()
        Log.d(TAG, "refreshVipBadgeAndPulse: active=$active, badge=${vipBadge != null}")
        val current = vipBadge
        if (active) {
            if (current == null) {
                val badge = BadgeDrawable.create(this).apply {
                    backgroundColor = getColor(R.color.vip_gold)
                    isVisible = true
                }
                vipBadge = badge
                BadgeUtils.attachBadgeDrawable(badge, anchor)
            }
            startVipIconPulse()
        } else {
            if (current != null) {
                BadgeUtils.detachBadgeDrawable(current, anchor)
                vipBadge = null
            }
            stopVipIconPulse()
        }
    }

    /**
     * Multi-animator cho crown icon khi VIP active:
     * - Pulse: scale 1.0 ↔ 1.15 (2.4s reverse infinite)
     * - Wiggle: rotation -6° ↔ +6° (lệch phase 1.6s reverse infinite)
     * - Tint shift: colorControlNormal ↔ vip_gold qua argb evaluator (3s reverse infinite)
     *
     * 3 animators chạy parallel với duration khác nhau → tạo hiệu ứng "living crown" không
     * monotone — pulse + nghiêng nhẹ + tint vàng nhấp nháy.
     */
    private fun startVipIconPulse() {
        val icon = vipIconView ?: return
        if (vipIconPulse?.isRunning == true) return  // tránh restart duplicate
        stopVipIconPulse()  // clean any stale animators (rotation/tint)

        vipIconPulse = ObjectAnimator.ofPropertyValuesHolder(
            icon,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.15f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.15f),
        ).apply {
            duration = 1200L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }

        vipIconWiggle = ObjectAnimator.ofFloat(icon, View.ROTATION, -6f, 6f).apply {
            duration = 800L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }

        val baseColor = resolveAttrColor(android.R.attr.colorControlNormal)
        val goldColor = getColor(R.color.vip_gold_dark)
        vipIconTint = android.animation.ValueAnimator.ofObject(
            android.animation.ArgbEvaluator(),
            baseColor,
            goldColor,
        ).apply {
            duration = 1500L
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            addUpdateListener { anim ->
                val color = anim.animatedValue as? Int ?: return@addUpdateListener
                vipIconView?.setColorFilter(color)
            }
            start()
        }
    }

    private fun resolveAttrColor(attr: Int): Int {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return if (tv.resourceId != 0) getColor(tv.resourceId) else tv.data
    }

    private fun stopVipIconPulse() {
        vipIconPulse?.cancel()
        vipIconPulse?.removeAllUpdateListeners()
        vipIconPulse?.removeAllListeners()
        vipIconPulse = null
        vipIconWiggle?.cancel()
        vipIconWiggle?.removeAllUpdateListeners()
        vipIconWiggle?.removeAllListeners()
        vipIconWiggle = null
        vipIconTint?.cancel()
        vipIconTint?.removeAllUpdateListeners()
        vipIconTint?.removeAllListeners()
        vipIconTint = null
        vipIconView?.scaleX = 1f
        vipIconView?.scaleY = 1f
        vipIconView?.rotation = 0f
        vipIconView?.clearColorFilter()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuActionShare -> {
                showExportFormatSheet()
                true
            }
            R.id.menuActionVip -> {
                navigateToVip()
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
        Log.d(TAG, "onDestroy")
        navController.removeOnDestinationChangedListener(destinationChangedListener)
        stopVipIconPulse()
        val anchor = vipActionView
        vipBadge?.let { badge ->
            if (anchor != null) BadgeUtils.detachBadgeDrawable(badge, anchor)
        }
        vipBadge = null
        vipActionView = null
        vipIconView = null
        menuMain = null
        adView?.let { AdManager.bannerDestroy(it) }
        adView = null
        super.onDestroy()
    }
}
