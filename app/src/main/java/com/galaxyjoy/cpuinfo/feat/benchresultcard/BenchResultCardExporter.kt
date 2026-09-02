package com.galaxyjoy.cpuinfo.feat.benchresultcard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.allbench.VMAllBench
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/** Renders + shares the U23 benchmark result card — same `FileProvider`/cache-dir mechanism as
 * [com.galaxyjoy.cpuinfo.feat.devicecard.DeviceCardExporter]. */
class BenchResultCardExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchersProvider: DispatchersProvider,
) {

    fun exportBenchResultCard(scope: CoroutineScope, results: VMAllBench.Results) {
        scope.launch {
            val fileUri = withContext(dispatchersProvider.io) {
                val bitmap = BenchResultCardRenderer.render(context, results)
                saveToCache(bitmap)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.bench_result_card_share_button))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun saveToCache(bitmap: Bitmap): Uri {
        val file = File(context.cacheDir, "bench_result_card_${System.nanoTime()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
