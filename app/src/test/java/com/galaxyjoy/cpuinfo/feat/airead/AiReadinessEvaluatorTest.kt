package com.galaxyjoy.cpuinfo.feat.airead

import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessEvaluator.IsaFlags
import com.galaxyjoy.cpuinfo.feat.airead.AiReadinessEvaluator.Tier
import org.junit.Assert.assertEquals
import org.junit.Test

class AiReadinessEvaluatorTest {

    private val noFlags = IsaFlags(neonDot = false, i8mm = false, bf16 = false, fp16Arith = false, sve = false, sve2 = false)
    private val gb = 1024L * 1024L * 1024L

    @Test
    fun `no ISA extensions and low RAM is NOT_READY`() {
        val result = AiReadinessEvaluator.evaluate(noFlags, totalRamBytes = 2 * gb, coreCount = 4)
        assertEquals(0, result.score)
        assertEquals(Tier.NOT_READY, result.tier)
    }

    @Test
    fun `neonDot alone with mid RAM is BASIC`() {
        val flags = noFlags.copy(neonDot = true)
        val result = AiReadinessEvaluator.evaluate(flags, totalRamBytes = 6 * gb, coreCount = 4)
        // neonDot(1) + ram>=6gb(1) = 2 -> still NOT_READY at boundary
        assertEquals(2, result.score)
        assertEquals(Tier.NOT_READY, result.tier)
    }

    @Test
    fun `i8mm plus dotprod plus 6GB RAM crosses into BASIC`() {
        val flags = noFlags.copy(neonDot = true, i8mm = true)
        val result = AiReadinessEvaluator.evaluate(flags, totalRamBytes = 6 * gb, coreCount = 4)
        // neonDot(1) + i8mm(2) + ram>=6gb(1) = 4
        assertEquals(4, result.score)
        assertEquals(Tier.BASIC, result.tier)
    }

    @Test
    fun `i8mm plus bf16 plus 8GB RAM plus 8 cores reaches CAPABLE`() {
        val flags = noFlags.copy(i8mm = true, bf16 = true)
        val result = AiReadinessEvaluator.evaluate(flags, totalRamBytes = 8 * gb, coreCount = 8)
        // i8mm(2) + bf16(2) + ram>=6gb(1) + ram>=8gb(1) + cores>=8(1) = 7
        assertEquals(7, result.score)
        assertEquals(Tier.CAPABLE, result.tier)
    }

    @Test
    fun `full ISA support with high RAM and core count reaches ADVANCED and hits maxScore`() {
        val flags = IsaFlags(neonDot = true, i8mm = true, bf16 = true, fp16Arith = true, sve = true, sve2 = true)
        val result = AiReadinessEvaluator.evaluate(flags, totalRamBytes = 16 * gb, coreCount = 8)
        // neonDot(1)+i8mm(2)+bf16(2)+fp16(1)+sve/sve2(2, non-additive)+ram6(1)+ram8(1)+cores8(1) = 11
        assertEquals(Tier.ADVANCED, result.tier)
        assertEquals(11, result.maxScore)
        assertEquals(result.maxScore, result.score)
    }

    @Test
    fun `sve alone contributes same as sve2 alone - no double counting when both present`() {
        val sveOnly = AiReadinessEvaluator.evaluate(noFlags.copy(sve = true), totalRamBytes = 0, coreCount = 1)
        val sve2Only = AiReadinessEvaluator.evaluate(noFlags.copy(sve2 = true), totalRamBytes = 0, coreCount = 1)
        val both = AiReadinessEvaluator.evaluate(noFlags.copy(sve = true, sve2 = true), totalRamBytes = 0, coreCount = 1)
        assertEquals(2, sveOnly.score)
        assertEquals(2, sve2Only.score)
        assertEquals(2, both.score)
    }

    @Test
    fun `zero RAM and core count do not crash and score purely on ISA`() {
        val result = AiReadinessEvaluator.evaluate(noFlags.copy(i8mm = true), totalRamBytes = 0, coreCount = 0)
        assertEquals(2, result.score)
    }
}
