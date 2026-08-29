package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.hardware.Sensor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R

@Composable
internal fun SensorTestScreen(
    uiState: VMSensorTest.UiState,
    onStartClicked: () -> Unit,
    onSkipClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onShareClicked: (List<SensorTestRunner.StepResult>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.sensor_test_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(20.dp))

        when (uiState) {
            is VMSensorTest.UiState.Idle -> IdleContent(onStartClicked)
            is VMSensorTest.UiState.NoTestableSensors -> NoSensorsContent()
            is VMSensorTest.UiState.Running -> RunningContent(uiState, onSkipClicked)
            is VMSensorTest.UiState.Done -> DoneContent(uiState, onDoneClicked, onShareClicked)
        }
    }
}

@Composable
private fun IdleContent(onStartClicked: () -> Unit) {
    Text(
        text = stringResource(R.string.sensor_test_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onStartClicked, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(text = stringResource(R.string.sensor_test_start_button), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun NoSensorsContent() {
    Text(
        text = stringResource(R.string.sensor_test_no_sensors),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RunningContent(state: VMSensorTest.UiState.Running, onSkipClicked: () -> Unit) {
    Text(
        text = stringResource(R.string.sensor_test_step_label, state.stepIndex + 1, state.totalSteps),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(sensorNameRes(state.sensorType)),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(sensorInstructionRes(state.sensorType)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))

    CircularProgressIndicator()
    Spacer(Modifier.height(12.dp))

    val liveValues = state.liveValues
    Text(
        text = if (liveValues != null) {
            SensorTestEvaluator.formatLiveValue(state.sensorType, liveValues.toFloatArray())
        } else {
            stringResource(R.string.sensor_test_waiting_label)
        },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(24.dp))

    OutlinedButton(onClick = onSkipClicked) {
        Text(text = stringResource(R.string.sensor_test_skip_button))
    }
}

@Composable
private fun DoneContent(
    state: VMSensorTest.UiState.Done,
    onDoneClicked: () -> Unit,
    onShareClicked: (List<SensorTestRunner.StepResult>) -> Unit,
) {
    Text(
        text = stringResource(R.string.sensor_test_done_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(16.dp))

    state.results.forEach { result ->
        SensorResultRow(result)
        Spacer(Modifier.height(10.dp))
    }

    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onShareClicked(state.results) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.sensor_test_share_button), modifier = Modifier.padding(start = 8.dp))
        }
        Button(onClick = onDoneClicked) {
            Text(text = stringResource(R.string.sensor_test_retry_button))
        }
    }
}

@Composable
private fun SensorResultRow(result: SensorTestRunner.StepResult) {
    val (accentColor, labelRes) = when (result.outcome) {
        SensorTestRunner.StepOutcome.DETECTED -> Color(0xFF4CAF50) to R.string.sensor_test_result_detected
        SensorTestRunner.StepOutcome.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant to R.string.sensor_test_result_skipped
        SensorTestRunner.StepOutcome.TIMED_OUT -> MaterialTheme.colorScheme.error to R.string.sensor_test_result_timed_out
        SensorTestRunner.StepOutcome.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant to R.string.sensor_test_result_skipped
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(sensorNameRes(result.sensorType)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
    }
}

private fun sensorNameRes(sensorType: Int): Int = when (sensorType) {
    Sensor.TYPE_ACCELEROMETER -> R.string.sensor_test_name_accelerometer
    Sensor.TYPE_GYROSCOPE -> R.string.sensor_test_name_gyroscope
    Sensor.TYPE_MAGNETIC_FIELD -> R.string.sensor_test_name_magnetometer
    Sensor.TYPE_LIGHT -> R.string.sensor_test_name_light
    Sensor.TYPE_PROXIMITY -> R.string.sensor_test_name_proximity
    Sensor.TYPE_PRESSURE -> R.string.sensor_test_name_pressure
    else -> R.string.sensor_test_title
}

private fun sensorInstructionRes(sensorType: Int): Int = when (sensorType) {
    Sensor.TYPE_ACCELEROMETER -> R.string.sensor_test_instruction_accelerometer
    Sensor.TYPE_GYROSCOPE -> R.string.sensor_test_instruction_gyroscope
    Sensor.TYPE_MAGNETIC_FIELD -> R.string.sensor_test_instruction_magnetometer
    Sensor.TYPE_LIGHT -> R.string.sensor_test_instruction_light
    Sensor.TYPE_PROXIMITY -> R.string.sensor_test_instruction_proximity
    Sensor.TYPE_PRESSURE -> R.string.sensor_test_instruction_pressure
    else -> R.string.sensor_test_intro
}
