package com.galaxyjoy.cpuinfo

import android.util.Log
import androidx.multidex.MultiDexApplication
import com.galaxyjoy.cpuinfo.appinitializers.InitializersApp
import com.galaxyjoy.cpuinfo.feat.SplashActivity
import com.galaxyjoy.cpuinfo.sdkadbmob.AdMobManager
import com.galaxyjoy.cpuinfo.sdkadbmob.AppLifecycleListener
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

//TODO roy93~ why you see ad
//TODO roy93~ firebase

//TODO roy93~ vung bi mat de show applovin config
//TODO roy93~ splash screen
//TODO roy93~ add lottie trang tri cho dep
//TODO roy93~ github
//TODO roy93~ uninstall app

//admob
//ad applovin
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
//        this.setupApplovinAd()
        setupAdmob()

        initializers.init(this)
    }

    private fun setupAdmob() {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@GalaxyApp) {}
            AdMobManager.init(this@GalaxyApp) { success, gaidCurrent ->
                Log.d("roy93~", "AdMobManager init success $success, gaidCurrent $gaidCurrent")
            }
        }
        registerActivityLifecycleCallbacks(
            AppLifecycleListener(
                { isForeground, activity ->
                    if (isForeground) {
                        Log.d("roy93~", "App moved to Foreground")
                        Log.d("roy93~", "activity.localClassName ${activity.localClassName}")
                        Log.d(
                            "roy93~",
                            "SplashActivity::class.java.simpleName ${SplashActivity::class.java.simpleName}"
                        )
                        if (activity.localClassName == SplashActivity::class.java.simpleName) {
                            //do nothing
                        } else {
//                            AdMobManager.showAppOpenAd(activity)
                        }
                    } else {
                        Log.d("roy93~", "App moved to Background")
                    }
                }, { activity ->
                    Log.d("roy93~", "callbackActivityCreated ${activity.localClassName}")
                    if (activity.localClassName == SplashActivity::class.java.simpleName) {
                        //do nothing
                    } else {
//                        AdMobManager.loadAppOpenAd(
//                            context = this,
//                            adUnitId = BuildConfig.ADMOB_APP_OPEN_ID,
//                            onAdLoaded = {},
//                        )
                    }
                }
            )
        )
    }
}
