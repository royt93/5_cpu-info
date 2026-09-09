package com.galaxyjoy.cpuinfo.feat.vip

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

/**
 * JVM unit test — uses an injected fake [Map] instead of the real [VipKeys.keyToDays] default
 * (that default goes through `AdKeys.VIP_30D_KEY`/`VIP_3D_KEY`'s `android.util.Base64` decode,
 * unresolved in this project's JVM unit-test stub — same reasoning as `VipGiftCodeTest`).
 *
 * Regression coverage for 2026-09-08 fix: before this fix, the string passed to
 * `AdManager.activateVipByKey` was always the internal `vipKeySecret`, never the raw key the user
 * typed — [lookupDays] existed only to pick a `days` value. After the fix, [lookupDays]'s result
 * also gates whether `onRedeemClick` calls the SDK with the user's raw input at all, so a wrong
 * `days`/key mapping here now directly changes which key redeems for how long.
 */
class VipKeysTest {

    private val fakeKeys = mapOf(
        "fake-30d-key" to VipKeys.VIP_30D_DAYS,
        "fake-3d-key" to VipKeys.VIP_3D_DAYS,
    )

    @Test
    fun `lookupDays returns VIP_30D_DAYS for the 30-day key`() {
        assertEquals(VipKeys.VIP_30D_DAYS, VipKeys.lookupDays("fake-30d-key", fakeKeys))
    }

    @Test
    fun `lookupDays returns VIP_3D_DAYS for the 3-day key`() {
        assertEquals(VipKeys.VIP_3D_DAYS, VipKeys.lookupDays("fake-3d-key", fakeKeys))
    }

    @Test
    fun `lookupDays returns null for a key not in the whitelist`() {
        assertNull(VipKeys.lookupDays("not-a-real-key", fakeKeys))
    }

    @Test
    fun `lookupDays returns null for empty input`() {
        assertNull(VipKeys.lookupDays("", fakeKeys))
    }

    @Test
    fun `lookupDays trims surrounding whitespace before matching`() {
        assertEquals(30, VipKeys.lookupDays("  \nfake-30d-key\n  ", fakeKeys))
    }

    @Test
    fun `lookupDays is case-sensitive`() {
        assertNull(VipKeys.lookupDays("FAKE-30D-KEY", fakeKeys))
    }

    private val dayMs = 24L * 60L * 60L * 1000L

    @Test
    fun `daysUntil rounds up a partial day`() {
        // 12h remaining -> must round UP to 1, never down to 0 (would under-report a token grant
        // as "0 days" in the redeem-success toast).
        assertEquals(1, VipKeys.daysUntil(expiryMs = dayMs / 2, nowMs = 0L))
    }

    @Test
    fun `daysUntil returns exact day count for an exact multiple`() {
        assertEquals(3, VipKeys.daysUntil(expiryMs = 3 * dayMs, nowMs = 0L))
    }

    @Test
    fun `daysUntil rounds up just over a whole day boundary`() {
        assertEquals(4, VipKeys.daysUntil(expiryMs = 3 * dayMs + 1, nowMs = 0L))
    }

    @Test
    fun `daysUntil never returns less than 1 even for an already-expired timestamp`() {
        assertEquals(1, VipKeys.daysUntil(expiryMs = 0L, nowMs = dayMs))
    }
}
