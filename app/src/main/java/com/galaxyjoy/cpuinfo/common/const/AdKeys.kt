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
     * Secret nội bộ truyền vào `AdSdkConfig.vipKeySecret` — CHỈ dùng chống-tamper
     * SharedPreferences trong SDK (1.8.5+), KHÔNG còn dùng làm "mã VIP" user gõ (đó là lỗi cũ đã
     * fix: trước đây field này trùng giá trị với [VIP_30D_KEY], nghĩa là lộ mã 30 ngày cũng lộ
     * luôn secret chống giả mạo). Giá trị RIÊNG, không public cho ai, không trùng bất kỳ
     * [VIP_30D_KEY]/[VIP_3D_KEY] nào — xem `ads.properties` (myKeyStore) mục `vipAntiTamperSecret`.
     */
    val VIP_ANTI_TAMPER_SECRET: String
        get() = decodeBase64(VIP_ANTI_TAMPER_SECRET_B64)

    /** VIP key 30 ngày (plain Base64-encoded). */
    val VIP_30D_KEY: String
        get() = decodeBase64(VIP_30D_B64)

    /** VIP key 3 ngày (plain Base64-encoded) — dùng cho Rewarded ad reward. */
    val VIP_3D_KEY: String
        get() = decodeBase64(VIP_3D_B64)

    /** U11 — HMAC signing key riêng cho mã tặng VIP giữa 2 user (`VipGiftCode`). Cố ý **khác**
     * [VIP_ANTI_TAMPER_SECRET]/[VIP_30D_KEY] — lộ khoá này chỉ cho phép tự tạo mã tặng 1 ngày (đã
     * giới hạn 1 mã/ngày + hết hạn sau vài ngày ở `VipGiftLogic`), không cho phép đúc lại key
     * redeem 30 ngày thật hay giả mạo chống-tamper. */
    val VIP_GIFT_SIGNING_KEY: String
        get() = decodeBase64(VIP_GIFT_SIGNING_KEY_B64)

    // Base64 của plain key trong doc/AD_PROMPT_AOS.MD §0.
    private const val VIP_ANTI_TAMPER_SECRET_B64 = "cVFTaDVpZSVwd1ZJc2NTb2tPNmhUS3liM2VERkE="
    private const val VIP_30D_B64 = "OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA=="
    private const val VIP_3D_B64 = "ZVE3QDkzTDBmITJZMjcwN3hOMDQwMjE5OTN1MEkjMmFL"
    private const val VIP_GIFT_SIGNING_KEY_B64 = "ZzFmVDlBYzNaMjdSb3kwNFFwMjE5OTNZMnUwSTcjUTA="

    private fun decodeBase64(b64: String): String =
        String(Base64.decode(b64, Base64.NO_WRAP))
}
