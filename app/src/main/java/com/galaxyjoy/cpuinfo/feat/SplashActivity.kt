package com.galaxyjoy.cpuinfo.feat

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.galaxyjoy.cpuinfo.BuildConfig
import com.galaxyjoy.cpuinfo.databinding.ActivitySplashBinding
import com.galaxyjoy.cpuinfo.sdkadbmob.AdMobManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.jvm.java

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("roy93~", "onCreate")
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdMobManager.initSplashScreen(this, {
            goToMain()
        })
    }

    private fun goToMain() {
        val intent = Intent(this@SplashActivity, ActHost::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finishAffinity()
    }
}
