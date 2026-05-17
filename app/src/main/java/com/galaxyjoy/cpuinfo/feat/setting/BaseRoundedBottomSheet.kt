package com.galaxyjoy.cpuinfo.feat.setting

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

/**
 * Base for all in-app bottom sheets. Forces:
 *  - 16dp rounded top corners (built at runtime via MaterialShapeDrawable so we don't
 *    rely on the theme parent — works regardless of M2 vs M3 base theme).
 *  - Fully expanded on open, skip half-collapsed state.
 *  - Surface color matching app theme + elevation overlay for dark mode.
 *
 * Subclass and override [onCreateView] with ComposeView content.
 */
abstract class BaseRoundedBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val container = dialog.findViewById<android.view.View>(R.id.design_bottom_sheet)
                ?: return@setOnShowListener

            // Resolve ?attr/colorSurface from the activity's theme
            val ctx = container.context
            val surfaceColor = ctx.resolveColor(R.attr.colorSurface)

            val shape = ShapeAppearanceModel.builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, dpToPx(CORNER_DP))
                .setTopRightCorner(CornerFamily.ROUNDED, dpToPx(CORNER_DP))
                .setBottomLeftCorner(CornerFamily.ROUNDED, 0f)
                .setBottomRightCorner(CornerFamily.ROUNDED, 0f)
                .build()

            // Plain MaterialShapeDrawable — NOT createWithElevationOverlay, which would
            // lighten the surface in dark mode and break Compose-text contrast against
            // the Compose theme's onSurface.
            container.background = MaterialShapeDrawable(shape).apply {
                fillColor = ColorStateList.valueOf(surfaceColor)
            }

            BottomSheetBehavior.from(container).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
        }
        return dialog
    }

    private fun android.content.Context.resolveColor(@AttrRes attr: Int): Int {
        val tv = TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return if (tv.resourceId != 0) {
            ContextCompat.getColor(this, tv.resourceId)
        } else {
            tv.data
        }
    }

    private fun dpToPx(dp: Int): Float =
        dp * resources.displayMetrics.density

    companion object {
        private const val CORNER_DP = 16
    }
}
