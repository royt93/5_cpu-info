package com.galaxyjoy.cpuinfo.feat.benchhistory

import android.content.Context
import android.content.Intent
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * U25 — power-user data dump of the benchmark history already collected by U18 (`*ResultPrefs`)
 * but never exposed outside the trend chart. 4 independent CSV blocks (one per benchmark type,
 * each with its own header/columns) rather than 1 sparse union-column table — the 4 result shapes
 * don't share fields, and a real CSV each is more directly spreadsheet-pasteable than a single
 * table full of empty cells. `Locale.US` for every number, same reasoning as
 * [com.galaxyjoy.cpuinfo.util.SystemInfoExporter.formatTwoDecimals]: a locale that uses a comma as
 * the decimal separator would corrupt a comma-delimited CSV row, not just look wrong.
 */
class BenchHistoryExporter(
    private val throttlePrefs: ThrottleResultPrefs,
    private val storagePrefs: StorageBenchResultPrefs,
    private val ramPrefs: RamBenchResultPrefs,
    private val gpuPrefs: GpuBenchResultPrefs,
) {

    fun exportHistory(context: Context, scope: CoroutineScope) {
        scope.launch {
            val csv = withContext(Dispatchers.IO) { buildCsv() }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_bench_history_share_subject))
                putExtra(Intent.EXTRA_TEXT, csv)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.export_bench_history_share_chooser_title)),
            )
        }
    }

    internal fun buildCsv(): String = buildString {
        appendLine("=== CPU Throttle ===")
        appendLine("timestamp,peak_freq_mhz,sustained_freq_mhz,throttle_percent,max_temp_c,ops_per_second")
        throttlePrefs.getHistory().forEach {
            appendLine("${timestamp(it.timestampMs)},${it.peakFreqMhz},${it.sustainedFreqMhz},${it.throttlePercent},${it.maxTempC},${it.opsPerSecond}")
        }
        appendLine()

        appendLine("=== Storage ===")
        appendLine("timestamp,seq_write_mb_s,seq_read_mb_s,random_write_ops_s,random_read_ops_s,hash_mb_s")
        storagePrefs.getHistory().forEach {
            appendLine(
                "${timestamp(it.timestampMs)},${decimal(it.seqWriteMbPerSec)},${decimal(it.seqReadMbPerSec)}," +
                    "${decimal(it.randomWriteOpsPerSec)},${decimal(it.randomReadOpsPerSec)},${decimal(it.hashMbPerSec)}",
            )
        }
        appendLine()

        appendLine("=== RAM ===")
        appendLine("timestamp,write_mb_s,read_mb_s")
        ramPrefs.getHistory().forEach {
            appendLine("${timestamp(it.timestampMs)},${decimal(it.writeMbPerSec)},${decimal(it.readMbPerSec)}")
        }
        appendLine()

        appendLine("=== GPU ===")
        appendLine("timestamp,avg_fps")
        gpuPrefs.getHistory().forEach {
            appendLine("${timestamp(it.timestampMs)},${decimal(it.avgFps)}")
        }
    }

    internal fun timestamp(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(epochMs)

    internal fun decimal(value: Double): String = "%.1f".format(Locale.US, value)
}
