package com.galaxyjoy.cpuinfo.feat.devicecard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Renders + shares the U14 device card. Saved to [Context.getCacheDir] and shared via the
 * `FileProvider` already configured for the app (`@xml/file_paths`'s `cache-path`) — same
 * mechanism `feat/storagebench`/`feat/rambench` would use if they ever needed to share a file
 * instead of plain text, just applied to an image here.
 */
class DeviceCardExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceCardProvider: DeviceCardProvider,
    private val dispatchersProvider: DispatchersProvider,
) {

    fun exportDeviceCard(scope: CoroutineScope) {
        scope.launch {
            val fileUri = withContext(dispatchersProvider.io) {
                val data = deviceCardProvider.build()
                val bitmap = DeviceCardRenderer.render(context, data)
                saveToCache(bitmap)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.device_card_share_button))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun saveToCache(bitmap: Bitmap): android.net.Uri {
        val file = File(context.cacheDir, "device_card_${System.nanoTime()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
