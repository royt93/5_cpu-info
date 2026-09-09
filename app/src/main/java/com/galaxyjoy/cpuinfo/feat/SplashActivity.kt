package com.galaxyjoy.cpuinfo.feat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.galaxyjoy.cpuinfo.BaseActivity
import com.galaxyjoy.cpuinfo.databinding.ActivitySplashBinding
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.ExperimentalAdApi
import com.roy.sdkadbmob.SafeLogger
import com.roy.sdkadbmob.awaitSplashComplete
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    companion object {
        private const val TAG = "roy93~Splash"
    }

    private lateinit var binding: ActivitySplashBinding
    private var splashJob: Job? = null

    private val delayedFinishRunnable = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Full-bleed splash drawable (@drawable/startup) đã che hết status/nav bar area —
        // chỉ cần status/nav bar transparent + auto icon để không có viền màu cứng lúc App Open
        // ad hoặc hệ thống hiện overlay đè lên, không cần custom inset listener (không có
        // element tương tác nào sát mép màn ở splash).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        SafeLogger.d(TAG, "onCreate → request UMP consent")
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // UMP consent → đợi resolved trước khi load App Open / Banner / Interstitial.
        // Callback có thể fire ở BG thread → guard isFinishing/isDestroyed trước khi
        // launch coroutine để tránh race khi user back ra khỏi splash sớm.
        AdManager.requestConsentInfoUpdate(this) { canRequestAds ->
            if (isFinishing || isDestroyed) {
                SafeLogger.d(TAG, "UMP callback fired AFTER activity destroyed → skip")
                return@requestConsentInfoUpdate
            }
            SafeLogger.d(TAG, "UMP canRequestAds=$canRequestAds → start splash flow")
            runSplashFlow()
        }
    }

    @OptIn(ExperimentalAdApi::class)
    private fun runSplashFlow() {
        SafeLogger.d(TAG, "runSplashFlow: awaitSplashComplete start")
        splashJob?.cancel()
        splashJob = lifecycleScope.launch {
            try {
                AdManager.awaitSplashComplete(this@SplashActivity)
                SafeLogger.d(TAG, "runSplashFlow: awaitSplashComplete returned → navigate")
            } catch (e: CancellationException) {
                // Activity bị destroy (xoay màn hình...) giữa lúc awaitSplashComplete đang suspend
                // → coroutine bị cancel theo lifecycleScope, KHÔNG phải lỗi load ad. Rethrow thay vì
                // nuốt (đúng mẫu SplashActivity.kt thật của SDK) — goToMain() bên dưới vẫn tự guard
                // isFinishing/isDestroyed nên không navigate nhầm dù rethrow ở đây.
                throw e
            } catch (e: Exception) {
                SafeLogger.e(TAG, "awaitSplashComplete error", e)
            }
            goToMain()
        }
    }

    private fun goToMain() {
        if (isFinishing || isDestroyed) {
            SafeLogger.d(TAG, "goToMain skipped (activity finishing/destroyed)")
            return
        }
        SafeLogger.d(TAG, "goToMain → ActHost")
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
