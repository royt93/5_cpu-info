package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

@Keep
data class StorageVolume(val totalBytes: Long, val usedBytes: Long)

@Keep
data class StorageData(
    val internal: StorageVolume,
    /** Primary shared storage (`Environment.getExternalStorageDirectory()`) — usually present
     * even without a physical SD card, since it's backed by emulated storage on most devices. */
    val external: StorageVolume?,
    /** Secondary removable SD card, detected via `/proc/mounts`. Null when none is inserted. */
    val sdCard: StorageVolume?,
)
