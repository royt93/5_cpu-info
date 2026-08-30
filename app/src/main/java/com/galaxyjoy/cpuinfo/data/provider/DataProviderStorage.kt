package com.galaxyjoy.cpuinfo.data.provider

import android.annotation.SuppressLint
import android.os.Environment
import androidx.annotation.VisibleForTesting
import com.galaxyjoy.cpuinfo.domain.model.StorageVolume
import timber.log.Timber
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import javax.inject.Inject

class DataProviderStorage @Inject constructor() {

    @Suppress("DEPRECATION")
    fun getInternalVolume(): StorageVolume {
        val path = Environment.getDataDirectory()
        return StorageVolume(path.totalSpace, path.totalSpace - path.usableSpace)
    }

    @Suppress("DEPRECATION")
    fun getExternalVolume(): StorageVolume? {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) return null
        val path = Environment.getExternalStorageDirectory()
        return StorageVolume(path.totalSpace, path.totalSpace - path.usableSpace)
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
        return StorageVolume(file.totalSpace, file.totalSpace - file.usableSpace)
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
    }
}
