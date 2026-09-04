package com.galaxyjoy.cpuinfo.feat.vip.gift

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real end-to-end tier for [VipGiftCode] — unlike [VipGiftCodeTest] (explicit plain-string test
 * key, JVM-only), this uses the real default `signingKey` (`AdKeys.VIP_GIFT_SIGNING_KEY`), whose
 * `android.util.Base64` decode only resolves to the real value on a real device.
 */
@RunWith(AndroidJUnit4::class)
class VipGiftCodeInstrumentedTest {

    @Test
    fun realSigningKey_encodeThenDecode_roundTripsExactly() {
        val code = VipGiftCode.encode(20_000L)

        assertEquals(20_000L, VipGiftCode.decode(code))
    }

    @Test
    fun realSigningKey_decodeReturnsNull_forGarbageText() {
        assertNull(VipGiftCode.decode("not a real code"))
    }

    @Test
    fun realSigningKey_decodeReturnsNull_whenTampered() {
        val code = VipGiftCode.encode(20_000L)
        val tampered = code.dropLast(1) + if (code.last() == '0') '1' else '0'

        assertNull(VipGiftCode.decode(tampered))
    }
}
