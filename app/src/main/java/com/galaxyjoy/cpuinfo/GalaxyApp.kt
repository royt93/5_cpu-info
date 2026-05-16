package com.galaxyjoy.cpuinfo

import android.util.Log
import androidx.multidex.MultiDexApplication
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.galaxyjoy.cpuinfo.appinitializers.InitializersApp
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSdkConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

//TODO roy93~ why you see ad

//TODO roy93~ add lottie trang tri cho dep
//TODO roy93~ github

//appLovin MAX
//review in app bingo
//120hz
//splash screen
//gen ic_launcher https://easyappicon.com/
//keystore
//rename app
//license
//leak canary
//proguard
//font scale
//them version o man hinh chu
//rate, more, share, fb fan page, policy
//leak canary
//proguard

@HiltAndroidApp
class GalaxyApp : MultiDexApplication() {

    @Inject
    lateinit var initializers: InitializersApp

    override fun onCreate() {
        super.onCreate()
        setupAd()
        initializers.init(this)
    }

    private fun setupAd() {
        val adConfig = AdSdkConfig(
            isEnableAdmob         = BuildConfig.IS_ENABLE_ADMOB,
            isDebug               = BuildConfig.DEBUG,
            admobBannerId         = BuildConfig.ADMOB_BANNER_ID,
            admobInterstitialId   = BuildConfig.ADMOB_INTERSTITIAL_ID,
            admobAppOpenId        = BuildConfig.ADMOB_APP_OPEN_ID,
            applovinBannerId      = BuildConfig.APPLOVIN_BANNER_ID,
            applovinInterstitialId= BuildConfig.APPLOVIN_INTER_ID,
            applovinAppOpenId     = BuildConfig.APPLOVIN_APP_OPEN_ID
        )

        // QUAN TRỌNG: đúng thứ tự 3 lệnh này!
        AdManager.setConfig(adConfig)
        AdManager.earlyInit(this)

        // IS_ENABLE_ADMOB = false → AppLovin MAX path
        val initConfig = AppLovinSdkInitializationConfiguration.builder(
            BuildConfig.APPLOVIN_SDK_KEY,
            this,
        )
            .setMediationProvider(AppLovinMediationProvider.MAX)
            .build()
        AppLovinSdk.getInstance(this).initialize(initConfig) {
            AdManager.init(this, adConfig) { success, gaid ->
                Log.d("GalaxyApp", "AdManager init success=$success gaid=$gaid")
            }
        }
    }

}
