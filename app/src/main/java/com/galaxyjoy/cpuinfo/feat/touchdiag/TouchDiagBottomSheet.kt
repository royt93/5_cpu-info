package com.galaxyjoy.cpuinfo.feat.touchdiag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * E11 "Interactive Touchscreen Diagnostic" — static axis-range capabilities
 * ([TouchCapabilityProvider], from [android.view.InputDevice]) plus a live multi-touch canvas.
 *
 * Scoped honestly against the original idea (see `doc/task/epic-05-new-ideas.md` E11): the grid
 * uses Compose's own `pointerInput`/`PointerInputChange` (position + pressure + historical
 * timestamps) rather than raw `MotionEvent` — Compose's pointer API doesn't expose per-pointer
 * touch-major/minor or a platform "max touch points" (there is no public API for the latter at
 * all, verified against `android-37/android.jar`), so the circle size here is pressure-driven and
 * "max simultaneous touches" is whatever was actually observed this session, not a claimed
 * hardware limit.
 */
@AndroidEntryPoint
class TouchDiagBottomSheet : BaseRoundedBottomSheet() {

    private val vm: VMTouchDiag by viewModels()

    // User is often holding still to read capability rows between touches — without this the
    // screen can time out and sleep mid-diagnostic. No screen in the app sets this flag anywhere
    // else (checked — not even the long-running benchmarks), scoped narrowly to just this sheet.
    override fun onStart() {
        super.onStart()
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                TouchDiagContent(
                    uiState = uiState,
                    onFrame = vm::onFrame,
                    onTouchEnd = vm::onTouchEnd,
                    onResetSession = vm::onResetSession,
                )
            }
        }
    }

    companion object {
        const val TAG = "TouchDiagBottomSheet"
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun TouchDiagContent(
    uiState: TouchDiagUiState,
    onFrame: (points: List<TouchPoint>, historicalTimestampsMillis: List<Long>) -> Unit,
    onTouchEnd: () -> Unit,
    onResetSession: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.touch_diag_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = stringResource(R.string.touch_diag_capabilities_section),
                style = MaterialTheme.typography.titleMedium,
            )
            val caps = uiState.capabilities
            if (caps == null) {
                Text(stringResource(R.string.touch_diag_no_capabilities))
            } else {
                Text(stringResource(R.string.touch_diag_device_name, caps.deviceName))
                CapabilityRow(R.string.touch_diag_pressure_range, caps.pressureRange)
                CapabilityRow(R.string.touch_diag_size_range, caps.sizeRange)
                CapabilityRow(R.string.touch_diag_touch_major_range, caps.touchMajorRange)
                CapabilityRow(R.string.touch_diag_orientation_range, caps.orientationRange)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = stringResource(R.string.touch_diag_active_points, uiState.activePoints.size),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(stringResource(R.string.touch_diag_max_observed, uiState.maxPointersObserved))
            Text(
                uiState.sampleRateHz?.let { stringResource(R.string.touch_diag_sample_rate, it) }
                    ?: stringResource(R.string.touch_diag_sample_rate_unavailable),
            )

            Button(onClick = onResetSession, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.touch_diag_reset_button))
            }

            Text(
                text = stringResource(R.string.touch_diag_canvas_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )

            val accentColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val points = event.changes
                                    .filter { it.pressed }
                                    .map { change ->
                                        TouchPoint(
                                            id = change.id.value.toInt(),
                                            x = change.position.x,
                                            y = change.position.y,
                                            pressure = change.pressure,
                                        )
                                    }
                                val timestamps = event.changes.flatMap { change ->
                                    change.historical.map { it.uptimeMillis } + change.uptimeMillis
                                }
                                if (points.isEmpty()) onTouchEnd() else onFrame(points, timestamps)
                            }
                        }
                    },
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        uiState.activePoints.forEach { point ->
                            val radius = (20f + point.pressure * 40f).coerceIn(20f, 80f)
                            drawCircle(
                                color = accentColor,
                                radius = radius,
                                center = Offset(point.x, point.y),
                                style = Stroke(width = 6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(labelRes: Int, range: ClosedFloatingPointRange<Float>?) {
    val text = if (range == null) {
        stringResource(labelRes, "—")
    } else {
        // Always render with a "." decimal point regardless of the device's default locale — same
        // fix as SystemInfoExporter.formatTwoDecimals (B26): a Vietnamese/German/Arabic default
        // locale can flip "." to "," or use non-Western digits, breaking this string's layout.
        stringResource(labelRes, "%.2f – %.2f".format(java.util.Locale.US, range.start, range.endInclusive))
    }
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
