package com.galaxyjoy.cpuinfo.feat.infor.base

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import com.galaxyjoy.cpuinfo.R
import com.google.android.material.snackbar.Snackbar

/**
 * Shared "long-press a row to copy its value" behavior, previously duplicated identically across
 * [BaseRvFragment] and the 4 info fragments (Sensor/RAM/GPU/CPU) that can't extend it because they
 * need extra views beyond a bare RecyclerView (FAB, Compose overlay, etc).
 */
fun Fragment.copyToClipboardAndNotify(container: View, text: String) {
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(requireContext().getString(R.string.app_name), text)
    clipboard.setPrimaryClip(clip)
    Snackbar.make(container, R.string.text_copied, Snackbar.LENGTH_SHORT).show()
}
