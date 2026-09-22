package com.galaxyjoy.cpuinfo.feat.foldable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ext.findActivity
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * E08 "Foldable / Multi-Display & Hinge Posture" — see `doc/task/epic-05-new-ideas.md`. Multi-
 * display list is a one-shot read ([DisplayInventoryProvider], rarely changes mid-session); hinge
 * posture is collected live via `WindowInfoTracker` because it genuinely can change while the
 * sheet is open on a real foldable.
 */
@AndroidEntryPoint
class FoldableDisplayBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var displayInventoryProvider: DisplayInventoryProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val displays = displayInventoryProvider.listDisplays()

        setContent {
            CpuInfoTheme {
                val activity = LocalContext.current.findActivity()
                var foldPostures by remember { mutableStateOf<List<FoldPosture>>(emptyList()) }
                LaunchedEffect(Unit) {
                    WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { info ->
                        foldPostures = info.displayFeatures
                            .filterIsInstance<FoldingFeature>()
                            .map(FoldingFeatureMapper::describe)
                    }
                }
                FoldableDisplayContent(displays = displays, foldPostures = foldPostures)
            }
        }
    }

    companion object {
        const val TAG = "FoldableDisplayBottomSheet"
    }
}

@Composable
internal fun FoldableDisplayContent(displays: List<DisplayInfo>, foldPostures: List<FoldPosture>) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.foldable_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = stringResource(R.string.foldable_displays_section, displays.size),
                style = MaterialTheme.typography.titleMedium,
            )
            displays.forEach { display ->
                Text(
                    stringResource(
                        R.string.foldable_display_row,
                        display.name,
                        display.widthPx,
                        display.heightPx,
                        display.densityDpi,
                        display.state,
                    ) + if (display.isDefault) " " + stringResource(R.string.foldable_default_display_suffix) else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = stringResource(R.string.foldable_hinge_section),
                style = MaterialTheme.typography.titleMedium,
            )
            if (foldPostures.isEmpty()) {
                Text(stringResource(R.string.foldable_no_hinge))
            } else {
                val yesLabel = stringResource(R.string.yes)
                val noLabel = stringResource(R.string.no)
                foldPostures.forEach { posture ->
                    Text(
                        stringResource(
                            R.string.foldable_hinge_row,
                            posture.state,
                            posture.orientation,
                            posture.occlusionType,
                            if (posture.isSeparating) yesLabel else noLabel,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
