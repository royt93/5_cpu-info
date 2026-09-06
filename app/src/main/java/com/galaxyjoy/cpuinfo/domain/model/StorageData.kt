package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

@Keep
data class StorageVolume(
    val totalBytes: Long,
    val usedBytes: Long,
    /** E10 — real filesystem type (`ext4`/`f2fs`/`vfat`/`exfat`/`fuse`/...) matched from
     * `/proc/mounts` by mount-point prefix. Null when no matching mount entry was found. */
    val fsType: String? = null,
)

@Keep
data class StorageData(
    val internal: StorageVolume,
    /** Primary shared storage (`Environment.getExternalStorageDirectory()`) — usually present
     * even without a physical SD card, since it's backed by emulated storage on most devices. */
    val external: StorageVolume?,
    /** Secondary removable SD card, detected via `/proc/mounts`. Null when none is inserted. */
    val sdCard: StorageVolume?,
    /** E10 — any additional volume `StorageManager.getStorageVolumes()` reports that isn't
     * already covered by [internal]/[external]/[sdCard] above (e.g. a 2nd SD card, USB OTG
     * drive). Empty on the common single-storage device. */
    val extraVolumes: List<StorageVolumeInfo> = emptyList(),
)

@Keep
data class StorageVolumeInfo(
    val label: String,
    val isRemovable: Boolean,
    val totalBytes: Long,
    val usedBytes: Long,
    val fsType: String?,
)
