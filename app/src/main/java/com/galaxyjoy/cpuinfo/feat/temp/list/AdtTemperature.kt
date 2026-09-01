package com.galaxyjoy.cpuinfo.feat.temp.list

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.galaxyjoy.cpuinfo.R
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
                val temperature = temperatureItem.temperature
                if (temperature == null) {
                    // The value slot's XML style (TextH7, bold) is sized for a short reading like
                    // "32°C" — reusing it for this fallback text used to render an oversized bold
                    // paragraph. Drop to a smaller, non-bold body style for this case only.
                    temperatureTv.setTextAppearance(R.style.TextBody2)
                    temperatureTv.setTypeface(temperatureTv.typeface, Typeface.NORMAL)
                    temperatureTv.text = temperatureTv.context.getString(R.string.temperature_not_supported)
                } else {
                    temperatureTv.setTextAppearance(R.style.TextH7)
                    temperatureTv.setTypeface(temperatureTv.typeface, Typeface.BOLD)
                    temperatureTv.text = temperatureFormatter.format(temperature)
                }
            }
        }
    }
}
