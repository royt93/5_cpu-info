package com.galaxyjoy.cpuinfo.feat.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.setFragmentResult
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import com.galaxyjoy.cpuinfo.util.LocaleManager

/**
 * Material You bottom sheet to pick app language. Returns the chosen BCP-47 tag via
 * Fragment Result API under key [REQUEST_KEY] / arg [ARG_TAG]. Empty string means
 * "follow system default".
 */
class LanguagePickerBottomSheet : BaseRoundedBottomSheet() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                LanguagePickerContent(
                    currentTag = LocaleManager.currentTag(),
                    onPicked = { tag ->
                        setFragmentResult(REQUEST_KEY, Bundle().apply { putString(ARG_TAG, tag) })
                        dismiss()
                    },
                )
            }
        }
    }

    companion object {
        const val TAG = "LanguagePickerBottomSheet"
        const val REQUEST_KEY = "LanguagePicker.result"
        const val ARG_TAG = "tag"
    }
}

@Composable
private fun LanguagePickerContent(
    currentTag: String,
    onPicked: (String) -> Unit,
) {
    var selectedTag by rememberSaveable { mutableStateOf(currentTag) }
    val options = remember { LocaleManager.SUPPORTED_LOCALES }

    // Surface establishes (1) the rounded clip + (2) LocalContentColor = onSurface so
    // titleLarge Text inherits the correct on-surface color in both light + dark modes.
    // Without this wrapper, LocalContentColor defaults to Color.Black → title invisible
    // on dark surface.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.language_picker_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                items(options, key = { it.tag.ifEmpty { "system" } }) { option ->
                    LanguageRow(
                        option = option,
                        selected = option.tag == selectedTag,
                        onClick = {
                            selectedTag = option.tag
                            onPicked(option.tag)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(
    option: LocaleManager.LocaleOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = when (option.displayKey) {
        LocaleManager.DisplayKey.SystemDefault -> stringResource(R.string.language_system_default)
        null -> option.nativeName.orEmpty()
    }
    // RadioButton already reserves a 48dp touch target on its own — extra vertical padding
    // here was stacking on top of that (14dp*2 = 28dp), making each row ~76dp tall. 6dp keeps
    // rows comfortably tappable (~60dp) without the excess whitespace between options.
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                // Use tertiary instead of primary: in this app's Compose dark palette
                // primary is near-black (#1C1C1C), which would be invisible on the
                // dark surface (#191C1D). Tertiary is peach in dark / brand teal in light.
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
