package com.galaxyjoy.cpuinfo.feat.storagetruth

import android.content.Context
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject

/**
 * Drives the E01 write-then-verify workload on [Dispatchers.IO] against a throwaway file in
 * [Context.getCacheDir], always deleted in a `finally` block. Two full passes, not interleaved
 * per-block: (1) write [StorageTruthBenchmark.generatePattern] for every block using
 * [RandomAccessFile] mode `"rwd"` (every write synced to storage immediately, not just buffered),
 * so the write side genuinely commits to the flash chip, not just the OS write-back cache;
 * (2) close and reopen the file, read every block back and hand it to
 * [StorageTruthBenchmark.findMismatches].
 *
 * Known limitation, documented rather than glossed over: unlike desktop tools (H2testw/F3), there
 * is no public, unprivileged Android API to force-evict a file's pages from the OS *read* page
 * cache (`posix_fadvise(POSIX_FADV_DONTNEED)` exists in the Linux kernel/AOSP but is not exposed
 * by the public `android.system.Os` SDK surface — verified against the actual API 37 `android.jar`,
 * only `posix_fallocate` is present). So on a device with enough free RAM to keep the whole test
 * file resident, the verify-read pass may be served from cache rather than physically re-reading
 * the flash. This mainly narrows *how subtle* a fraud this can prove (a wraparound whose corrupted
 * bytes happen to still be freshly cached elsewhere in the same file could go unnoticed); the
 * severe capacity fraud this quick scan targets (real capacity far below [StorageTruthBenchmark.TEST_SIZE_BYTES])
 * still tends to surface as write-time I/O errors or inconsistent latency on real counterfeit
 * controllers, not just cache-maskable silent corruption. A future VIP "deep scan" spanning
 * multiple gigabytes would naturally outgrow most devices' free RAM and sidestep this entirely.
 */
class StorageTruthRunner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val temperatureProvider: TemperatureProvider,
) {

    enum class Phase { WRITING, VERIFYING }

    sealed interface State {
        data class Running(val phase: Phase, val blockIndex: Int, val blockCount: Int) : State
        data class Finished(val result: StorageTruthBenchmark.Result) : State
        data class Aborted(val reason: StorageTruthBenchmark.AbortReason) : State
    }

    @Volatile
    private var stopRequested = false

    fun requestStop() {
        stopRequested = true
    }

    /** Checked every [SAFETY_CHECK_INTERVAL_BLOCKS] blocks, not every block — `getBatteryTemperature()`
     * is cheap but this test can run hundreds of blocks; no need to poll it that often. */
    private val safetyCheckIntervalBlocks = 32

    suspend fun run(
        testSizeBytes: Long = StorageTruthBenchmark.TEST_SIZE_BYTES,
        blockSizeBytes: Int = StorageTruthBenchmark.BLOCK_SIZE_BYTES,
        onState: suspend (State) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        stopRequested = false
        val blockCount = (testSizeBytes / blockSizeBytes).toInt()

        if (StorageTruthBenchmark.shouldAbortForSafety(temperatureProvider.getBatteryTemperature())) {
            onState(State.Aborted(StorageTruthBenchmark.AbortReason.OVERHEAT))
            return@withContext
        }
        // 2x margin: leaves room for the OS/other apps and avoids ever filling the device to 100%.
        if (context.cacheDir.usableSpace < testSizeBytes * 2) {
            onState(State.Aborted(StorageTruthBenchmark.AbortReason.INSUFFICIENT_SPACE))
            return@withContext
        }

        val file = File(context.cacheDir, "storage_truth_${System.nanoTime()}.bin")
        try {
            RandomAccessFile(file, "rwd").use { raf ->
                for (i in 0 until blockCount) {
                    if (stopRequested) {
                        onState(State.Aborted(StorageTruthBenchmark.AbortReason.INTERRUPTED))
                        return@withContext
                    }
                    if (i % safetyCheckIntervalBlocks == 0 &&
                        StorageTruthBenchmark.shouldAbortForSafety(temperatureProvider.getBatteryTemperature())
                    ) {
                        onState(State.Aborted(StorageTruthBenchmark.AbortReason.OVERHEAT))
                        return@withContext
                    }
                    onState(State.Running(Phase.WRITING, i, blockCount))
                    raf.write(StorageTruthBenchmark.generatePattern(i, blockSizeBytes))
                }
            }

            onState(State.Running(Phase.VERIFYING, 0, blockCount))
            val mismatches = RandomAccessFile(file, "r").use { raf ->
                val buffer = ByteArray(blockSizeBytes)
                // Explicit seek per index (not just relying on sequential position) so this stays
                // correct regardless of the order findMismatches happens to call this lambda in.
                StorageTruthBenchmark.findMismatches(blockCount, blockSizeBytes) { i ->
                    raf.seek(i.toLong() * blockSizeBytes)
                    raf.readFully(buffer)
                    buffer.copyOf()
                }
            }
            onState(State.Finished(StorageTruthBenchmark.Result(blockCount, mismatches)))
        } finally {
            file.delete()
        }
    }
}
