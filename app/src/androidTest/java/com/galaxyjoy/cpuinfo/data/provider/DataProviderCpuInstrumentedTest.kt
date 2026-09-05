package com.galaxyjoy.cpuinfo.data.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * U34 — real-device tier for [DataProviderCpu.getGovernor], which reads real sysfs
 * (`/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor`) that doesn't exist on the JVM unit
 * test runner — same "only pure logic gets a JVM test, raw sysfs I/O gets a real-device one"
 * split already established for [getCurrentFreq]/[getMinMaxFreq] (untested on JVM, per
 * `DataProviderCpuTest`).
 */
@RunWith(AndroidJUnit4::class)
class DataProviderCpuInstrumentedTest {

    private val dataProviderCpu = DataProviderCpu()

    @Test
    fun getGovernor_core0_neverCrashesAndIsNullOrNonBlank() {
        // Not asserting non-null: confirmed on a real device (TECNO KJ7) that some OEM kernels
        // lock `scaling_governor` down to `system:system` mode 0660 (`ls -l` shows
        // `-rw-rw---- system system`), unlike the world-readable `scaling_cur_freq`/
        // `cpuinfo_min_freq`/`cpuinfo_max_freq` this class already relies on — a regular app
        // process gets `Permission denied` there. getGovernor() must degrade to null in that
        // case, never crash and never return a blank-but-non-null placeholder.
        val governor = dataProviderCpu.getGovernor(0)

        assertTrue(governor == null || governor.isNotBlank())
    }

    @Test
    fun getGovernor_impossibleCoreIndex_returnsNullInsteadOfCrashing() {
        assertTrue(dataProviderCpu.getGovernor(9999) == null)
    }
}
