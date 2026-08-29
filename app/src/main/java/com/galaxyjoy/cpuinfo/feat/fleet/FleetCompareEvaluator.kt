package com.galaxyjoy.cpuinfo.feat.fleet

object FleetCompareEvaluator {

    /**
     * Android reports total RAM as ~10-15% below the marketed figure (memory reserved for the
     * kernel, drivers, and hardware components isn't visible to `ActivityManager`) — a genuine
     * 12GB device commonly reports ~10.5GB. 0.75 gives real devices comfortable margin while
     * still catching devices that are actually shipped with much less RAM than claimed.
     */
    private const val RAM_TOLERANCE = 0.75

    /**
     * Formatted storage capacity is always below the marketed figure (filesystem overhead,
     * reserved system partitions) — typically a ~10-12% gap. 0.85 gives real devices margin
     * while still catching a storage chip that's genuinely smaller than claimed.
     */
    private const val STORAGE_TOLERANCE = 0.85

    private const val BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0

    data class Result(
        val matchedEntry: FleetSpecCatalog.FleetSpecEntry?,
        val actualRamGb: Int,
        val actualStorageGb: Int,
        val ramMismatch: Boolean,
        val storageMismatch: Boolean,
    ) {
        val hasMismatch: Boolean get() = ramMismatch || storageMismatch
    }

    fun evaluate(buildModel: String, actualRamBytes: Long, actualStorageBytes: Long): Result {
        val entry = FleetSpecCatalog.findMatch(buildModel)
        val actualRamGb = bytesToGb(actualRamBytes)
        val actualStorageGb = bytesToGb(actualStorageBytes)

        val ramMismatch = entry != null && actualRamGb < entry.minRamGb * RAM_TOLERANCE
        val storageMismatch = entry != null && actualStorageGb < entry.minStorageGb * STORAGE_TOLERANCE

        return Result(
            matchedEntry = entry,
            actualRamGb = actualRamGb,
            actualStorageGb = actualStorageGb,
            ramMismatch = ramMismatch,
            storageMismatch = storageMismatch,
        )
    }

    private fun bytesToGb(bytes: Long): Int = (bytes / BYTES_PER_GB).toInt()
}
