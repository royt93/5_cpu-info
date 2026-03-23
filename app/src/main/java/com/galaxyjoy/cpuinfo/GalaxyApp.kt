package com.galaxyjoy.cpuinfo

import android.util.Log
import androidx.multidex.MultiDexApplication
import com.applovin.sdk.AppLovinSdk
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
        val sdk = AppLovinSdk.getInstance(this)
        sdk.mediationProvider = "max"
        sdk.initializeSdk {
            AdManager.init(this, adConfig) { success, gaid ->
                Log.d("GalaxyApp", "AdManager init success=$success gaid=$gaid")
            }
        }
    }

    private fun getMyVipGAIDSet(): Set<String> = setOf(
        "9ad0127d-04be-4b6c-937a-ca3ed7f650b9", // vsmart iris
        "9b6499f2-d4de-4b9e-afdf-ac2a2b127fb1", // ss a50
        "c09b2f04-e145-490c-96f9-dab620074104", // oppo f7
        "c228aa08-bedd-4e6e-adf6-ae5e95bcddae", // vivo v15
        "46259467-0ac4-49c4-a3a2-7d3db3ce4bda", // tecno spark 20 pro+
        "1b7c3e3f-c709-4e85-b26f-dd74c4df2ed7", // vivo 1906
        "adaa42e7-9cc6-4a8a-9c90-d4d87842b12c", // tecno spark go 2024
        "f5a36a2f-5add-4315-a171-0f8dddab78c7", // ss s20u
        "6fbb207d-341d-470d-bb0a-dddd79522b32", // ss a52
        "40f8e222-cf7a-4fac-9913-6809c4c58817", // mipad 5
        "932099db-d381-4b52-98dc-5b96ba8b4ff4", // oppo reno 2f
        "a1339bd1-8ea5-47cd-969e-4b5721b576b7", // redmi note 8+
        "3f2f21d2-85eb-451b-a1a5-003668ba6345", // zte blade
        "261f772c-6a10-499c-b896-4157d9ab6a25", // ss a11
        "460d3f5c-bbe2-46fc-841a-6381e3c93864", // redmi95
        "49606ad7-5cee-43b4-9af7-8aa274644737", // redmi note 13 pro
        "6cf051f8-83f5-43b7-8c1a-1d20ae1f8d93", // redmi pad pro
        "da10cb05-5458-42df-ba86-630732356b35", // vivo z9
        "8f6ccdc1-08fd-4611-abdf-f48bdadb5581", // tablet lenovo
        "66e652de-79ef-4889-8074-9b482fd81b5a", // redmi a3
        "4ed22dd8-e8fb-442e-a75e-081a3d977957", // ss s24u
    )
}
