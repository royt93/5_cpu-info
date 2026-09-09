package com.galaxyjoy.cpuinfo.feat.vip.mint

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyLimits
import com.roy.sdkadbmob.AdSdkConfig
import com.roy.sdkadbmob.InternalAdApi
import com.roy.sdkadbmob.clearAppPreferencesForTest
import com.roy.sdkadbmob.configureTestHooks
import com.roy.sdkadbmob.resetVipActivationBackoffForTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tier for the 2026-09-08 VIP ECDSA token migration — real `AdManager.generateVipKeyPair`
 * / `generateVipToken` / `activateVipByToken` (SDK 1.8.5), not mocked. Deliberately generates its
 * OWN throwaway keypair per test (never the app's real production keypair, which lives only in
 * `ads.properties` and must never appear in tracked source) — proves the exact mechanism
 * [VipTokenMintBottomSheet]'s `onGenerate` callback and [FVipManagement.onRedeemClick]'s token
 * fallback rely on, without touching real secrets.
 */
@OptIn(InternalAdApi::class)
@RunWith(AndroidJUnit4::class)
class VipTokenMintFlowInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val toleranceMs = 5 * 60 * 1000L
    private val dayMs = 24L * 60L * 60L * 1000L

    private fun configureWith(publicKeyBase64: String) {
        AdManager.setConfig(
            AdSdkConfig(
                isEnableAdmob = true,
                isDebug = true, // generateVipToken() is a no-op ("") when isDebug=false
                vipKeySecret = "integration-test-anti-tamper-secret-not-real",
                vipTokenPublicKey = publicKeyBase64,
                safety = AdSafetyLimits.TEST,
            ),
        )
        AdManager.configureTestHooks(network = { true })
        AdManager.clearAppPreferencesForTest(context)
        AdManager.resetVipActivationBackoffForTest()
    }

    @After
    fun tearDown() {
        AdManager.clearAppPreferencesForTest(context)
        AdManager.resetVipActivationBackoffForTest()
    }

    @Test
    fun mintThenRedeem_withMatchingKeypair_grantsRequestedDays() {
        val pair = AdManager.generateVipKeyPair()
        configureWith(pair.publicKeyBase64)

        val token = AdManager.generateVipToken(privateKeyBase64 = pair.privateKeyBase64, days = 5)
        assertTrue("generateVipToken should mint a non-blank token when isDebug=true", token.isNotBlank())

        val activated = AdManager.activateVipByToken(context, token)
        assertTrue("a token signed by the matching private key must activate", activated)
        assertTrue(AdManager.isVipByKeyActive())

        val expected = System.currentTimeMillis() + 5 * dayMs
        assertEquals(expected.toDouble(), AdManager.getVipByKeyExpiry().toDouble(), toleranceMs.toDouble())
    }

    @Test
    fun redeemToken_signedByDifferentPrivateKey_isRejected() {
        val trustedPair = AdManager.generateVipKeyPair()
        val attackerPair = AdManager.generateVipKeyPair()
        configureWith(trustedPair.publicKeyBase64)

        // Forge a token with a DIFFERENT keypair's private key — must NOT verify against the
        // public key the app actually trusts. This is the core security property of switching
        // off the deprecated shared-secret vipKeySecret path.
        val forgedToken = AdManager.generateVipToken(privateKeyBase64 = attackerPair.privateKeyBase64, days = 30)
        assertTrue(forgedToken.isNotBlank()) // minting itself succeeds — verification must not

        val activated = AdManager.activateVipByToken(context, forgedToken)

        assertFalse("token signed by a non-matching private key must be rejected", activated)
        assertFalse(AdManager.isVipByKeyActive())
    }

    @Test
    fun mintWithBlankPrivateKey_returnsBlank_matchingMintUisFailurePath() {
        val pair = AdManager.generateVipKeyPair()
        configureWith(pair.publicKeyBase64)

        val token = AdManager.generateVipToken(privateKeyBase64 = "", days = 5)

        assertTrue(
            "an empty/invalid private key must not mint a usable token (VipTokenMintContent " +
                "treats a blank return as failure)",
            token.isBlank(),
        )
    }
}
