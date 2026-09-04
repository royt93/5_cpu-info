package com.galaxyjoy.cpuinfo.feat.devicereport

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.allbench.VMAllBench
import com.galaxyjoy.cpuinfo.feat.benchhistory.BenchHistoryExporter
import com.galaxyjoy.cpuinfo.feat.benchresultcard.BenchResultCardRenderer
import com.galaxyjoy.cpuinfo.feat.devicecard.DeviceCardProvider
import com.galaxyjoy.cpuinfo.feat.devicecard.DeviceCardRenderer
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchmark
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmark
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmark
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * U28 — 1-tap bundle of everything the app can already export separately: the U14 device card
 * (always available, no benchmark needed), the U23 bench result card (only if all 4 benchmark
 * types have a saved result — see [buildCombinedBenchResultsOrNull]), and the U25 CSV history
 * (always available, empty sections if nothing was ever run). Zipped into one file rather than
 * `ACTION_SEND_MULTIPLE` — that intent action doesn't reliably carry both image streams AND a
 * large text body together across share targets, a single zip attachment does.
 */
class DeviceReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceCardProvider: DeviceCardProvider,
    private val throttlePrefs: ThrottleResultPrefs,
    private val storagePrefs: StorageBenchResultPrefs,
    private val ramPrefs: RamBenchResultPrefs,
    private val gpuPrefs: GpuBenchResultPrefs,
    private val dispatchersProvider: DispatchersProvider,
) {

    fun exportFullReport(scope: CoroutineScope) {
        scope.launch {
            val zipUri = withContext(dispatchersProvider.io) { buildZip() }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, zipUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.device_report_share_chooser_title))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    internal fun buildZip(): Uri {
        val file = File(context.cacheDir, "device_report_${System.nanoTime()}.zip")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            writeBitmapEntry(zip, "device_card.png", DeviceCardRenderer.render(context, deviceCardProvider.build()))
            buildCombinedBenchResultsOrNull()?.let { results ->
                writeBitmapEntry(zip, "bench_result_card.png", BenchResultCardRenderer.render(context, results))
            }
            val csv = BenchHistoryExporter(throttlePrefs, storagePrefs, ramPrefs, gpuPrefs).buildCsv()
            writeTextEntry(zip, "benchmark_history.csv", csv)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Reconstructs a [VMAllBench.Results] from each benchmark's independently-latest saved run
     * (there's no persisted "1 full AllBench session" record to read back — `BenchResultCardExporter`
     * only ever renders from a just-completed in-memory [VMAllBench.UiState.Done]). `null` if any
     * of the 4 has never been run — a card with only some benchmarks isn't a real "bench result".
     * Fields [BenchResultCardRenderer.render] never reads (throttle's `throttled`/`startTempC`/
     * `durationMs`/`aborted`/`abortReason`, gpu's `frameCount`/`durationMs`) get harmless
     * placeholder values — `*ResultPrefs.SavedResult` doesn't persist them, same reasoning
     * [com.galaxyjoy.cpuinfo.feat.benchresultcard.BenchResultCardRendererTest]'s own sample data
     * already uses for fields the renderer ignores.
     */
    internal fun buildCombinedBenchResultsOrNull(): VMAllBench.Results? {
        val throttle = throttlePrefs.getLastResult() ?: return null
        val storage = storagePrefs.getLastResult() ?: return null
        val ram = ramPrefs.getLastResult() ?: return null
        val gpu = gpuPrefs.getLastResult() ?: return null

        return VMAllBench.Results(
            throttle = ThrottleFingerprint.Result(
                peakFreqMhz = throttle.peakFreqMhz,
                sustainedFreqMhz = throttle.sustainedFreqMhz,
                throttlePercent = throttle.throttlePercent,
                throttled = false,
                startTempC = 0,
                maxTempC = throttle.maxTempC,
                durationMs = 0,
                aborted = false,
                abortReason = null,
                opsPerSecond = throttle.opsPerSecond,
            ),
            storage = StorageBenchmark.Result(
                seqWriteMbPerSec = storage.seqWriteMbPerSec,
                seqReadMbPerSec = storage.seqReadMbPerSec,
                randomWriteOpsPerSec = storage.randomWriteOpsPerSec,
                randomReadOpsPerSec = storage.randomReadOpsPerSec,
                hashMbPerSec = storage.hashMbPerSec,
            ),
            ram = RamBenchmark.Result(writeMbPerSec = ram.writeMbPerSec, readMbPerSec = ram.readMbPerSec),
            gpu = GpuBenchmark.Result(avgFps = gpu.avgFps, frameCount = 0, durationMs = 0),
        )
    }

    private fun writeBitmapEntry(zip: ZipOutputStream, entryName: String, bitmap: Bitmap) {
        zip.putNextEntry(ZipEntry(entryName))
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, zip)
        zip.closeEntry()
    }

    private fun writeTextEntry(zip: ZipOutputStream, entryName: String, text: String) {
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(text.toByteArray())
        zip.closeEntry()
    }
}
