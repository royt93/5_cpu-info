package com.galaxyjoy.cpuinfo.feat.storagebench

import android.content.Context
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.random.Random

/**
 * Drives the F06 workload on [Dispatchers.IO]: sequential write/read + random write/read against
 * a throwaway file in [Context.getCacheDir], then a short SHA-256 hashing burst. The temp file is
 * always deleted in a `finally` block, whichever phase the run ends on.
 */
class StorageBenchmarkRunner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val temperatureProvider: TemperatureProvider,
) {

    enum class Phase { SEQ_WRITE, SEQ_READ, RANDOM_WRITE, RANDOM_READ, HASH }

    sealed interface State {
        data class Running(val phase: Phase) : State
        data class Finished(val result: StorageBenchmark.Result) : State
        data object Aborted : State
    }

    @Volatile
    private var stopRequested = false

    fun requestStop() {
        stopRequested = true
    }

    suspend fun run(onState: suspend (State) -> Unit): Unit = withContext(Dispatchers.IO) {
        stopRequested = false

        if (StorageBenchmark.shouldAbortForSafety(temperatureProvider.getBatteryTemperature())) {
            onState(State.Aborted)
            return@withContext
        }

        val testFile = File(context.cacheDir, "storage_bench_${System.nanoTime()}.tmp")
        try {
            onState(State.Running(Phase.SEQ_WRITE))
            val seqWriteNanos = measureNanos { sequentialWrite(testFile) }
            if (stopRequested) return@withContext onState(State.Aborted)

            onState(State.Running(Phase.SEQ_READ))
            val seqReadNanos = measureNanos { sequentialRead(testFile) }
            if (stopRequested) return@withContext onState(State.Aborted)

            if (StorageBenchmark.shouldAbortForSafety(temperatureProvider.getBatteryTemperature())) {
                return@withContext onState(State.Aborted)
            }

            onState(State.Running(Phase.RANDOM_WRITE))
            val randomWriteNanos = measureNanos { randomWrite(testFile) }
            if (stopRequested) return@withContext onState(State.Aborted)

            onState(State.Running(Phase.RANDOM_READ))
            val randomReadNanos = measureNanos { randomRead(testFile) }
            if (stopRequested) return@withContext onState(State.Aborted)

            onState(State.Running(Phase.HASH))
            val (hashBytes, hashNanos) = hashBenchmark()

            onState(
                State.Finished(
                    StorageBenchmark.Result(
                        seqWriteMbPerSec = StorageBenchmark.mbPerSec(StorageBenchmark.SEQ_FILE_SIZE_BYTES, seqWriteNanos),
                        seqReadMbPerSec = StorageBenchmark.mbPerSec(StorageBenchmark.SEQ_FILE_SIZE_BYTES, seqReadNanos),
                        randomWriteOpsPerSec = StorageBenchmark.opsPerSec(StorageBenchmark.RANDOM_OPS_COUNT, randomWriteNanos),
                        randomReadOpsPerSec = StorageBenchmark.opsPerSec(StorageBenchmark.RANDOM_OPS_COUNT, randomReadNanos),
                        hashMbPerSec = StorageBenchmark.mbPerSec(hashBytes, hashNanos),
                    ),
                ),
            )
        } finally {
            testFile.delete()
        }
    }

    private inline fun measureNanos(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return System.nanoTime() - start
    }

    private fun sequentialWrite(file: File) {
        val buffer = ByteArray(StorageBenchmark.SEQ_CHUNK_BYTES).also(Random::nextBytes)
        FileOutputStream(file).use { out ->
            var written = 0L
            while (written < StorageBenchmark.SEQ_FILE_SIZE_BYTES) {
                out.write(buffer)
                written += buffer.size
            }
            out.fd.sync()
        }
    }

    private fun sequentialRead(file: File) {
        val buffer = ByteArray(StorageBenchmark.SEQ_CHUNK_BYTES)
        FileInputStream(file).use { input ->
            while (input.read(buffer) != -1) {
                // drain — throughput is what's measured, not the content
            }
        }
    }

    private fun randomWrite(file: File) {
        val buffer = ByteArray(StorageBenchmark.RANDOM_BLOCK_BYTES).also(Random::nextBytes)
        val maxOffset = StorageBenchmark.RANDOM_FILE_SIZE_BYTES - StorageBenchmark.RANDOM_BLOCK_BYTES
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(StorageBenchmark.RANDOM_FILE_SIZE_BYTES)
            repeat(StorageBenchmark.RANDOM_OPS_COUNT) {
                raf.seek(randomOffset(maxOffset))
                raf.write(buffer)
            }
            raf.fd.sync()
        }
    }

    private fun randomRead(file: File) {
        val buffer = ByteArray(StorageBenchmark.RANDOM_BLOCK_BYTES)
        val maxOffset = StorageBenchmark.RANDOM_FILE_SIZE_BYTES - StorageBenchmark.RANDOM_BLOCK_BYTES
        RandomAccessFile(file, "r").use { raf ->
            repeat(StorageBenchmark.RANDOM_OPS_COUNT) {
                raf.seek(randomOffset(maxOffset))
                raf.read(buffer)
            }
        }
    }

    private fun randomOffset(maxOffset: Long): Long = (Random.nextDouble() * maxOffset).toLong()

    /** @return total bytes hashed to `hashDurationNanos`. */
    private fun hashBenchmark(): Pair<Long, Long> {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(StorageBenchmark.HASH_BUFFER_BYTES).also(Random::nextBytes)
        var totalBytes = 0L
        val start = System.nanoTime()
        val durationNanos = StorageBenchmark.HASH_DURATION_MS * 1_000_000L
        while (System.nanoTime() - start < durationNanos) {
            digest.reset()
            digest.digest(buffer)
            totalBytes += buffer.size
        }
        return totalBytes to (System.nanoTime() - start)
    }
}
