package com.galaxyjoy.cpuinfo.feat.canmydevice

import com.galaxyjoy.cpuinfo.domain.model.CameraFacing
import com.galaxyjoy.cpuinfo.domain.model.CameraLensData
import com.galaxyjoy.cpuinfo.domain.model.DrmSchemeData
import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessEvaluator
import com.galaxyjoy.cpuinfo.feat.canmydevice.CanMyDeviceEvaluator.RuleId
import com.galaxyjoy.cpuinfo.feat.canmydevice.CanMyDeviceEvaluator.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class CanMyDeviceEvaluatorTest {

    private fun widevine(
        supported: Boolean = true,
        securityLevel: String? = "L1",
        hdcpLevel: String? = "HDCP_V2_2",
        maxHdcpLevel: String? = "HDCP_V2_2",
    ) = DrmSchemeData(
        name = "Widevine",
        supported = supported,
        securityLevel = securityLevel,
        hdcpLevel = hdcpLevel,
        maxHdcpLevel = maxHdcpLevel,
        version = "16.0.0",
    )

    private fun lens(
        hasRawCapture: Boolean? = null,
        maxSlowMotionFps: Int? = null,
    ) = CameraLensData(id = "0", facing = CameraFacing.BACK, hasRawCapture = hasRawCapture, maxSlowMotionFps = maxSlowMotionFps)

    private fun aiResult(tier: AiReadinessEvaluator.Tier) = AiReadinessEvaluator.Result(
        score = 0,
        maxScore = 11,
        tier = tier,
        flags = AiReadinessEvaluator.IsaFlags(
            neonDot = false,
            i8mm = false,
            bf16 = false,
            fp16Arith = false,
            sve = false,
            sve2 = false,
        ),
    )

    private fun snapshot(
        drmSchemes: List<DrmSchemeData> = listOf(widevine()),
        cameraLenses: List<CameraLensData> = listOf(lens()),
        vulkanVersion: String = "1.3.0",
        aiTier: AiReadinessEvaluator.Tier = AiReadinessEvaluator.Tier.CAPABLE,
    ) = CanMyDeviceEvaluator.Snapshot(
        drmSchemes = drmSchemes,
        cameraLenses = cameraLenses,
        vulkanVersion = vulkanVersion,
        aiReadiness = aiResult(aiTier),
    )

    private fun ruleFor(result: CanMyDeviceEvaluator.Result, id: RuleId) = result.rules.first { it.id == id }

    // --- Netflix HD (Widevine security level) ---

    @Test
    fun `Widevine L1 answers YES for Netflix HD`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(drmSchemes = listOf(widevine(securityLevel = "L1"))))
        assertEquals(Verdict.YES, ruleFor(result, RuleId.NETFLIX_HD).verdict)
    }

    @Test
    fun `Widevine L3 answers NO for Netflix HD`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(drmSchemes = listOf(widevine(securityLevel = "L3"))))
        assertEquals(Verdict.NO, ruleFor(result, RuleId.NETFLIX_HD).verdict)
    }

    @Test
    fun `Widevine not supported answers NO for Netflix HD`() {
        val result = CanMyDeviceEvaluator.evaluate(drmSnapshotOnly(widevine(supported = false, securityLevel = null)))
        assertEquals(Verdict.NO, ruleFor(result, RuleId.NETFLIX_HD).verdict)
    }

    @Test
    fun `Widevine L2 answers PARTIAL for Netflix HD`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(drmSchemes = listOf(widevine(securityLevel = "L2"))))
        assertEquals(Verdict.PARTIAL, ruleFor(result, RuleId.NETFLIX_HD).verdict)
    }

    // --- 4K HDCP ---

    @Test
    fun `HDCP V2_2 answers YES for 4K streaming`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(drmSchemes = listOf(widevine(maxHdcpLevel = "HDCP_V2_2"))))
        assertEquals(Verdict.YES, ruleFor(result, RuleId.HDCP_4K).verdict)
    }

    @Test
    fun `HDCP V1 answers NO for 4K streaming`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(drmSchemes = listOf(widevine(maxHdcpLevel = "HDCP_V1", hdcpLevel = "HDCP_V1"))))
        assertEquals(Verdict.NO, ruleFor(result, RuleId.HDCP_4K).verdict)
    }

    @Test
    fun `missing HDCP level answers NO for 4K streaming`() {
        val result = CanMyDeviceEvaluator.evaluate(
            snapshot(drmSchemes = listOf(widevine(maxHdcpLevel = null, hdcpLevel = null))),
        )
        assertEquals(Verdict.NO, ruleFor(result, RuleId.HDCP_4K).verdict)
    }

    @Test
    fun `HDCP falls back to hdcpLevel when maxHdcpLevel is null`() {
        val result = CanMyDeviceEvaluator.evaluate(
            snapshot(drmSchemes = listOf(widevine(maxHdcpLevel = null, hdcpLevel = "HDCP_V2_3"))),
        )
        assertEquals(Verdict.YES, ruleFor(result, RuleId.HDCP_4K).verdict)
    }

    // --- Camera RAW ---

    @Test
    fun `a lens with RAW capture answers YES`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(cameraLenses = listOf(lens(hasRawCapture = true))))
        assertEquals(Verdict.YES, ruleFor(result, RuleId.CAMERA_RAW).verdict)
    }

    @Test
    fun `no lens with RAW capture answers NO`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(cameraLenses = listOf(lens(hasRawCapture = false))))
        assertEquals(Verdict.NO, ruleFor(result, RuleId.CAMERA_RAW).verdict)
    }

    @Test
    fun `unknown RAW capability (null) is treated as NO, not crashing`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(cameraLenses = listOf(lens(hasRawCapture = null))))
        assertEquals(Verdict.NO, ruleFor(result, RuleId.CAMERA_RAW).verdict)
    }

    // --- Slow motion ---

    @Test
    fun `240fps slow motion answers YES`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(cameraLenses = listOf(lens(maxSlowMotionFps = 240))))
        assertEquals(Verdict.YES, ruleFor(result, RuleId.SLOW_MOTION).verdict)
    }

    @Test
    fun `120fps slow motion answers NO`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(cameraLenses = listOf(lens(maxSlowMotionFps = 120))))
        assertEquals(Verdict.NO, ruleFor(result, RuleId.SLOW_MOTION).verdict)
    }

    @Test
    fun `no slow motion mode at all answers NO without crashing`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(cameraLenses = listOf(lens(maxSlowMotionFps = null))))
        assertEquals(Verdict.NO, ruleFor(result, RuleId.SLOW_MOTION).verdict)
    }

    // --- Vulkan gaming ---

    @Test
    fun `Vulkan 1_3 answers YES for heavy gaming`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(vulkanVersion = "1.3.250"))
        assertEquals(Verdict.YES, ruleFor(result, RuleId.VULKAN_GAMING).verdict)
    }

    @Test
    fun `no Vulkan support answers NO for heavy gaming`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(vulkanVersion = "0.0.0"))
        assertEquals(Verdict.NO, ruleFor(result, RuleId.VULKAN_GAMING).verdict)
    }

    @Test
    fun `unparseable Vulkan version string (localized unknown fallback) answers NO`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(vulkanVersion = "Unknown"))
        assertEquals(Verdict.NO, ruleFor(result, RuleId.VULKAN_GAMING).verdict)
    }

    @Test
    fun `Vulkan 1_0 only answers PARTIAL for heavy gaming`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(vulkanVersion = "1.0.3"))
        assertEquals(Verdict.PARTIAL, ruleFor(result, RuleId.VULKAN_GAMING).verdict)
    }

    // --- On-device AI (delegates to AiReadinessEvaluator's tier) ---

    @Test
    fun `ADVANCED AI tier answers YES`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(aiTier = AiReadinessEvaluator.Tier.ADVANCED))
        assertEquals(Verdict.YES, ruleFor(result, RuleId.ON_DEVICE_AI).verdict)
    }

    @Test
    fun `CAPABLE AI tier answers YES`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(aiTier = AiReadinessEvaluator.Tier.CAPABLE))
        assertEquals(Verdict.YES, ruleFor(result, RuleId.ON_DEVICE_AI).verdict)
    }

    @Test
    fun `NOT_READY AI tier answers NO`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(aiTier = AiReadinessEvaluator.Tier.NOT_READY))
        assertEquals(Verdict.NO, ruleFor(result, RuleId.ON_DEVICE_AI).verdict)
    }

    @Test
    fun `BASIC AI tier answers PARTIAL`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot(aiTier = AiReadinessEvaluator.Tier.BASIC))
        assertEquals(Verdict.PARTIAL, ruleFor(result, RuleId.ON_DEVICE_AI).verdict)
    }

    // --- Result shape ---

    @Test
    fun `evaluate always returns exactly 6 rules and yesCount matches`() {
        val result = CanMyDeviceEvaluator.evaluate(snapshot())
        assertEquals(6, result.totalCount)
        assertEquals(result.rules.count { it.verdict == Verdict.YES }, result.yesCount)
    }

    private fun drmSnapshotOnly(scheme: DrmSchemeData) = snapshot(drmSchemes = listOf(scheme))
}
