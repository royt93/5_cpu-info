package com.galaxyjoy.cpuinfo.feat.canmydevice

import com.galaxyjoy.cpuinfo.domain.model.CameraLensData
import com.galaxyjoy.cpuinfo.domain.model.DrmSchemeData
import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessEvaluator

/**
 * Pure rule logic for U05 "Can My Device?" — no Android deps. Answers concrete user questions
 * ("Netflix HD?", "quay RAW?", "game Vulkan?") from capability data already collected elsewhere
 * in the app (DRM scheme info, camera characteristics, Vulkan version, AI Readiness score) rather
 * than reading any new raw system API. MVP scope: a fixed set of rules, no remote rule-pack.
 */
object CanMyDeviceEvaluator {

    enum class Verdict { YES, NO, PARTIAL }

    enum class RuleId {
        NETFLIX_HD,
        HDCP_4K,
        CAMERA_RAW,
        SLOW_MOTION,
        VULKAN_GAMING,
        ON_DEVICE_AI,
    }

    data class Rule(
        val id: RuleId,
        val verdict: Verdict,
        val reasonDetail: String,
    )

    data class Snapshot(
        val drmSchemes: List<DrmSchemeData>,
        val cameraLenses: List<CameraLensData>,
        val vulkanVersion: String,
        val aiReadiness: AiReadinessEvaluator.Result,
    )

    data class Result(val rules: List<Rule>) {
        val yesCount: Int = rules.count { it.verdict == Verdict.YES }
        val totalCount: Int = rules.size
    }

    fun evaluate(snapshot: Snapshot): Result = Result(
        rules = listOf(
            evaluateNetflixHd(snapshot.drmSchemes),
            evaluateHdcp4k(snapshot.drmSchemes),
            evaluateCameraRaw(snapshot.cameraLenses),
            evaluateSlowMotion(snapshot.cameraLenses),
            evaluateVulkanGaming(snapshot.vulkanVersion),
            evaluateOnDeviceAi(snapshot.aiReadiness),
        ),
    )

    // Widevine L1 = full HD/UHD hardware-protected pipeline (Netflix HD, Disney+ HD, Amazon HD).
    // L2 is a genuine in-between tier (hardware crypto, not full TEE) — some HD streams work,
    // some services still cap it to SD, hence PARTIAL rather than forcing a binary answer.
    // L3 = software-only DRM, streaming services cap it to SD.
    private fun evaluateNetflixHd(schemes: List<DrmSchemeData>): Rule {
        val widevine = schemes.firstOrNull { it.name == "Widevine" }
        val (verdict, reason) = when {
            widevine == null || !widevine.supported ->
                Verdict.NO to "Widevine DRM not supported on this device."
            widevine.securityLevel == "L1" ->
                Verdict.YES to "Widevine L1 — full HD/UHD hardware-protected playback."
            widevine.securityLevel == "L2" ->
                Verdict.PARTIAL to "Widevine L2 — hardware crypto without full TEE; some services still cap playback to SD."
            widevine.securityLevel == "L3" ->
                Verdict.NO to "Widevine L3 — software-only DRM, streaming apps cap playback to SD."
            else ->
                Verdict.NO to "Widevine security level could not be determined (${widevine.securityLevel ?: "unknown"})."
        }
        return Rule(RuleId.NETFLIX_HD, verdict, reason)
    }

    // 4K DRM-protected streams require HDCP >= 2.2 on the output path.
    private fun evaluateHdcp4k(schemes: List<DrmSchemeData>): Rule {
        val widevine = schemes.firstOrNull { it.name == "Widevine" }
        val level = widevine?.maxHdcpLevel ?: widevine?.hdcpLevel
        val rank = HDCP_RANK[level]
        val verdict = if (rank != null && rank >= HDCP_RANK_V2_2) Verdict.YES else Verdict.NO
        val reason = when {
            widevine == null || !widevine.supported -> "Widevine DRM not supported on this device."
            level == null -> "HDCP level not reported by this device."
            verdict == Verdict.YES -> "HDCP level $level meets the 4K DRM requirement (HDCP 2.2+)."
            else -> "HDCP level $level is below the 4K DRM requirement (HDCP 2.2+)."
        }
        return Rule(RuleId.HDCP_4K, verdict, reason)
    }

