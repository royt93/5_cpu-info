package com.galaxyjoy.cpuinfo.data.provider

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataProviderStorageTest {

    // Regression for B09: malformed/short lines from /proc/mounts used to throw
    // ArrayIndexOutOfBounds / StringIndexOutOfBounds, silently swallowed by the caller's
    // try/catch and failing SD detection with no log. candidateMountPoint() must skip them.

    @Test
    fun `candidateMountPoint accepts a well-formed vold mount line`() {
        val line = "/dev/block/vold/179:65 /storage/1234-5678 vfat rw 0 0"
        assertEquals(
            "/storage/1234-5678",
            DataProviderStorage.candidateMountPoint(line, externalDir = "/storage/emulated/0", alreadyFound = emptyList()),
        )
    }

    @Test
    fun `candidateMountPoint returns null for a line with too few columns`() {
        val line = "/dev/block/vold/179:65"
        assertNull(
            DataProviderStorage.candidateMountPoint(line, externalDir = "/storage/emulated/0", alreadyFound = emptyList()),
        )
    }

    @Test
    fun `candidateMountPoint returns null when mount point has no slash`() {
        val line = "/dev/block/vold/179:65 noSlashHere vfat rw 0 0"
        assertNull(
            DataProviderStorage.candidateMountPoint(line, externalDir = "/storage/emulated/0", alreadyFound = emptyList()),
        )
    }

    @Test
    fun `candidateMountPoint filters out asec, legacy and obb lines`() {
        val externalDir = "/storage/emulated/0"
        assertNull(DataProviderStorage.candidateMountPoint("/dev/block/vold/1 /mnt/asec/foo vfat rw 0 0", externalDir, emptyList()))
        assertNull(DataProviderStorage.candidateMountPoint("/dev/block/vold/1 /mnt/legacy/foo vfat rw 0 0", externalDir, emptyList()))
        assertNull(DataProviderStorage.candidateMountPoint("/dev/block/vold/1 /storage/Android/obb vfat rw 0 0", externalDir, emptyList()))
    }

    @Test
    fun `candidateMountPoint filters out lines not matching known SD prefixes`() {
        val line = "tmpfs /storage vfat rw 0 0"
        assertNull(
            DataProviderStorage.candidateMountPoint(line, externalDir = "/storage/emulated/0", alreadyFound = emptyList()),
        )
    }

    @Test
    fun `candidateMountPoint filters out the primary external dir and already-found duplicates`() {
        val externalDir = "/storage/emulated/0"
        assertNull(
            DataProviderStorage.candidateMountPoint(
                "/dev/block/vold/1 $externalDir vfat rw 0 0", externalDir, emptyList(),
            ),
        )
        assertNull(
            DataProviderStorage.candidateMountPoint(
                "/dev/block/vold/1 /storage/1234-5678 vfat rw 0 0", externalDir,
                alreadyFound = listOf("/storage/1234-5678"),
            ),
        )
    }

    @Test
    fun `findSdCardVolume returns null when proc mounts has no matching SD card`() {
        // On the JVM test host /proc/mounts either doesn't exist or has no vold/media_rw lines —
        // this is also the real behavior on a device with no SD card inserted, which must fail
        // silently rather than crash.
        assertNull(DataProviderStorage().findSdCardVolume())
    }
}
