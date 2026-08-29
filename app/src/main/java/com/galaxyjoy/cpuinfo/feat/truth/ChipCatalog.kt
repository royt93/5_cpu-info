package com.galaxyjoy.cpuinfo.feat.truth

/**
 * Display names + vendor-lock heuristics for `cpuinfo_vendor`/`cpuinfo_uarch` enum values
 * (U01 "Device Truth Score"). Enum values copied from `external/cpuinfo/include/cpuinfo.h` —
 * keep in sync if the vendored library is ever updated.
 *
 * Most vendor/uarch combos (standard ARM Cortex cores, Qualcomm Kryo/Krait/Falkor/Saphira) are
 * used across many phone brands and are NOT a reliable signal on their own — flagging them as
 * "mismatch" would false-positive constantly (e.g. Samsung ships Qualcomm Snapdragon in some
 * markets). Only [isBrandLocked] vendors design their own silicon exclusively for their own
 * devices, so a mismatch there is actually meaningful evidence.
 */
object ChipCatalog {

    private val VENDOR_NAMES = mapOf(
        0 to "Unknown",
        1 to "Intel",
        2 to "AMD",
        3 to "ARM (licensed design)",
        4 to "Qualcomm",
        5 to "Apple",
        6 to "Samsung",
        7 to "Nvidia",
        8 to "MIPS",
        9 to "IBM",
        10 to "Ingenic",
        11 to "VIA",
        12 to "Cavium",
        13 to "Broadcom",
        14 to "APM",
        15 to "HiSilicon (Huawei)",
        16 to "Hygon",
        30 to "Texas Instruments",
        31 to "Marvell",
        32 to "RDC",
        33 to "DM&P",
        34 to "Motorola",
    )

    /** Vendors that only ship their silicon in their own devices — a brand mismatch here is real evidence. */
    private val BRAND_LOCKED_VENDORS: Map<Int, Set<String>> = mapOf(
        5 to setOf("apple"),
        6 to setOf("samsung"),
        15 to setOf("huawei", "honor"),
    )

    private val UARCH_NAMES = mapOf(
        0 to "Unknown",
        // ARM Cortex-A/X — licensed by many vendors, not brand-locked.
        0x00300205 to "ARM Cortex-A5",
        0x00300207 to "ARM Cortex-A7",
        0x00300208 to "ARM Cortex-A8",
        0x00300209 to "ARM Cortex-A9",
        0x00300212 to "ARM Cortex-A12",
        0x00300215 to "ARM Cortex-A15",
        0x00300217 to "ARM Cortex-A17",
        0x00300332 to "ARM Cortex-A32",
        0x00300335 to "ARM Cortex-A35",
        0x00300353 to "ARM Cortex-A53",
        0x00300354 to "ARM Cortex-A55 (r0)",
        0x00300355 to "ARM Cortex-A55",
        0x00300357 to "ARM Cortex-A57",
        0x00300365 to "ARM Cortex-A65",
        0x00300372 to "ARM Cortex-A72",
        0x00300373 to "ARM Cortex-A73",
        0x00300375 to "ARM Cortex-A75",
        0x00300376 to "ARM Cortex-A76",
        0x00300377 to "ARM Cortex-A77",
        0x00300378 to "ARM Cortex-A78",
        0x00300400 to "ARM Neoverse N1",
        0x00300401 to "ARM Neoverse E1",
        0x00300402 to "ARM Neoverse V1",
        0x00300403 to "ARM Neoverse N2",
        0x00300501 to "ARM Cortex-X1",
        0x00300502 to "ARM Cortex-X2",
        0x00300551 to "ARM Cortex-A510",
        0x00300571 to "ARM Cortex-A710",
        // Qualcomm — used across many brands, not brand-locked.
        0x00400100 to "Qualcomm Scorpion",
        0x00400101 to "Qualcomm Krait",
        0x00400102 to "Qualcomm Kryo",
        0x00400103 to "Qualcomm Falkor",
        0x00400104 to "Qualcomm Saphira",
        // Nvidia (Tegra — Shield TV / some Chromebooks, not phones).
        0x00500100 to "Nvidia Denver",
        0x00500101 to "Nvidia Denver 2",
        0x00500102 to "Nvidia Carmel",
        // Samsung Exynos big cores — brand-locked (only Samsung ships these).
        0x00600100 to "Samsung Exynos M1",
        0x00600101 to "Samsung Exynos M2",
        0x00600102 to "Samsung Exynos M3",
        0x00600103 to "Samsung Exynos M4",
        0x00600104 to "Samsung Exynos M5",
        // Apple Silicon — not expected on Android at all.
        0x00700105 to "Apple A11 (big)",
        0x00700106 to "Apple A11 (little)",
        0x00700107 to "Apple A12 (big)",
        0x00700108 to "Apple A12 (little)",
        0x00700109 to "Apple A13 (big)",
        0x0070010A to "Apple A13 (little)",
        0x0070010B to "Apple A14/M1 (big)",
        0x0070010C to "Apple A14/M1 (little)",
        // HiSilicon — brand-locked (Huawei/Honor only).
        0x00C00100 to "HiSilicon TaiShan v110",
    )

    fun vendorName(vendorId: Int): String = VENDOR_NAMES[vendorId] ?: "Unknown (id=$vendorId)"

    fun uarchName(uarchId: Int): String = UARCH_NAMES[uarchId] ?: "Unknown (id=0x%08X)".format(uarchId)

    fun isBrandLocked(vendorId: Int): Boolean = BRAND_LOCKED_VENDORS.containsKey(vendorId)

    /**
     * @return true if [declaredManufacturer] (typically [android.os.Build.MANUFACTURER] or
     * [android.os.Build.BRAND], lowercased) is plausible for a brand-locked [vendorId]. Only
     * meaningful when [isBrandLocked] is true — callers should not flag a mismatch otherwise.
     */
    fun isPlausibleBrand(vendorId: Int, declaredManufacturer: String): Boolean {
        val allowed = BRAND_LOCKED_VENDORS[vendorId] ?: return true
        val declared = declaredManufacturer.lowercase()
        return allowed.any { declared.contains(it) }
    }
}
