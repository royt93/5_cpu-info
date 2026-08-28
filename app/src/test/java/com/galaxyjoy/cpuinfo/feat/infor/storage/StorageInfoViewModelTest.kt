package com.galaxyjoy.cpuinfo.feat.infor.storage

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StorageInfoViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Regression for B08: the mount broadcast can fire more than once for the same card;
    // upsertSdCard() must replace the existing row instead of duplicating it.
    @Test
    fun `upsertSdCard adds a new row when no SD card is tracked yet`() {
        val list = ListLiveData<StorageItem>()
        val sdCard = StorageItem("SD", R.drawable.ic_sdcard, 1000L, 500L)

        StorageInfoViewModel.upsertSdCard(list, sdCard)

        assertEquals(1, list.size)
        assertEquals(sdCard, list[0])
    }

    @Test
    fun `upsertSdCard replaces the existing row instead of duplicating on repeat mount events`() {
        val list = ListLiveData<StorageItem>()
        val firstReading = StorageItem("SD", R.drawable.ic_sdcard, 1000L, 500L)
        val secondReading = StorageItem("SD", R.drawable.ic_sdcard, 1000L, 600L) // more used now

        StorageInfoViewModel.upsertSdCard(list, firstReading)
        StorageInfoViewModel.upsertSdCard(list, secondReading)

        assertEquals(1, list.size) // not 2 — this is the actual regression
        assertEquals(secondReading, list[0])
    }

    @Test
    fun `upsertSdCard does not touch unrelated rows`() {
        val list = ListLiveData<StorageItem>()
        val internal = StorageItem("Internal", R.drawable.ic_root, 5000L, 1000L)
        list.add(internal)

        StorageInfoViewModel.upsertSdCard(list, StorageItem("SD", R.drawable.ic_sdcard, 1000L, 500L))

        assertEquals(2, list.size)
        assertEquals(internal, list[0])
    }

    // Regression for B09: malformed/short lines from /proc/mounts used to throw
    // ArrayIndexOutOfBounds / StringIndexOutOfBounds, silently swallowed by the caller's
    // try/catch and failing SD detection with no log. candidateMountPoint() must skip them.

    @Test
    fun `candidateMountPoint accepts a well-formed vold mount line`() {
        val line = "/dev/block/vold/179:65 /storage/1234-5678 vfat rw 0 0"
        assertEquals(
            "/storage/1234-5678",
            StorageInfoViewModel.candidateMountPoint(line, externalDir = "/storage/emulated/0", alreadyFound = emptyList()),
        )
    }

    @Test
    fun `candidateMountPoint returns null for a line with too few columns`() {
        val line = "/dev/block/vold/179:65"
        assertNull(
            StorageInfoViewModel.candidateMountPoint(line, externalDir = "/storage/emulated/0", alreadyFound = emptyList()),
        )
    }

    @Test
    fun `candidateMountPoint returns null when mount point has no slash`() {
        val line = "/dev/block/vold/179:65 noSlashHere vfat rw 0 0"
        assertNull(
            StorageInfoViewModel.candidateMountPoint(line, externalDir = "/storage/emulated/0", alreadyFound = emptyList()),
        )
    }

    @Test
    fun `candidateMountPoint filters out asec, legacy and obb lines`() {
        val externalDir = "/storage/emulated/0"
        assertNull(StorageInfoViewModel.candidateMountPoint("/dev/block/vold/1 /mnt/asec/foo vfat rw 0 0", externalDir, emptyList()))
        assertNull(StorageInfoViewModel.candidateMountPoint("/dev/block/vold/1 /mnt/legacy/foo vfat rw 0 0", externalDir, emptyList()))
        assertNull(StorageInfoViewModel.candidateMountPoint("/dev/block/vold/1 /storage/Android/obb vfat rw 0 0", externalDir, emptyList()))
    }

    @Test
    fun `candidateMountPoint filters out lines not matching known SD prefixes`() {
        val line = "tmpfs /storage vfat rw 0 0"
        assertNull(
            StorageInfoViewModel.candidateMountPoint(line, externalDir = "/storage/emulated/0", alreadyFound = emptyList()),
        )
    }

    @Test
    fun `candidateMountPoint filters out the primary external dir and already-found duplicates`() {
        val externalDir = "/storage/emulated/0"
        assertNull(
            StorageInfoViewModel.candidateMountPoint(
                "/dev/block/vold/1 $externalDir vfat rw 0 0", externalDir, emptyList(),
            ),
        )
        assertNull(
            StorageInfoViewModel.candidateMountPoint(
                "/dev/block/vold/1 /storage/1234-5678 vfat rw 0 0", externalDir,
                alreadyFound = listOf("/storage/1234-5678"),
            ),
        )
    }
}
