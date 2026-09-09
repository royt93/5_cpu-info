package com.galaxyjoy.cpuinfo.feat.vip.gift

import com.galaxyjoy.cpuinfo.common.const.AdKeys
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * U11 — gift-a-day share code. Deliberately does NOT embed any VIP redeem key or the app's
 * anti-tamper secret ([AdKeys.VIP_ANTI_TAMPER_SECRET]) anywhere in the shared text. The code just
 * proves "this is a genuine gift, issued on day X" via HMAC with a separate signing key
 * ([AdKeys.VIP_GIFT_SIGNING_KEY]); redemption grants the gift day(s) via `AdManager.grantVipDays`
 * (see `FVipManagement.tryRedeemGiftCode`), unrelated to the redeem-key/anti-tamper secrets.
 */
object VipGiftCode {

    private const val SEPARATOR = "."
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val SIGNATURE_HEX_LENGTH = 12

    fun encode(issuedEpochDay: Long, signingKey: String = AdKeys.VIP_GIFT_SIGNING_KEY): String =
        "$issuedEpochDay$SEPARATOR${signature(issuedEpochDay, signingKey)}"

    /** Returns the issued epoch-day if [raw]'s signature checks out, else `null` — malformed,
     * truncated, or tampered-with input all just fail closed here, never throw.
     *
     * [signingKey] defaults to the real embedded key; tests pass a plain string literal instead —
     * [AdKeys.VIP_GIFT_SIGNING_KEY] goes through `android.util.Base64`, a real Android framework
     * call this project's JVM unit-test stub (`isReturnDefaultValues = true`) turns into a `null`
     * return rather than the real decode, same class of limitation as `Uri.Builder`/
     * `GradientDrawable` elsewhere in this codebase.
     */
    fun decode(raw: String, signingKey: String = AdKeys.VIP_GIFT_SIGNING_KEY): Long? {
        val parts = raw.trim().split(SEPARATOR)
        if (parts.size != 2) return null
        val issuedEpochDay = parts[0].toLongOrNull() ?: return null
        return if (parts[1] == signature(issuedEpochDay, signingKey)) issuedEpochDay else null
    }

    private fun signature(issuedEpochDay: Long, signingKey: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(signingKey.toByteArray(), HMAC_ALGORITHM))
        val digest = mac.doFinal("VIPGIFT|$issuedEpochDay".toByteArray())
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }.take(SIGNATURE_HEX_LENGTH)
    }
}
