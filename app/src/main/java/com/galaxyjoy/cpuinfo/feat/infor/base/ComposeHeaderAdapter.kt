package com.galaxyjoy.cpuinfo.feat.infor.base

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView

/**
 * Single-item [RecyclerView.Adapter] that hosts a Composable as the list's first row via
 * [androidx.recyclerview.widget.ConcatAdapter], instead of a separate sticky `ComposeView`
 * sibling above the `RecyclerView`. Lets the header scroll away with the rest of the content
 * through the RecyclerView's own scroll (so [shrinkFabOnScroll], which listens for real
 * RecyclerView scroll deltas, keeps working unchanged).
 */
class ComposeHeaderAdapter(
    private val content: @Composable () -> Unit,
) : RecyclerView.Adapter<ComposeHeaderAdapter.ViewHolder>() {

    class ViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        return ViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.composeView.setContent(content)
    }

    override fun getItemCount(): Int = 1
}
