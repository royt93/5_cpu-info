package com.galaxyjoy.cpuinfo.feat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.galaxyjoy.cpuinfo.databinding.ActivitySplashBinding
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.ExperimentalAdApi
import com.roy.sdkadbmob.awaitSplashComplete
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "roy93~Splash"
    }

    private lateinit var binding: ActivitySplashBinding
    private var splashJob: Job? = null

    private val delayedFinishRunnable = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate → request UMP consent")
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // UMP consent → đợi resolved trước khi load App Open / Banner / Interstitial.
        // Callback có thể fire ở BG thread → guard isFinishing/isDestroyed trước khi
        // launch coroutine để tránh race khi user back ra khỏi splash sớm.
        AdManager.requestConsentInfoUpdate(this) { canRequestAds ->
            if (isFinishing || isDestroyed) {
                Log.d(TAG, "UMP callback fired AFTER activity destroyed → skip")
                return@requestConsentInfoUpdate
            }
            Log.d(TAG, "UMP canRequestAds=$canRequestAds → start splash flow")
            runSplashFlow()
        }
    }

    @OptIn(ExperimentalAdApi::class)
    private fun runSplashFlow() {
        Log.d(TAG, "runSplashFlow: awaitSplashComplete start")
        splashJob?.cancel()
        splashJob = lifecycleScope.launch {
            try {
                AdManager.awaitSplashComplete(this@SplashActivity)
                Log.d(TAG, "runSplashFlow: awaitSplashComplete returned → navigate")
            } catch (e: Exception) {
                Log.e(TAG, "awaitSplashComplete error", e)
            }
            goToMain()
        }
    }

    private fun goToMain() {
        if (isFinishing || isDestroyed) {
            Log.d(TAG, "goToMain skipped (activity finishing/destroyed)")
            return
        }
        Log.d(TAG, "goToMain → ActHost")
        val intent = Intent(this@SplashActivity, ActHost::class.java)
        startActivity(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        window.decorView.postDelayed(delayedFinishRunnable, 300)
    }

    override fun onDestroy() {
        splashJob?.cancel()
        splashJob = null
        window.decorView.removeCallbacks(delayedFinishRunnable)
        super.onDestroy()
    }
}
