package com.galaxyjoy.cpuinfo.feat.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 * Material You bottom sheet to pick app theme (light / dark / battery-or-system).
 * Returns chosen value (matches @array/themeEntryArray) via Fragment Result API.
 */
class ThemePickerBottomSheet : BaseRoundedBottomSheet() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val initialValue = arguments?.getString(ARG_INITIAL_VALUE).orEmpty()
        setContent {
            CpuInfoTheme {
                ThemePickerContent(
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
        const val TAG = "ThemePickerBottomSheet"
        const val REQUEST_KEY = "ThemePicker.result"
        const val ARG_VALUE = "value"
        const val ARG_INITIAL_VALUE = "initial_value"

        fun newInstance(currentValue: String): ThemePickerBottomSheet =
            ThemePickerBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_INITIAL_VALUE, currentValue) }
            }
    }
}

@Composable
private fun ThemePickerContent(
    initialValue: String,
    onPicked: (String) -> Unit,
) {
    val names = stringArrayResource(R.array.themeListArray)
    val values = stringArrayResource(R.array.themeEntryArray)
    val pairs = remember(names, values) { names.zip(values) }

    var selected by rememberSaveable { mutableStateOf(initialValue) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.pref_theme_choose),
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
