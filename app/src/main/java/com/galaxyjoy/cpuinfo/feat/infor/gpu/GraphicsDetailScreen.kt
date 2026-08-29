package com.galaxyjoy.cpuinfo.feat.infor.gpu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.domain.model.GpuData

@Composable
internal fun GraphicsDetailBar(
    gpuData: GpuData,
    extensionCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.graphics_detail_bar_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.graphics_detail_bar_value,
                    gpuData.glesVersion,
                    extensionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GraphicsDetailBottomSheet(
    gpuData: GpuData,
    vulkanCapability: GraphicsDetailProvider.VulkanCapability,
    extensions: List<String>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.graphics_detail_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            CapabilityRow(stringResource(R.string.gles_version), gpuData.glesVersion)
            CapabilityRow(stringResource(R.string.vulkan_version), gpuData.vulkanVersion)
            CapabilityRow(
                stringResource(R.string.graphics_detail_vulkan_hardware_level),
                if (vulkanCapability.hardwareLevel >= 0) {
                    vulkanCapability.hardwareLevel.toString()
                } else {
                    stringResource(R.string.unknown)
                },
            )
            CapabilityRow(
                stringResource(R.string.graphics_detail_vulkan_compute),
                stringResource(
                    if (vulkanCapability.computeSupported) R.string.yes else R.string.no,
                ),
            )
            if (gpuData.glVendor != null) {
                CapabilityRow(stringResource(R.string.vendor), gpuData.glVendor)
            }
            if (gpuData.glRenderer != null) {
                CapabilityRow(stringResource(R.string.renderer), gpuData.glRenderer)
            }

            Spacer(Modifier.padding(top = 8.dp))
            Text(
                text = stringResource(R.string.graphics_detail_extensions_title, extensions.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (extensions.isEmpty()) {
                Text(
                    text = stringResource(R.string.graphics_detail_no_extensions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(items = extensions, key = { it }) { extension ->
                        ExtensionRow(extension)
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ExtensionRow(extension: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = extension,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
