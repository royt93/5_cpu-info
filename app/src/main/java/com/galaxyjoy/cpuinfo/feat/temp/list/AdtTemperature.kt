package com.galaxyjoy.cpuinfo.feat.temp.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.galaxyjoy.cpuinfo.databinding.ViItemTemperatureBinding
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureFormatter

/**
 * Temperature list adapter, fed a fresh list on each change notification from its caller.
 */
class AdtTemperature(
    private val temperatureFormatter: TemperatureFormatter,
    private val temperatureList: List<TemperatureItem>
) : RecyclerView.Adapter<AdtTemperature.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViItemTemperatureBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, temperatureFormatter)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(temperatureList[position])
    }

    override fun getItemCount(): Int = temperatureList.size

    class ViewHolder(
        private val binding: ViItemTemperatureBinding,
        private val temperatureFormatter: TemperatureFormatter
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(temperatureItem: TemperatureItem) {
            with(binding) {
                temperatureIv.setImageResource(temperatureItem.iconRes)
                temperatureTypeTv.text = temperatureItem.name
                if (temperatureItem.temperature == null) {
                    temperatureTv.text =
                        "Your device does not support providing the necessary information, so I am unable to retrieve the temperature."
                } else {
                    temperatureItem.temperature?.let {
                        temperatureTv.text = temperatureFormatter.format(it)
                    }
                }
            }
        }
    }
}
