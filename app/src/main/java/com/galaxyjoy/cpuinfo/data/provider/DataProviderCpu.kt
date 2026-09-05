package com.galaxyjoy.cpuinfo.data.provider

import android.os.Build
import androidx.annotation.VisibleForTesting
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.io.RandomAccessFile
import java.util.regex.Pattern
import javax.inject.Inject

class DataProviderCpu @Inject constructor() {

    fun getAbi(): String {
        return if (Build.VERSION.SDK_INT >= 21) {
            Build.SUPPORTED_ABIS[0]
        } else {
            @Suppress("DEPRECATION")
            Build.CPU_ABI
        }
    }

    /**
     * `Runtime.availableProcessors()` only counts cores currently online. On modern
     * big.LITTLE/DynamIQ chips the big cores are often power-collapsed (offline) when idle, so
     * that API alone would undercount and flicker with load. `/sys/.../cpu/possible` lists every
     * core the kernel knows about regardless of online state, so prefer it when readable.
     */
    fun getNumberOfCores(): Int {
        return getNumberOfCoresFromPossibleList() ?: if (Build.VERSION.SDK_INT >= 17) {
            Runtime.getRuntime().availableProcessors()
        } else {
            getNumCoresLegacy()
        }
    }

    /**
     * Parses e.g. "0-7" or "0-3,4-7" from `/sys/devices/system/cpu/possible` into a core count.
     */
    private fun getNumberOfCoresFromPossibleList(): Int? {
        return try {
            val text = RandomAccessFile("${CPU_INFO_DIR}possible", "r").use { it.readLine() }
            parsePossibleCoreCount(text)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checking frequencies directories and return current value if exists (otherwise we can
     * assume that core is stopped - value -1)
     */
    fun getCurrentFreq(coreNumber: Int): Long {
        val currentFreqPath = "${CPU_INFO_DIR}cpu$coreNumber/cpufreq/scaling_cur_freq"
        return try {
            RandomAccessFile(currentFreqPath, "r").use { it.readLine().toLong() / 1000 }
        } catch (e: Exception) {
            Timber.e("getCurrentFreq() - cannot read file")
            -1
        }
    }

    /**
     * Read max/min frequencies for specific [coreNumber]. Return [Pair] with min and max frequency
     * or [Pair] with -1.
     */
    fun getMinMaxFreq(coreNumber: Int): Pair<Long, Long> {
        val minPath = "${CPU_INFO_DIR}cpu$coreNumber/cpufreq/cpuinfo_min_freq"
        val maxPath = "${CPU_INFO_DIR}cpu$coreNumber/cpufreq/cpuinfo_max_freq"
        return try {
            val minMhz = RandomAccessFile(minPath, "r").use { it.readLine().toLong() / 1000 }
            val maxMhz = RandomAccessFile(maxPath, "r").use { it.readLine().toLong() / 1000 }
            Pair(minMhz, maxMhz)
        } catch (_: Exception) {
            Timber.e("getMinMaxFreq() - cannot read file")
            Pair(-1, -1)
        }
    }

    /**
     * U34 — cpufreq governor (e.g. "schedutil"/"performance"/"ondemand") for [coreNumber]'s
     * scaling policy, or `null` if the core is offline/unreadable — same try/catch-around-
     * `RandomAccessFile` shape as [getCurrentFreq]/[getMinMaxFreq], but `null` instead of a `-1`
     * sentinel since there's no sensible numeric placeholder for a governor name.
     *
     * Confirmed on a real device (TECNO KJ7) that `scaling_governor` isn't always readable: some
     * OEM kernels lock it to `system:system` mode 0660, unlike the world-readable
     * `scaling_cur_freq`/`cpuinfo_min_freq`/`cpuinfo_max_freq` [getCurrentFreq]/[getMinMaxFreq]
     * already rely on — a regular app gets `Permission denied` there, caught below and degraded
     * to `null` like any other unreadable-file case. The caller hides the row entirely when
     * `null` rather than showing a placeholder.
     */
    fun getGovernor(coreNumber: Int): String? {
        val path = "${CPU_INFO_DIR}cpu$coreNumber/cpufreq/scaling_governor"
        return try {
            RandomAccessFile(path, "r").use { it.readLine() }?.trim()?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.e("getGovernor() - cannot read file")
            null
        }
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     *
     * @return The number of cores, or 1 if check fails
     */
    private fun getNumCoresLegacy(): Int {
        class CpuFilter : FileFilter {
            override fun accept(pathname: File): Boolean {
                // Check if filename is "cpu", followed by a single digit number
                return Pattern.matches("cpu[0-9]+", pathname.name)
            }
        }
        return try {
            File(CPU_INFO_DIR).listFiles(CpuFilter())?.size ?: 1
        } catch (_: Exception) {
            1
        }
    }

    companion object {
        private const val CPU_INFO_DIR = "/sys/devices/system/cpu/"

        /** Parses e.g. "0-7" or "0-3,4-7" into a core count. Null on malformed/blank input. */
        @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
        internal fun parsePossibleCoreCount(text: String?): Int? {
            if (text.isNullOrBlank()) return null
            return try {
                text.split(",").sumOf { range ->
                    val bounds = range.trim().split("-")
                    when (bounds.size) {
                        1 -> 1
                        2 -> bounds[1].toInt() - bounds[0].toInt() + 1
                        else -> 0
                    }
                }.takeIf { it > 0 }
            } catch (_: Exception) {
                null
            }
        }
    }
}
