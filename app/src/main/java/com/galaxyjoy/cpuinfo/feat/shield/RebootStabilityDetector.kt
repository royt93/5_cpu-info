package com.galaxyjoy.cpuinfo.feat.shield

/** E21 — pure boot-count comparison, no Android deps so it's plain-JVM testable.
 *
 * Originally computed boot time as `wall-clock minus uptime` and flagged a reboot when that
 * shifted by more than a tolerance — but that value shifts by exactly the size of ANY wall-clock
 * adjustment (NTP sync, manual date/timezone change), which is indistinguishable from a real
 * reboot with that approach and produces false positives, especially on budget devices whose RTC
 * is wrong until the network fixes it. `Settings.Global.BOOT_COUNT` is a monotonic counter the OS
 * itself maintains and increments once per real boot — immune to clock changes entirely. */
object RebootStabilityDetector {

    fun isNewBoot(currentBootCount: Int, lastKnownBootCount: Int): Boolean {
        if (lastKnownBootCount < 0) return false
        return currentBootCount != lastKnownBootCount
    }
}
