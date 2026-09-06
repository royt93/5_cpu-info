package com.galaxyjoy.cpuinfo.data.provider

import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataProviderStorageTest {

    private val appContext: Context = mockk(relaxed = true)
    private val storageManager: StorageManager = mockk(relaxed = true)
    private val provider = DataProviderStorage(appContext, storageManager)

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
        assertNull(provider.findSdCardVolume())
    }

    @Test
    fun `parseMountLine extracts mount point and fs type from a well-formed line`() {
        val entry = DataProviderStorage.parseMountLine("/dev/block/dm-1 /data ext4 rw,seclabel 0 0")
        assertEquals("/data", entry?.mountPoint)
        assertEquals("ext4", entry?.fsType)
    }

    @Test
    fun `parseMountLine returns null for a line with too few columns`() {
        assertNull(DataProviderStorage.parseMountLine("/dev/block/dm-1 /data"))
    }

    @Test
    fun `getExtraVolumes skips volumes with no directory`() {
        val volume: StorageVolume = mockk()
        every { volume.directory } returns null
        every { storageManager.storageVolumes } returns listOf(volume)

        assertTrue(provider.getExtraVolumes(emptySet()).isEmpty())
    }

    @Test
    fun `getExtraVolumes skips paths already covered elsewhere`() {
        val coveredPath = System.getProperty("java.io.tmpdir")!!
        val volume: StorageVolume = mockk()
        every { volume.directory } returns java.io.File(coveredPath)
        every { storageManager.storageVolumes } returns listOf(volume)

        assertTrue(provider.getExtraVolumes(setOf(coveredPath)).isEmpty())
    }

    @Test
    fun `getExtraVolumes reports an uncovered volume with its description and removable flag`() {
        val volume: StorageVolume = mockk()
        val directory = java.io.File(System.getProperty("java.io.tmpdir")!!)
        every { volume.directory } returns directory
        every { volume.getDescription(appContext) } returns "USB Drive"
        every { volume.isRemovable } returns true
        every { storageManager.storageVolumes } returns listOf(volume)

        val result = provider.getExtraVolumes(emptySet())

        assertEquals(1, result.size)
        assertEquals("USB Drive", result[0].label)
        assertTrue(result[0].isRemovable)
    }

    @Test
    fun `getExtraVolumes is empty when StorageManager throws`() {
        every { storageManager.storageVolumes } throws SecurityException("no permission")

        assertEquals(emptyList(), provider.getExtraVolumes(emptySet()))
    }
}
