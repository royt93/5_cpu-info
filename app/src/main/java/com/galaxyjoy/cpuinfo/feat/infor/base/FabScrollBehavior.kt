package com.galaxyjoy.cpuinfo.feat.infor.base

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

/**
 * Shrinks [fab] to an icon-only pill while the list scrolls, extends it back to its full label
 * at rest — keeps a full-width extended FAB from permanently dominating the screen while still
 * surfacing its label whenever the user isn't actively scrolling. Shared by every info-tab FAB
 * (CPU/Sensors) so they all animate the same way instead of each rolling its own listener.
 */
fun RecyclerView.shrinkFabOnScroll(fab: ExtendedFloatingActionButton) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy > 0) {
                fab.shrink()
            } else if (dy < 0) {
                fab.extend()
            }
        }
    })
}
