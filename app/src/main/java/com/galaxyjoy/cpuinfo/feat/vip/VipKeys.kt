package com.galaxyjoy.cpuinfo.feat.vip

import com.galaxyjoy.cpuinfo.common.const.AdKeys

/**
 * Whitelist các plain key + số ngày tương ứng.
 *
 * Lib `AdManager.activateVipByKey(ctx, key, days)` (v1.1.5) chỉ accept duy nhất
 * `key == adConfig.vipKeySecret`. Vì vậy tất cả entry trong whitelist này MUST
 * map về cùng `AdKeys.VIP_SECRET` khi gọi lib, chỉ thay đổi field `days`.
 *
 * Map app-side là để UX (user có cảm giác có "2 key khác nhau" cho 30/3 ngày),
 * nhưng key truyền vào lib luôn = `VIP_SECRET`.
 */
internal object VipKeys {

    const val VIP_30D_DAYS = 30
    const val VIP_3D_DAYS = 3

    private val keyToDays: Map<String, Int> by lazy {
        mapOf(
            AdKeys.VIP_30D_KEY to VIP_30D_DAYS,
            AdKeys.VIP_3D_KEY  to VIP_3D_DAYS,
        )
    }

    /** Trả số ngày nếu key hợp lệ, hoặc null. */
    fun lookupDays(rawInput: String): Int? = keyToDays[rawInput.trim()]
}
