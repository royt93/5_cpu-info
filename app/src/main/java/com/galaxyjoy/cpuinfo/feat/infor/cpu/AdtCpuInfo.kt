package com.galaxyjoy.cpuinfo.feat.infor.cpu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.ViewHolderCpuFrequencyBinding
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoItems

/** One row of [FrmCpuInfo]'s list — either a live per-core frequency gauge or a plain title/value line. */
sealed interface CpuRow {
    data class FrequencyRow(
        val current: Long,
        val max: Long,
        val currentDescription: String,
        val minDescription: String,
        val maxDescription: String,
    ) : CpuRow

    data class ValueRow(val title: String, val value: String) : CpuRow
}

/**
 * Two view types in one adapter (frequency gauge vs plain title/value) — the one CPU/GPU/RAM
 * screen that doesn't fit the shared [AdtInfoItems] row shape, since per-core frequency needs a
 * progress bar, not a text value. Reuses [AdtInfoItems.SingleItemViewHolder] for the plain rows
 * rather than duplicating that view holder.
 */
class AdtCpuInfo(
    private val items: List<CpuRow>,
    private val onClickListener: AdtInfoItems.OnClickListener?,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is CpuRow.FrequencyRow -> VIEW_TYPE_FREQUENCY
        is CpuRow.ValueRow -> VIEW_TYPE_VALUE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_FREQUENCY -> FrequencyViewHolder(
                ViewHolderCpuFrequencyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )

            else -> AdtInfoItems.SingleItemViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.vi_item_value, parent, false),
                onClickListener,
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = items[position]) {
            is CpuRow.FrequencyRow -> (holder as FrequencyViewHolder).bind(row)
            is CpuRow.ValueRow -> (holder as AdtInfoItems.SingleItemViewHolder).bind(row.title to row.value)
        }
    }

    override fun getItemCount(): Int = items.size

    class FrequencyViewHolder(
        private val binding: ViewHolderCpuFrequencyBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: CpuRow.FrequencyRow) {
            binding.currentFrequency = row.current
            binding.maxFrequency = row.max
            binding.currentFrequencyDescription = row.currentDescription
            binding.minFrequencyDescription = row.minDescription
            binding.maxFrequencyDescription = row.maxDescription
            binding.executePendingBindings()
        }
    }

    companion object {
        private const val VIEW_TYPE_FREQUENCY = 0
        private const val VIEW_TYPE_VALUE = 1
    }
}
