package com.galaxyjoy.cpuinfo.feat.vip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.galaxyjoy.cpuinfo.BaseActivity
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.applyStatusBarColorToToolbar
import com.galaxyjoy.cpuinfo.util.enableEdgeToEdgeMatchingActionBar
import com.galaxyjoy.cpuinfo.util.isNightMode
import com.galaxyjoy.cpuinfo.util.resolveActionBarColor
import com.galaxyjoy.cpuinfo.util.resolveActionBarContentColor
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
        // Phải gọi trước super.onCreate()/setContentView() — xem kdoc enableEdgeToEdgeMatchingActionBar().
        enableEdgeToEdgeMatchingActionBar()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.act_vip)
        // Padding riêng cho IME — enableEdgeToEdge() chỉ set decorFitsSystemWindows(false),
        // không tự apply padding/adjustResize. util/Ext.kt's applyEdgeToEdgeContentPadding() chỉ
        // apply systemBars, không apply IME → giữ bản custom này combine cả systemBars + IME.
        setupEdgeToEdgeWithIme()

        val nightMode = isNightMode()
        val actionBarContentColor = resolveActionBarContentColor(nightMode)
        val toolbar = findViewById<Toolbar>(R.id.toolbarVip)
        toolbar.applyStatusBarColorToToolbar(resolveActionBarColor(nightMode))
        toolbar.setTitleTextColor(actionBarContentColor)
        // XML's app:navigationIcon drawable has no tint of its own (relies on the old static
        // white ?attr/colorOnPrimary theme default) — same fix as resolveActionBarContentColor.
        toolbar.navigationIcon?.mutate()?.setTint(actionBarContentColor)
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
     * Custom edge-to-edge: bottom padding nhận MAX(nav bar, IME). Khi keyboard show → padding
     * bottom = IME height → ScrollView bên trong tự shrink → `View.requestRectangleOnScreen`
     * trong FVipManagement work đúng (scroll btn Activate vào view above keyboard).
     *
     * Không pad `top` ở đây nữa — pad root sẽ đẩy toolbar xuống, để lộ dải nền cửa sổ mặc định
     * (`@color/background`) phía trên thay vì màu toolbar; top đã được xử lý riêng ở
     * [applyStatusBarColorToToolbar] ngay trên toolbar để nền toolbar tự tràn lên tới y=0.
     */
    private fun setupEdgeToEdgeWithIme() {
        val root = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(
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
