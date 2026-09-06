package com.galaxyjoy.cpuinfo.data.provider

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import androidx.annotation.VisibleForTesting
import com.galaxyjoy.cpuinfo.domain.model.StorageVolume
import com.galaxyjoy.cpuinfo.domain.model.StorageVolumeInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import javax.inject.Inject

class DataProviderStorage @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val storageManager: StorageManager,
) {

    @Suppress("DEPRECATION")
    fun getInternalVolume(): StorageVolume {
        val path = Environment.getDataDirectory()
        return StorageVolume(path.totalSpace, path.totalSpace - path.usableSpace, fsTypeForPath(path.path))
    }

    @Suppress("DEPRECATION")
    fun getExternalVolume(): StorageVolume? {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) return null
        val path = Environment.getExternalStorageDirectory()
        return StorageVolume(path.totalSpace, path.totalSpace - path.usableSpace, fsTypeForPath(path.path))
    }

    /**
     * And there the magic starts :) TBH I'm not so sure that this is the only good solution but
     * from my testing it is the working one for most of the phones.
     */
    @SuppressLint("UsableSpace")
    fun findSdCardVolume(): StorageVolume? {
        val mountPoint = getExternalSdMounts().firstOrNull()?.substringBefore(":") ?: return null
        val file = File(mountPoint)
        if (!file.exists() || file.totalSpace <= 0) return null
        return StorageVolume(file.totalSpace, file.totalSpace - file.usableSpace, fsTypeForPath(mountPoint))
    }

    /** Paths already surfaced by [getInternalVolume]/[getExternalVolume]/[findSdCardVolume] — fed
     * into [getExtraVolumes] so it only reports genuinely additional volumes. Normalized via
     * [canonicalPath] since `StorageManager.getStorageVolumes()` (a different subsystem than
     * `Environment.get*Directory()`) isn't guaranteed to return byte-identical strings for the
     * same real volume (e.g. `/storage/emulated/0` vs a `/storage/self/primary` alias). */
    @Suppress("DEPRECATION")
    fun getCoveredPaths(): Set<String> = setOfNotNull(
        canonicalPath(Environment.getDataDirectory().path),
        canonicalPath(Environment.getExternalStorageDirectory().path),
        getExternalSdMounts().firstOrNull()?.substringBefore(":")?.let(::canonicalPath),
    )

    /** E10 — every volume [StorageManager.getStorageVolumes] reports that isn't already covered
     * by [getInternalVolume]/[getExternalVolume]/[findSdCardVolume] (matched by directory path),
     * e.g. a 2nd SD card or a USB OTG drive. Uses the real API instead of another `/proc/mounts`
     * guess — [StorageVolume.getDirectory] is nullable (not fully mounted / no accessible path),
     * volumes without one are skipped rather than shown with fake 0-byte totals. */
    fun getExtraVolumes(alreadyCoveredPaths: Set<String>): List<StorageVolumeInfo> = try {
        storageManager.storageVolumes.mapNotNull { volume ->
            val directory = volume.directory ?: return@mapNotNull null
            if (canonicalPath(directory.path) in alreadyCoveredPaths) return@mapNotNull null
            if (!directory.exists() || directory.totalSpace <= 0) return@mapNotNull null
            StorageVolumeInfo(
                label = volume.getDescription(appContext),
                isRemovable = volume.isRemovable,
                totalBytes = directory.totalSpace,
                usedBytes = directory.totalSpace - directory.usableSpace,
                fsType = fsTypeForPath(directory.path),
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun canonicalPath(path: String): String = try {
        File(path).canonicalPath
    } catch (_: Exception) {
        path
    }

    private fun fsTypeForPath(path: String): String? = fsTypeForPath(path, readMountEntries())

    private fun readMountEntries(): List<MountEntry> = try {
        File("/proc/mounts").bufferedReader().useLines { lines ->
            lines.mapNotNull(::parseMountLine).toList()
        }
    } catch (_: Exception) {
        emptyList()
    }

    @Suppress("DEPRECATION")
    private fun getExternalSdMounts(): List<String> {
        val sdDirList = mutableListOf<String>()
        try {
            DataInputStream(FileInputStream("/proc/mounts")).use { dis ->
                val br = BufferedReader(InputStreamReader(dis))
                val externalDir = Environment.getExternalStorageDirectory().path
                while (true) {
                    val strLine = br.readLine() ?: break
                    val mountPoint = candidateMountPoint(strLine, externalDir, sdDirList) ?: continue
                    val path = File(mountPoint)
                    if ((path.exists() || path.isDirectory || path.canWrite())
                        && path.exists()
                        && !path.path.contains("/system")
                    ) {
                        sdDirList.add(mountPoint)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.i(e)
        }

        return sdDirList
    }

    companion object {
        /**
         * Applies the format/dedup filters to a single `/proc/mounts` line and returns the
         * candidate mount point, or null if the line should be skipped. Deliberately excludes
         * filesystem existence checks (File.exists()/canWrite()) so this stays pure and testable
         * — those still run in [getExternalSdMounts] afterwards.
         *
         * Malformed/short lines used to throw ArrayIndexOutOfBounds / StringIndexOutOfBounds here,
         * which the caller's try/catch silently swallowed — failing SD detection with no log (B09).
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
        internal fun candidateMountPoint(
            line: String,
            externalDir: String,
            alreadyFound: List<String>,
        ): String? {
            if (line.contains("asec") || line.contains("legacy") || line.contains("Android/obb")) {
                return null
            }
            if (!(line.startsWith("/dev/block/vold/")
                        || line.startsWith("/dev/block/sd")
                        || line.startsWith("/dev/fuse")
                        || line.startsWith("/mnt/media_rw"))
            ) {
                return null
            }
            val lineElements = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (lineElements.size < 2) return null
            val mountPoint = lineElements[1]
            val lastSlash = mountPoint.lastIndexOf("/")
            if (lastSlash < 0) return null
            if (alreadyFound.contains(mountPoint)) return null
            if (mountPoint == externalDir || mountPoint == "/storage/emulated") return null
            if (alreadyFound.any { it.endsWith(mountPoint.substring(lastSlash)) }) return null
            return mountPoint
        }

        /**
         * Parses one `/proc/mounts` line (`device mountPoint fsType options dump pass`) into a
         * [MountEntry], or null for a malformed/short line. Pure so the fs-type-matching logic
         * ([fsTypeForPath]) is unit-testable without a real `/proc/mounts` file.
         */
        /**
         * Longest-prefix match of [path] against [mounts] — a real mount point is sometimes a
         * parent directory of the path callers ask about (e.g. `/storage/emulated` vs
         * `/storage/emulated/0`), so an exact-match-only lookup would miss it.
         *
         * `trimEnd('/')` on the mount point before appending the separator matters specifically
         * for root (`mountPoint == "/"`): naively appending `"/"` would build the prefix `"//"`,
         * which never matches any real absolute path, silently breaking root as a catch-all
         * fallback. Pure (mount list passed in) so this is unit-testable without a real device.
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
        internal fun fsTypeForPath(path: String, mounts: List<MountEntry>): String? = mounts
            .filter { path == it.mountPoint || path.startsWith("${it.mountPoint.trimEnd('/')}/") }
            .maxByOrNull { it.mountPoint.length }
            ?.fsType

        @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
        internal fun parseMountLine(line: String): MountEntry? {
            val fields = line.split(" ")
            if (fields.size < 3) return null
            return MountEntry(mountPoint = fields[1], fsType = fields[2])
        }
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
internal data class MountEntry(val mountPoint: String, val fsType: String)
