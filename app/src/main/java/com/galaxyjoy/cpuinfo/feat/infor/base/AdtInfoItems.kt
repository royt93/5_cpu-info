package com.galaxyjoy.cpuinfo.feat.infor.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.isNightMode
import com.galaxyjoy.cpuinfo.util.resolveAccentColor

/**
 * Adapter for all info items inside [BaseRvFragment]. It should support two types of layouts:
 * vertical and horizontal. It also change color of the first string from passed [Pair] to red if
 * the second one is empty.
 *
 */
class AdtInfoItems(
    private val itemsObservableList: List<Pair<String, String>>,
    private val layoutType: LayoutType = LayoutType.HORIZONTAL_LAYOUT,
    private val onClickListener: OnClickListener? = null
) : RecyclerView.Adapter<AdtInfoItems.SingleItemViewHolder>() {

    enum class LayoutType {
        HORIZONTAL_LAYOUT, VERTICAL_LAYOUT
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SingleItemViewHolder {
        val layout = if (layoutType == LayoutType.HORIZONTAL_LAYOUT)
            R.layout.vi_item_value else R.layout.vi_item_value_vertical
        return SingleItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                layout, parent,
                false
            ), onClickListener
        )
    }

    override fun onBindViewHolder(
        holder: SingleItemViewHolder,
        position: Int
    ) {
        holder.bind(itemsObservableList[position])
    }

    override fun getItemCount(): Int = itemsObservableList.size

    class SingleItemViewHolder(
        itemView: View,
        onClickListener: OnClickListener?
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleTv: TextView = itemView.findViewById(R.id.titleTv)
        private val valueTv: TextView = itemView.findViewById(R.id.valueTv)
        private var item: Pair<String, String>? = null

        init {
            itemView.findViewById<LinearLayoutCompat>(R.id.itemContainer)
                .setOnLongClickListener { _ ->
                    item?.let {
                        if (it.second.isNotEmpty()) {
                            onClickListener?.onItemLongPressed(it)
                            return@setOnLongClickListener true
                        }
                    }
                    false
                }
        }

        fun bind(item: Pair<String, String>) {
            this.item = item
            titleTv.text = item.first
            valueTv.text = item.second

            if (item.second.isEmpty()) {
                // R.color.accent was static — Material You audit found this the single shared
                // spot behind "still cyan" complaints across nearly every info tab (CPU/RAM/
                // Android/Audio/Battery/...), since they all render section headers through this
                // one adapter.
                val ctx = titleTv.context
                titleTv.setTextColor(ctx.resolveAccentColor(ctx.isNightMode()))
                valueTv.visibility = View.GONE
            } else {
                titleTv.setTextColor(
                    ContextCompat.getColor(
                        titleTv.context,
                        R.color.onSurface
                    )
                )
                valueTv.visibility = View.VISIBLE
            }
        }
    }

    interface OnClickListener {

        /**
         * Invoked when user use long press on item
         */
        fun onItemLongPressed(item: Pair<String, String>)
    }
}
