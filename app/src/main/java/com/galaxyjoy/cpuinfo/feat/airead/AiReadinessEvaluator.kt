package com.galaxyjoy.cpuinfo.feat.airead

/**
 * Pure scoring logic for F10/U12 "AI Readiness Score" — no Android deps. Combines detected ISA
 * extensions that accelerate on-device ML inference (already parsed by libcpuinfo from
 * /proc/cpuinfo "Features", not newly-read registers) with RAM and core count into a simple,
 * transparent point score. This is a heuristic aid, not a benchmark — surfaced with a clear
 * disclaimer, same as U01/F04.
 */
object AiReadinessEvaluator {

    enum class Tier { NOT_READY, BASIC, CAPABLE, ADVANCED }

    data class IsaFlags(
        val neonDot: Boolean,
        val i8mm: Boolean,
        val bf16: Boolean,
        val fp16Arith: Boolean,
        val sve: Boolean,
        val sve2: Boolean,
    )

    data class Result(
        val score: Int,
        val maxScore: Int,
        val tier: Tier,
        val flags: IsaFlags,
    )

    // Sum of every individual bonus below (neonDot 1 + i8mm 2 + bf16 2 + fp16 1 + sve/sve2 2
    // [non-additive] + ram>=6GB 1 + ram>=8GB 1 + cores>=8 1) — keep in sync with evaluate().
    private const val MAX_SCORE = 11

    fun evaluate(flags: IsaFlags, totalRamBytes: Long, coreCount: Int): Result {
        var score = 0
        if (flags.neonDot) score += 1
        if (flags.i8mm) score += 2
        if (flags.bf16) score += 2
        if (flags.fp16Arith) score += 1
        if (flags.sve || flags.sve2) score += 2

        val ramGb = totalRamBytes / GB
        if (ramGb >= 6) score += 1
        if (ramGb >= 8) score += 1

        if (coreCount >= 8) score += 1

        val tier = when {
            score >= 9 -> Tier.ADVANCED
            score >= 6 -> Tier.CAPABLE
            score >= 3 -> Tier.BASIC
            else -> Tier.NOT_READY
        }

        return Result(score = score, maxScore = MAX_SCORE, tier = tier, flags = flags)
    }

    private const val GB = 1024L * 1024L * 1024L
}
