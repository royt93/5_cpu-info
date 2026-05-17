package com.galaxyjoy.cpuinfo.feat.vip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import com.galaxyjoy.cpuinfo.BaseActivity
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.setupEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

/**
 * Standalone Activity host cho VIP management screen.
 *
 * Tách riêng để:
 * - User experience tốt hơn (modal-like, có back arrow rõ ràng)
 * - Không phụ thuộc vào nav stack của ActHost
 * - Có thể launch từ nhiều entry point khác nhau (toolbar icon, Settings preference)
 */
@AndroidEntryPoint
class ActVip : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.act_vip)
        // Edge-to-edge — toolbar hứng status bar padding-top auto, content trong safe area.
        // Đồng bộ với ActHost. Phải gọi SAU setContentView để view tree ready.
        setupEdgeToEdge()

        val toolbar = findViewById<Toolbar>(R.id.toolbarVip)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.vip_screen_title)
        }
        toolbar.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.vipFragmentContainer, FVipManagement())
                .commit()
        }
    }

    override fun finish() {
        Log.d(TAG, "finish → return to caller")
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        private const val TAG = "roy93~ActVip"

        /** Convenience: start ActVip với slide-up transition đẹp. */
        fun start(context: Context) {
            val intent = Intent(context, ActVip::class.java)
            context.startActivity(intent)
        }
    }
}
