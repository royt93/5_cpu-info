package com.galaxyjoy.cpuinfo.feat

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.galaxyjoy.cpuinfo.databinding.ActivitySplashBinding
import com.roy.sdkadbmob.AdManager
import kotlin.jvm.java

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val delayedFinishRunnable = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("roy93~", "onCreate")
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdManager.initSplashScreen(this) {
            goToMain()
        }
    }

    private fun goToMain() {
        val intent = Intent(this@SplashActivity, ActHost::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        // Delay finish để đợi animation hoàn tất, dùng named Runnable để có thể cancel
        window.decorView.postDelayed(delayedFinishRunnable, 300)
    }

    override fun onDestroy() {
        // Cancel pending runnable để tránh rò rỉ Activity reference
        window.decorView.removeCallbacks(delayedFinishRunnable)
        super.onDestroy()
    }
}
