package com.galaxyjoy.cpuinfo.feat.foldable

import androidx.window.layout.FoldingFeature

data class FoldPosture(
    val isSeparating: Boolean,
    val occlusionType: String,
    val orientation: String,
    val state: String,
)

/**
 * Pure mapping from androidx.window's [FoldingFeature] (its `State`/`Orientation`/`OcclusionType`
 * are not real Kotlin enums — no exhaustive `when`, hence the `==` checks) into plain strings for
 * display. Kept separate from the live `WindowInfoTracker` collection (inline in
 * [FoldableDisplayBottomSheet] — a 1-line `Flow` chain didn't earn its own repository class) so the
 * mapping itself is unit-testable without a real windowing environment.
 */
internal object FoldingFeatureMapper {

    fun describe(feature: FoldingFeature): FoldPosture = FoldPosture(
        isSeparating = feature.isSeparating,
        occlusionType = if (feature.occlusionType == FoldingFeature.OcclusionType.FULL) "FULL" else "NONE",
        orientation = if (feature.orientation == FoldingFeature.Orientation.VERTICAL) "VERTICAL" else "HORIZONTAL",
        state = if (feature.state == FoldingFeature.State.HALF_OPENED) "HALF_OPENED" else "FLAT",
    )
}
