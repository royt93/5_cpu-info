package com.galaxyjoy.cpuinfo.common.const

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for 2026-09-08 fix: [AdKeys.VIP_SECRET] used to equal [AdKeys.VIP_30D_KEY]
 * exactly — the value passed to `AdSdkConfig.vipKeySecret` (anti-tamper) was the SAME string as
 * the public 30-day VIP redeem code, so leaking the code to a customer also leaked the
 * anti-tamper secret. [AdKeys.VIP_ANTI_TAMPER_SECRET] replaces `VIP_SECRET` and must never equal
 * any of the public-facing keys.
 *
 * `android.util.Base64` decode requires a real Android runtime — instrumented, not JVM unit test
 * (same reasoning as [com.galaxyjoy.cpuinfo.feat.vip.gift.VipGiftCodeInstrumentedTest]).
 */
@RunWith(AndroidJUnit4::class)
class AdKeysInstrumentedTest {

    @Test
    fun antiTamperSecret_isNotBlank() {
        assertTrue(AdKeys.VIP_ANTI_TAMPER_SECRET.isNotBlank())
    }

    @Test
    fun antiTamperSecret_differsFrom30DayRedeemKey() {
        assertNotEquals(AdKeys.VIP_30D_KEY, AdKeys.VIP_ANTI_TAMPER_SECRET)
    }

    @Test
    fun antiTamperSecret_differsFrom3DayRedeemKey() {
        assertNotEquals(AdKeys.VIP_3D_KEY, AdKeys.VIP_ANTI_TAMPER_SECRET)
    }

    @Test
    fun antiTamperSecret_differsFromGiftSigningKey() {
        assertNotEquals(AdKeys.VIP_GIFT_SIGNING_KEY, AdKeys.VIP_ANTI_TAMPER_SECRET)
    }

    @Test
    fun redeemKeys_30dAnd3dAreStillDistinctFromEachOther() {
        assertNotEquals(AdKeys.VIP_30D_KEY, AdKeys.VIP_3D_KEY)
    }
}
