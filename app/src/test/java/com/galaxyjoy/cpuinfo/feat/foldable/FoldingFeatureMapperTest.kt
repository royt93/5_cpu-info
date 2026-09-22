package com.galaxyjoy.cpuinfo.feat.foldable

import androidx.window.layout.FoldingFeature
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class FoldingFeatureMapperTest {

    @Test
    fun `maps a half-opened vertical separating hinge`() {
        val feature: FoldingFeature = mockk()
        every { feature.isSeparating } returns true
        every { feature.occlusionType } returns FoldingFeature.OcclusionType.NONE
        every { feature.orientation } returns FoldingFeature.Orientation.VERTICAL
        every { feature.state } returns FoldingFeature.State.HALF_OPENED

        val result = FoldingFeatureMapper.describe(feature)

        assertEquals(FoldPosture(true, "NONE", "VERTICAL", "HALF_OPENED"), result)
    }

    @Test
    fun `maps a flat horizontal occluding non-separating hinge`() {
        val feature: FoldingFeature = mockk()
        every { feature.isSeparating } returns false
        every { feature.occlusionType } returns FoldingFeature.OcclusionType.FULL
        every { feature.orientation } returns FoldingFeature.Orientation.HORIZONTAL
        every { feature.state } returns FoldingFeature.State.FLAT

        val result = FoldingFeatureMapper.describe(feature)

        assertEquals(FoldPosture(false, "FULL", "HORIZONTAL", "FLAT"), result)
    }
}
