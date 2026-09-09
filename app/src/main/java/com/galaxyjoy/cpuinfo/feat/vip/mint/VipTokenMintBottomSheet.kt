package com.galaxyjoy.cpuinfo.feat.vip.mint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import com.roy.sdkadbmob.AdManager

/**
 * DEBUG-ONLY VIP token minting tool (AD_PROMPT_AOS.MD chú thích 3: "dựng 1 app Android nội bộ
 * nhỏ riêng" — thay vì 1 project riêng, gọn hơn khi đặt ngay trong app chính dưới dạng màn hình
 * chỉ build vào debug). Long-press nút "Đổi mã" ở màn VIP để mở (xem FVipManagement).
 *
 * Private key KHÔNG bao giờ lưu trong app (BuildConfig hay bất kỳ đâu) — dev tự dán tay mỗi lần
 * đúc token, lấy từ `ads.properties` (myKeyStore, field `vipTokenPrivateKey`) hoặc password
 * manager riêng. Đúc xong đóng sheet là mất, không có state nào được ghi lại.
 */
class VipTokenMintBottomSheet : BaseRoundedBottomSheet() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                VipTokenMintContent(
                    onGenerate = { privateKey, days ->
                        AdManager.generateVipToken(privateKeyBase64 = privateKey, days = days)
                    },
                )
            }
        }
    }

    companion object {
        const val TAG = "VipTokenMintBottomSheet"
    }
}

@Composable
internal fun VipTokenMintContent(onGenerate: (privateKey: String, days: Int) -> String) {
    var privateKey by remember { mutableStateOf("") }
    var daysText by remember { mutableStateOf("30") }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val clipboard: ClipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text(
            "Mint VIP token (debug only)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Private key KHÔNG lưu ở đâu cả — dán tay từ ads.properties (myKeyStore) mỗi lần dùng.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = privateKey,
            onValueChange = { privateKey = it; result = null; error = null },
            label = { Text("Private key (Base64)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = daysText,
            onValueChange = { daysText = it; result = null; error = null },
            label = { Text("Số ngày") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val days = daysText.toIntOrNull()
                if (privateKey.isBlank() || days == null || days <= 0) {
                    error = "Nhập private key + số ngày hợp lệ (> 0)"
                    result = null
                    return@Button
                }
                val token = onGenerate(privateKey.trim(), days)
                if (token.isBlank()) {
                    error = "Đúc token thất bại — sai private key, hoặc build này không phải debug"
                    result = null
                } else {
                    error = null
                    result = token
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Đúc token")
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        result?.let { token ->
            Spacer(Modifier.height(16.dp))
            Text("Token (gửi cho khách):", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            SelectionContainer {
                Text(token, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(token)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Copy")
            }
        }
    }
}
