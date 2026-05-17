package com.galaxyjoy.cpuinfo.feat.vip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.setFragmentResult
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme

/**
 * BottomSheet xác nhận revoke VIP — thay AlertDialog xấu.
 *
 * Return result qua FragmentResult API:
 *   - REQUEST_KEY: [REQUEST_KEY]
 *   - ARG_CONFIRMED: Boolean (true nếu user confirm revoke, false nếu cancel)
 */
class VipRevokeConfirmBottomSheet : BaseRoundedBottomSheet() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                RevokeConfirmContent(
                    onCancel = {
                        emitResult(false)
                        dismiss()
                    },
                    onConfirm = {
                        emitResult(true)
                        dismiss()
                    },
                )
            }
        }
    }

    private fun emitResult(confirmed: Boolean) {
        setFragmentResult(
            REQUEST_KEY,
            Bundle().apply { putBoolean(ARG_CONFIRMED, confirmed) },
        )
    }

    companion object {
        const val TAG = "VipRevokeConfirmBottomSheet"
        const val REQUEST_KEY = "VipRevoke.result"
        const val ARG_CONFIRMED = "confirmed"
    }
}

@Composable
private fun RevokeConfirmContent(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.vip_revoke_all_confirm_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.vip_revoke_all_confirm_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}
