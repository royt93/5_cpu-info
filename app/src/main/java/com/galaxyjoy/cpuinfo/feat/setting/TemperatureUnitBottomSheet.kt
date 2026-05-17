package com.galaxyjoy.cpuinfo.feat.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.setFragmentResult
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme

/**
 * Material You bottom sheet to pick temperature unit (°C / °F / °K).
 * Returns chosen value (matches @array/prefTemperatureValues) via Fragment Result API.
 */
class TemperatureUnitBottomSheet : BaseRoundedBottomSheet() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val initialValue = arguments?.getString(ARG_INITIAL_VALUE).orEmpty()
        setContent {
            CpuInfoTheme {
                TemperatureUnitContent(
                    initialValue = initialValue,
                    onPicked = { value ->
                        setFragmentResult(REQUEST_KEY, Bundle().apply { putString(ARG_VALUE, value) })
                        dismiss()
                    },
                )
            }
        }
    }

    companion object {
        const val TAG = "TemperatureUnitBottomSheet"
        const val REQUEST_KEY = "TemperatureUnit.result"
        const val ARG_VALUE = "value"
        const val ARG_INITIAL_VALUE = "initial_value"

        fun newInstance(currentValue: String): TemperatureUnitBottomSheet =
            TemperatureUnitBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_INITIAL_VALUE, currentValue) }
            }
    }
}

@Composable
private fun TemperatureUnitContent(
    initialValue: String,
    onPicked: (String) -> Unit,
) {
    val names = stringArrayResource(R.array.prefTemperatureNames)
    val values = stringArrayResource(R.array.prefTemperatureValues)
    val pairs = remember(names, values) {
        names.zip(values)
    }

    var selected by rememberSaveable { mutableStateOf(initialValue) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.temperature_unit),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            pairs.forEach { (label, value) ->
                OptionRow(
                    label = label,
                    selected = selected == value,
                    onClick = {
                        selected = value
                        onPicked(value)
                    },
                )
            }
        }
    }
}

@Composable
internal fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.tertiary,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
