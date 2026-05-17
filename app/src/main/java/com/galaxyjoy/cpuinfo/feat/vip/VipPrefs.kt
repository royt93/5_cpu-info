package com.galaxyjoy.cpuinfo.feat.vip

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistence riêng cho VIP screen.
 *
 * Lib [com.roy.sdkadbmob.AdManager] chỉ persist `vipByKeyUntil` (expiry timestamp).
 * Để vẽ progress bar elapsed-semantic cần biết cả `grantedAtMs` — không có trong lib.
 * Khi lib bổ sung `getVipByKeyGrantedAt()` → xoá class này, đọc trực tiếp lib.
 */
internal class VipPrefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveGrantedAtMs(ms: Long) {
        sp.edit().putLong(KEY_GRANTED_AT_MS, ms).apply()
    }

    fun getGrantedAtMs(): Long = sp.getLong(KEY_GRANTED_AT_MS, 0L)

    fun clearGrantedAtMs() {
        sp.edit().remove(KEY_GRANTED_AT_MS).apply()
    }

    fun markUserRedeemed() {
        sp.edit().putBoolean(KEY_USER_REDEEMED, true).apply()
    }

    fun userRedeemedAtLeastOnce(): Boolean = sp.getBoolean(KEY_USER_REDEEMED, false)

    /** Tổng số ngày VIP đã activate từ trước đến nay (cộng dồn). */
    fun getTotalDaysActivated(): Int = sp.getInt(KEY_TOTAL_DAYS_ACTIVATED, 0)

    fun addTotalDaysActivated(days: Int) {
        if (days <= 0) return
        val current = getTotalDaysActivated()
        sp.edit().putInt(KEY_TOTAL_DAYS_ACTIVATED, current + days).apply()
    }

    companion object {
        private const val PREFS_NAME = "vip_screen_prefs"
        private const val KEY_GRANTED_AT_MS = "granted_at_ms"
        private const val KEY_USER_REDEEMED = "user_redeemed_once"
        private const val KEY_TOTAL_DAYS_ACTIVATED = "total_days_activated"
    }
}