    // hasRawCapture is nullable in the domain model (null = capability info unavailable for that
    // lens, not "false" — see DataProviderCamera). ponytail: MVP collapses "unavailable" into NO
    // since we can't confirm the feature works; upgrade to a dedicated "unknown" state only if
    // this turns out to misinform users on real devices.
    private fun evaluateCameraRaw(lenses: List<CameraLensData>): Rule {
        val supported = lenses.any { it.hasRawCapture == true }
        val reason = if (supported) {
            "At least one camera lens reports RAW (DNG) capture support."
        } else {
            "No camera lens reports RAW (DNG) capture support."
        }
        return Rule(RuleId.CAMERA_RAW, if (supported) Verdict.YES else Verdict.NO, reason)
    }

    private fun evaluateSlowMotion(lenses: List<CameraLensData>): Rule {
        val maxFps = lenses.mapNotNull { it.maxSlowMotionFps }.maxOrNull() ?: 0
        val verdict = if (maxFps >= SLOW_MOTION_FPS_THRESHOLD) Verdict.YES else Verdict.NO
        val reason = if (maxFps > 0) {
            "Highest high-speed video mode detected: ${maxFps}fps (need ${SLOW_MOTION_FPS_THRESHOLD}fps+ for real slow motion)."
        } else {
            "No high-speed/slow-motion video mode detected."
        }
        return Rule(RuleId.SLOW_MOTION, verdict, reason)
    }

    // Heavy modern game engines (UE5 and similar) generally target Vulkan 1.1+; 1.0-only
    // hardware runs lighter Vulkan titles but is a real, common middle ground on older chips.
    private fun evaluateVulkanGaming(vulkanVersion: String): Rule {
        val (major, minor) = parseVersion(vulkanVersion)
        val (verdict, reason) = when {
            major > 1 || (major == 1 && minor >= 1) ->
                Verdict.YES to "Vulkan $major.$minor supported — meets what most modern heavy-graphics games require."
            major == 1 && minor == 0 ->
                Verdict.PARTIAL to "Vulkan 1.0 only — lighter Vulkan titles run, but some modern engines expect 1.1+."
            else ->
                Verdict.NO to "Vulkan hardware acceleration not detected on this device."
        }
        return Rule(RuleId.VULKAN_GAMING, verdict, reason)
    }

    private fun evaluateOnDeviceAi(aiReadiness: AiReadinessEvaluator.Result): Rule {
        val verdict = when (aiReadiness.tier) {
            AiReadinessEvaluator.Tier.ADVANCED, AiReadinessEvaluator.Tier.CAPABLE -> Verdict.YES
            AiReadinessEvaluator.Tier.BASIC -> Verdict.PARTIAL
            AiReadinessEvaluator.Tier.NOT_READY -> Verdict.NO
        }
        val reason = "AI Readiness score ${aiReadiness.score}/${aiReadiness.maxScore}."
        return Rule(RuleId.ON_DEVICE_AI, verdict, reason)
    }

    /** Parses "major.minor.patch" (as returned by [DataProviderGpu.getVulkanVersion]) into (major, minor); any unparseable input (including the localized "unknown" fallback) is treated as unsupported. */
    private fun parseVersion(version: String): Pair<Int, Int> {
        val parts = version.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return major to minor
    }

    // Rank order matches the documented MediaDrm HDCP level property values (higher = stronger
    // protection). HDCP_NO_DIGITAL_OUTPUT means content never leaves via an unprotected digital
    // path at all, which trivially satisfies any HDCP-version requirement.
    private val HDCP_RANK = mapOf(
        "HDCP_NONE" to 0,
        "HDCP_V1" to 1,
        "HDCP_V2" to 2,
        "HDCP_V2_1" to 3,
        "HDCP_V2_2" to 4,
        "HDCP_V2_3" to 5,
        "HDCP_NO_DIGITAL_OUTPUT" to 100,
    )
    private const val HDCP_RANK_V2_2 = 4
    private const val SLOW_MOTION_FPS_THRESHOLD = 240
}
