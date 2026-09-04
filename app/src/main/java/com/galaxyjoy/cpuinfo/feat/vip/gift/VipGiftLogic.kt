package com.galaxyjoy.cpuinfo.feat.vip.gift

/** U11 — pure day-counting/accumulation rules, no Android deps. */
object VipGiftLogic {

    const val GIFT_DAYS = 1

    /** Codes older than this (in epoch-days since issuance) are rejected — bounds how long a
     * screenshotted/forwarded code stays redeemable. */
    const val MAX_CODE_AGE_DAYS = 2L

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun canGenerateToday(lastGeneratedEpochDay: Long?, todayEpochDay: Long): Boolean =
        lastGeneratedEpochDay != todayEpochDay

    fun canRedeemToday(lastRedeemedEpochDay: Long?, todayEpochDay: Long): Boolean =
        lastRedeemedEpochDay != todayEpochDay

    fun isCodeFresh(issuedEpochDay: Long, todayEpochDay: Long): Boolean {
        val ageDays = todayEpochDay - issuedEpochDay
        return ageDays in 0..MAX_CODE_AGE_DAYS
    }

    /**
     * `AdManager.activateVipByKey(ctx, key, days)` (v1.1.5) always computes
     * `now + days*DAY_MS` and OVERWRITES the stored expiry — it does not accumulate. Redeeming a
     * gift while already VIP with e.g. 10 days left must not reset that down to 1 day, so this
     * computes the `days` value that makes the call's overwrite land on
     * `max(currentExpiryMs, nowMs) + giftDays` instead of just `nowMs + giftDays`.
     */
    fun daysToGrantForAccumulate(currentExpiryMs: Long, nowMs: Long, giftDays: Int = GIFT_DAYS): Int {
        val baseMs = maxOf(currentExpiryMs, nowMs)
        val targetMs = baseMs + giftDays * DAY_MS
        val diffMs = targetMs - nowMs
        return ((diffMs + DAY_MS - 1) / DAY_MS).toInt().coerceAtLeast(1)
    }
}
