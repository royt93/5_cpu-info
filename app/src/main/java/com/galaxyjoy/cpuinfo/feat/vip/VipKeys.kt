package com.galaxyjoy.cpuinfo.feat.vip

import com.galaxyjoy.cpuinfo.common.const.AdKeys

/**
 * Whitelist các plain key + số ngày tương ứng.
 *
 * SDK 1.8.5+: `AdSdkConfig.vipRedeemCodes` (đúng "thẻ cào" API, non-deprecated) đọc trực tiếp
 * [keyToDays] này — `AdManager.activateVipByKey(ctx, rawKeyUserGõ, _)` tự tra map + tự tính days,
 * KHÔNG còn cần app tự thay key user gõ bằng `vipKeySecret` như bản cũ (≤1.1.5, xem git history).
 *
 * [lookupDays] vẫn giữ để UI hiển thị đúng "days" trong toast thành công + phân biệt "key hợp lệ
 * nhưng chưa activate" với "không phải key, thử gift-code" ở `FVipManagement.onRedeemClick`.
 */
internal object VipKeys {

    const val VIP_30D_DAYS = 30
    const val VIP_3D_DAYS = 3

    /** Nguồn cho `AdSdkConfig.vipRedeemCodes` (GalaxyApp.kt) — SDK tự verify + tự cấp đúng số ngày. */
    val keyToDays: Map<String, Int> by lazy {
        mapOf(
            AdKeys.VIP_30D_KEY to VIP_30D_DAYS,
            AdKeys.VIP_3D_KEY  to VIP_3D_DAYS,
        )
    }

    /** Trả số ngày nếu key hợp lệ, hoặc null. [keyToDays] injectable cho unit test JVM (tránh phụ
     * thuộc `android.util.Base64` của bản thật — xem `VipGiftCode.decode`'s cùng pattern). */
    fun lookupDays(rawInput: String, keyToDays: Map<String, Int> = VipKeys.keyToDays): Int? =
        keyToDays[rawInput.trim()]

    /**
     * Số ngày còn lại tới [expiryMs], làm tròn LÊN ngày kế tiếp (giống cách SDK tính hạn cho
     * redeem code) — dùng để hiển thị "days" trong toast thành công khi kích hoạt qua **token
     * ECDSA** (không nằm trong [keyToDays] nên [lookupDays] trả null, không có sẵn con số để hiện
     * trước; phải đọc lại hạn thật từ `AdManager.getVipByKeyExpiry()` SAU khi activate thành công).
     */
    fun daysUntil(expiryMs: Long, nowMs: Long): Int {
        val dayMs = 24L * 60L * 60L * 1000L
        val diffMs = expiryMs - nowMs
        return ((diffMs + dayMs - 1) / dayMs).toInt().coerceAtLeast(1)
    }
}
