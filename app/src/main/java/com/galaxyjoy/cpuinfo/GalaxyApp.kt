package com.galaxyjoy.cpuinfo

import android.app.Application
import android.util.Log
import com.galaxyjoy.cpuinfo.appinitializers.InitializersApp
import com.galaxyjoy.cpuinfo.common.const.AdKeys
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyLimits
import com.roy.sdkadbmob.AdSdkConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GalaxyApp : Application() {

    private companion object {
        private const val TAG = "roy93~GalaxyApp"
    }

    @Inject
    lateinit var initializers: InitializersApp

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate → setupAd + initializers")
        setupAd()
        initializers.init(this)
    }

    private fun setupAd() {
        val safety = if (BuildConfig.DEBUG) AdSafetyLimits.TEST else AdSafetyLimits.UTILITY
        Log.d(
            TAG,
            "setupAd: isEnableAdmob=${BuildConfig.IS_ENABLE_ADMOB}, isDebug=${BuildConfig.DEBUG}, safetyPreset=${if (BuildConfig.DEBUG) "TEST" else "UTILITY"}",
        )
        val adConfig = AdSdkConfig(
            isEnableAdmob          = BuildConfig.IS_ENABLE_ADMOB,
            isDebug                = BuildConfig.DEBUG,
            admobAppOpenId         = BuildConfig.ADMOB_APP_OPEN_ID,
            admobInterstitialId    = BuildConfig.ADMOB_INTERSTITIAL_ID,
            admobBannerId          = BuildConfig.ADMOB_BANNER_ID,
            admobRewardedId        = BuildConfig.ADMOB_REWARDED_ID,
            applovinAppOpenId      = BuildConfig.APPLOVIN_APP_OPEN_ID,
            applovinInterstitialId = BuildConfig.APPLOVIN_INTER_ID,
            applovinBannerId       = BuildConfig.APPLOVIN_BANNER_ID,
            applovinRewardedId     = BuildConfig.APPLOVIN_REWARD_ID,
            applovinSdkKey         = BuildConfig.APPLOVIN_SDK_KEY,
            vipKeySecret           = AdKeys.VIP_SECRET,
            safety                 = safety,
        )

        AdManager.setConfig(adConfig)
        Log.d(TAG, "AdManager.setConfig done → initialize")
        AdManager.initialize(this) { success, gaid ->
            Log.d(TAG, "AdManager.initialize callback: success=$success, gaid=$gaid, isVipNow=${AdManager.isVipByKeyActive()}")
        }
    }
}
