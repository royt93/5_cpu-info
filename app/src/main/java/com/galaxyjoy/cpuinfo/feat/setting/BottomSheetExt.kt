package com.galaxyjoy.cpuinfo.feat.setting

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Force the bottom sheet to open fully expanded and stay expanded — avoids the
 * default half-expanded peek that crops Material You row content awkwardly.
 */
internal fun BottomSheetDialogFragment.expandFully() {
    val sheet = (dialog as? BottomSheetDialog)?.behavior ?: return
    sheet.state = BottomSheetBehavior.STATE_EXPANDED
    sheet.skipCollapsed = true
}
