package com.galaxyjoy.cpuinfo.feat.vip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.galaxyjoy.cpuinfo.BaseActivity
import com.galaxyjoy.cpuinfo.R
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
        // Phải gọi trước setContentView() — status/nav bar cùng màu colorPrimary như toolbar
        // (Widget.MaterialComponents.Toolbar.Primary bên dưới) theo yêu cầu match action bar.
        // SystemBarStyle.dark(): colorOnPrimary luôn trắng ở cả 2 theme (xem util/Ext.kt
        // setupEdgeToEdge kdoc) nên icon sáng luôn đúng, không cần .auto().
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(primaryColor),
            navigationBarStyle = SystemBarStyle.dark(primaryColor),
        )
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.act_vip)
        // Padding riêng cho IME — enableEdgeToEdge() chỉ set decorFitsSystemWindows(false),
        // không tự apply padding/adjustResize. Project's setupEdgeToEdge() chỉ apply systemBars,
        // không apply IME → giữ bản custom này combine cả systemBars + IME.
        setupEdgeToEdgeWithIme()

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

    /**
     * Custom edge-to-edge: top padding nhận status bar, bottom padding nhận MAX(nav bar, IME).
     * Khi keyboard show → padding bottom = IME height → ScrollView bên trong tự shrink →
     * `View.requestRectangleOnScreen` trong FVipManagement work đúng (scroll btn Activate
     * vào view above keyboard).
     */
    private fun setupEdgeToEdgeWithIme() {
        val root = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(
                top = systemBars.top,
                left = systemBars.left,
                right = systemBars.right,
                // Bottom = MAX(nav bar, IME) — khi keyboard show IME > nav, content shrink lên.
                bottom = maxOf(systemBars.bottom, ime.bottom),
            )
            insets
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
