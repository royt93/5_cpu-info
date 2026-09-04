package com.galaxyjoy.cpuinfo.common.const

import android.util.Base64
import com.galaxyjoy.cpuinfo.BuildConfig

/**
 * Centralized constants for ad SDK config + VIP screen.
 *
 * Plain keys are Base64-encoded at compile-time (see doc/AD_PROMPT_AOS.MD §10.9).
 * Reverse-engineering is trivial, but enough to block casual peeking of decompiled APK.
 */
object AdKeys {

    /** Privacy Policy URL — bound to consent dialog + VIP screen footer. */
    const val PRIVACY_POLICY_URL: String = BuildConfig.PRIVACY_POLICY_URL

    /**
     * VIP secret seed truyền vào AdSdkConfig.vipKeySecret.
     * Lib dùng nó để verify `showVipByKeyDialog`. Đặt = key 30-ngày (Section 0).
     */
    val VIP_SECRET: String
        get() = decodeBase64(VIP_SECRET_B64)

    /** VIP key 30 ngày (plain Base64-encoded). */
    val VIP_30D_KEY: String
        get() = decodeBase64(VIP_30D_B64)

    /** VIP key 3 ngày (plain Base64-encoded) — dùng cho Rewarded ad reward. */
    val VIP_3D_KEY: String
        get() = decodeBase64(VIP_3D_B64)

    /** U11 — HMAC signing key riêng cho mã tặng VIP giữa 2 user (`VipGiftCode`). Cố ý **khác**
     * [VIP_SECRET] — lộ khoá này chỉ cho phép tự tạo mã tặng 1 ngày (đã giới hạn 1 mã/ngày +
     * hết hạn sau vài ngày ở `VipGiftLogic`), không cho phép đúc lại key redeem 30 ngày thật. */
    val VIP_GIFT_SIGNING_KEY: String
        get() = decodeBase64(VIP_GIFT_SIGNING_KEY_B64)

    // Base64 của plain key trong doc/AD_PROMPT_AOS.MD §0.
    private const val VIP_SECRET_B64 = "OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA=="
    private const val VIP_30D_B64 = "OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA=="
    private const val VIP_3D_B64 = "ZVE3QDkzTDBmITJZMjcwN3hOMDQwMjE5OTN1MEkjMmFL"
    private const val VIP_GIFT_SIGNING_KEY_B64 = "ZzFmVDlBYzNaMjdSb3kwNFFwMjE5OTNZMnUwSTcjUTA="

    private fun decodeBase64(b64: String): String =
        String(Base64.decode(b64, Base64.NO_WRAP))
}
