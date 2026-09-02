package com.galaxyjoy.cpuinfo.feat.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.setFragmentResult
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import com.galaxyjoy.cpuinfo.util.SystemInfoExporter.Format

/**
 * Material You bottom sheet to pick export format (plain text vs JSON).
 * Returns chosen [Format.name] via Fragment Result API under [REQUEST_KEY] / [ARG_FORMAT].
 */
class ExportFormatBottomSheet : BaseRoundedBottomSheet() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val initialName = arguments?.getString(ARG_INITIAL_FORMAT)
        val initialFormat = runCatching { Format.valueOf(initialName.orEmpty()) }
            .getOrDefault(Format.TEXT)
        setContent {
            CpuInfoTheme {
                ExportFormatContent(
                    initial = initialFormat,
                    onPicked = { format ->
                        setFragmentResult(
                            REQUEST_KEY,
                            Bundle().apply { putString(ARG_FORMAT, format.name) },
                        )
                        dismiss()
                    },
                )
            }
        }
    }

    companion object {
        const val TAG = "ExportFormatBottomSheet"
        const val REQUEST_KEY = "ExportFormat.result"
        const val ARG_FORMAT = "format"
        const val ARG_INITIAL_FORMAT = "initial_format"

        fun newInstance(initialFormat: Format): ExportFormatBottomSheet =
            ExportFormatBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_INITIAL_FORMAT, initialFormat.name) }
            }
    }
}

@Composable
private fun ExportFormatContent(initial: Format, onPicked: (Format) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(initial) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.export_format_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            FormatRow(
                label = stringResource(R.string.export_as_text),
                selected = selected == Format.TEXT,
                onClick = {
                    selected = Format.TEXT
                    onPicked(Format.TEXT)
                },
            )
            FormatRow(
                label = stringResource(R.string.export_as_json),
                selected = selected == Format.JSON,
                onClick = {
                    selected = Format.JSON
                    onPicked(Format.JSON)
                },
            )
            FormatRow(
                label = stringResource(R.string.export_as_image),
                selected = selected == Format.IMAGE,
                onClick = {
                    selected = Format.IMAGE
                    onPicked(Format.IMAGE)
                },
            )
        }
    }
}

@Composable
private fun FormatRow(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
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
