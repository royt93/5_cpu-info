package com.galaxyjoy.cpuinfo.feat.vip.gift

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

/** Uses an explicit plain-string `signingKey` throughout — the default parameter goes through
 * [com.galaxyjoy.cpuinfo.common.const.AdKeys.VIP_GIFT_SIGNING_KEY]'s `android.util.Base64` decode,
 * which this project's JVM unit-test stub can't resolve (see [VipGiftCode.decode]'s own doc). */
class VipGiftCodeTest {

    private val testKey = "unit-test-signing-key"

    @Test
    fun `encode then decode round-trips exactly`() {
        val code = VipGiftCode.encode(20_000L, testKey)

        assertEquals(20_000L, VipGiftCode.decode(code, testKey))
    }

    @Test
    fun `decode trims surrounding whitespace from a pasted code`() {
        val code = VipGiftCode.encode(20_000L, testKey)

        assertEquals(20_000L, VipGiftCode.decode("  \n$code\n  ", testKey))
    }

    @Test
    fun `decode returns null for empty string`() {
        assertNull(VipGiftCode.decode("", testKey))
    }

    @Test
    fun `decode returns null for garbage text`() {
        assertNull(VipGiftCode.decode("not a real code", testKey))
    }

    @Test
    fun `decode returns null when the epoch day is not a number`() {
        assertNull(VipGiftCode.decode("abc.deadbeefcafe", testKey))
    }

    @Test
    fun `decode returns null when the signature is tampered with`() {
        val code = VipGiftCode.encode(20_000L, testKey)
        val tampered = code.dropLast(1) + if (code.last() == '0') '1' else '0'

        assertNull(VipGiftCode.decode(tampered, testKey))
    }

    @Test
    fun `decode returns null when the epoch day is tampered with but signature kept`() {
        val code = VipGiftCode.encode(20_000L, testKey)
        val forged = code.replaceFirst("20000", "20001")

        assertNull(VipGiftCode.decode(forged, testKey))
    }

    @Test
    fun `decode returns null when the signing key does not match`() {
        val code = VipGiftCode.encode(20_000L, testKey)

        assertNull(VipGiftCode.decode(code, "a different key"))
    }

    @Test
    fun `different epoch days produce different codes`() {
        assertEquals(false, VipGiftCode.encode(1L, testKey) == VipGiftCode.encode(2L, testKey))
    }

    @Test
    fun `encode is deterministic for the same epoch day and key`() {
        assertEquals(VipGiftCode.encode(555L, testKey), VipGiftCode.encode(555L, testKey))
    }
}
