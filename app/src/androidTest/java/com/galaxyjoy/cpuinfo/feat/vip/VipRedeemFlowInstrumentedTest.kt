package com.galaxyjoy.cpuinfo.feat.vip

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.common.const.AdKeys
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
 * Integration tier for the 2026-09-08 VIP-redeem migration: exercises the REAL
 * [AdManager]/`AppPreferences` (SDK 1.8.5), configured exactly like [com.galaxyjoy.cpuinfo.GalaxyApp]
 * (`vipRedeemCodes = VipKeys.keyToDays`, `vipKeySecret = AdKeys.VIP_ANTI_TAMPER_SECRET`), proving:
 * 1) the raw key `FVipManagement.onRedeemClick` now hands to `activateVipByKey` actually redeems
 *    (previously it always passed the secret, never the user's raw input);
 * 2) `AdManager.grantVipDays` (replacing the old `activateVipByKey(secret, days)` trick in
 *    `grantViaRewarded`/`tryRedeemGiftCode`/`ShieldScoreBottomSheet`) genuinely accumulates across
 *    calls, unlike the old raise-to-max legacy path.
 *
 * `@InternalAdApi` test seams ([configureTestHooks], [clearAppPreferencesForTest],
 * [resetVipActivationBackoffForTest]) are the SDK's own documented instrumented-test pattern (see
 * `AdManagerTestHooks.kt` KDoc) — test-only, never used from production code.
 */
@OptIn(InternalAdApi::class)
@RunWith(AndroidJUnit4::class)
class VipRedeemFlowInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val toleranceMs = 5 * 60 * 1000L // 5 min slop for test execution time
    private val dayMs = 24L * 60L * 60L * 1000L

    @Before
    fun setUp() {
        AdManager.setConfig(
            AdSdkConfig(
                isEnableAdmob = true,
                isDebug = true, // skips the release-only "weak vipKeySecret" fail-fast
                vipKeySecret = AdKeys.VIP_ANTI_TAMPER_SECRET,
                vipRedeemCodes = VipKeys.keyToDays,
                safety = AdSafetyLimits.TEST,
            ),
        )
        // activateVipByKey requires network per SDK design (V-03) — force it on so this test is
        // deterministic regardless of the test device's real connectivity.
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
    fun activateVipByKey_with30DayKey_grantsVipForThirtyDays() {
        val ok = AdManager.activateVipByKey(context, AdKeys.VIP_30D_KEY, days = 0)

        assertTrue("activateVipByKey should accept the real 30-day key via vipRedeemCodes", ok)
        assertTrue(AdManager.isVipByKeyActive())
        val expected = System.currentTimeMillis() + VipKeys.VIP_30D_DAYS * dayMs
        assertEquals(expected.toDouble(), AdManager.getVipByKeyExpiry().toDouble(), toleranceMs.toDouble())
    }

    @Test
    fun activateVipByKey_with3DayKey_grantsVipForThreeDays() {
        val ok = AdManager.activateVipByKey(context, AdKeys.VIP_3D_KEY, days = 0)

        assertTrue("activateVipByKey should accept the real 3-day key via vipRedeemCodes", ok)
        val expected = System.currentTimeMillis() + VipKeys.VIP_3D_DAYS * dayMs
        assertEquals(expected.toDouble(), AdManager.getVipByKeyExpiry().toDouble(), toleranceMs.toDouble())
    }

    @Test
    fun activateVipByKey_withUnknownCode_returnsFalseAndGrantsNothing() {
        val ok = AdManager.activateVipByKey(context, "not-a-real-redeem-code", days = 30)

        assertFalse(
            "legacy plaintext branch is disabled (allowLegacyPlaintextVipKey unset) — an unknown " +
                "string must not activate VIP just because it happens to be passed with days=30",
            ok,
        )
        assertFalse(AdManager.isVipByKeyActive())
    }

    @Test
    fun activateVipByKey_withAntiTamperSecretItself_isRejected() {
        // The exact bug this migration fixes: the internal secret must NOT double as a redeem code.
        val ok = AdManager.activateVipByKey(context, AdKeys.VIP_ANTI_TAMPER_SECRET, days = 30)

        assertFalse(
            "the anti-tamper secret must not itself work as a public VIP redeem code",
            ok,
        )
    }

    @Test
    fun grantVipDays_calledTwice_accumulatesInsteadOfCapping() {
        assertTrue(AdManager.grantVipDays(context, 3))
        val afterFirst = AdManager.getVipByKeyExpiry()

        assertTrue(AdManager.grantVipDays(context, 2))
        val afterSecond = AdManager.getVipByKeyExpiry()

        val grantedMs = afterSecond - afterFirst
        assertEquals(
            "second grantVipDays call must ADD 2 more days on top of the first grant " +
                "(this is what grantViaRewarded/tryRedeemGiftCode/ShieldScoreBottomSheet now rely on)",
            (2 * dayMs).toDouble(),
            grantedMs.toDouble(),
            toleranceMs.toDouble(),
        )
    }
}
