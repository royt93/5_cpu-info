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
    fun `fsTypeForPath matches an exact mount point`() {
        val mounts = listOf(MountEntry("/data", "ext4"))
        assertEquals("ext4", DataProviderStorage.fsTypeForPath("/data", mounts))
    }

    @Test
    fun `fsTypeForPath picks the longest matching prefix`() {
        val mounts = listOf(MountEntry("/storage/emulated", "sdcardfs"), MountEntry("/storage/emulated/0", "fuse"))
        assertEquals("fuse", DataProviderStorage.fsTypeForPath("/storage/emulated/0/DCIM", mounts))
    }

    @Test
    fun `fsTypeForPath falls back to root as a catch-all`() {
        // Regression: naively building the prefix as "\${mountPoint}/" for root ("/") produces
        // "//", which never matches any real absolute path — root must still work as a fallback.
        val mounts = listOf(MountEntry("/", "ext4"))
        assertEquals("ext4", DataProviderStorage.fsTypeForPath("/data/anything", mounts))
    }

    @Test
    fun `fsTypeForPath returns null when nothing matches`() {
        val mounts = listOf(MountEntry("/data", "ext4"))
        assertNull(DataProviderStorage.fsTypeForPath("/storage/emulated/0", mounts))
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
        // getExtraVolumes canonicalizes its own side of the comparison (symlink/alias-proof), so
        // the covered-paths set passed in must already be canonical too — exactly what the real
        // caller (getCoveredPaths()) does. A raw, non-canonical tmpdir path (e.g. "/tmp" on a host
        // where it's a symlink to "/private/tmp") would NOT match here, by design.
        val tmpDir = java.io.File(System.getProperty("java.io.tmpdir")!!)
        val canonicalCoveredPath = tmpDir.canonicalPath
        val volume: StorageVolume = mockk()
        every { volume.directory } returns tmpDir
        every { storageManager.storageVolumes } returns listOf(volume)

        assertTrue(provider.getExtraVolumes(setOf(canonicalCoveredPath)).isEmpty())
    }

    @Test
    fun `getExtraVolumes is not fooled by a non-canonical but equivalent covered path`() {
        // Regression: StorageManager's StorageVolume.directory and Environment.get*Directory()
        // are different subsystems and aren't guaranteed to return byte-identical path strings
        // for the same real volume — canonicalization must bridge that, not exact string equality.
        val tmpDir = java.io.File(System.getProperty("java.io.tmpdir")!!)
        val nonCanonicalCoveredPath = tmpDir.path + "/."
        val volume: StorageVolume = mockk()
        every { volume.directory } returns tmpDir
        every { storageManager.storageVolumes } returns listOf(volume)

        assertTrue(provider.getExtraVolumes(setOf(java.io.File(nonCanonicalCoveredPath).canonicalPath)).isEmpty())
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
