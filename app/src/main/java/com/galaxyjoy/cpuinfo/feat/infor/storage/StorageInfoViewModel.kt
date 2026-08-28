package com.galaxyjoy.cpuinfo.feat.infor.storage

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Environment
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.*
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for [FrmStorageInfo]
 *
 */
@HiltViewModel
class StorageInfoViewModel @Inject constructor(
    private val dispatchersProvider: DispatchersProvider,
    private val resources: Resources
) : ViewModel() {

    enum class MemoryType { INTERNAL, EXTERNAL }

    val listLiveData = ListLiveData<StorageItem>()

    private var sdCardFinderDisposable: Disposable? = null

    init {
        getStorageInfo()
    }

    /**
     * Get all available details about internal, external and secondary (SD card) storage
     */
    private fun getStorageInfo() {
        viewModelScope.launch {
            val memoryPair = withContext(dispatchersProvider.io) {
                getExternalAndInternalMemoryPair()
            }
            listLiveData.add(memoryPair.first)
            if (memoryPair.second != null) {
                listLiveData.add(memoryPair.second as StorageItem)
            }
            refreshSdCard()
        }
    }

    /**
     * @return pair of [StorageItem] where on the first place is internal storage and on the second
     * external
     */
    private fun getExternalAndInternalMemoryPair(): Pair<StorageItem, StorageItem?> {
        val internalTotal = getTotalMemorySize(MemoryType.INTERNAL)
        val internalUsed = internalTotal - getAvailableMemorySize(MemoryType.INTERNAL)
        val internalMemory = StorageItem(
            resources.getString(R.string.internal), R.drawable.ic_root,
            internalTotal, internalUsed
        )

        var externalMemory: StorageItem? = null
        if (isExternalMemoryAvailable()) {
            val externalTotal = getTotalMemorySize(MemoryType.EXTERNAL)
            val externalUsed = externalTotal - getAvailableMemorySize(MemoryType.EXTERNAL)
            externalMemory = StorageItem(
                resources.getString(R.string.external),
                R.drawable.ic_folder, externalTotal, externalUsed
            )
        }
        return Pair(internalMemory, externalMemory)
    }

    /**
     * Try to get new data about SD card or remove it from the list
     */
    @SuppressLint("UsableSpace")
    @Synchronized
    fun refreshSdCard() {
        if (sdCardFinderDisposable == null || sdCardFinderDisposable!!.isDisposed) {
            sdCardFinderDisposable = getSDCardFinder()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ sdCardFile ->
                    if (sdCardFile.totalSpace > 0) {
                        val sdTotal = sdCardFile.totalSpace
                        val sdUsed = sdTotal - sdCardFile.usableSpace
                        val sdMemory = StorageItem(
                            resources.getString(R.string.external),
                            R.drawable.ic_sdcard, sdTotal, sdUsed
                        )
                        upsertSdCard(listLiveData, sdMemory)
                    }
                }, {
                    Timber.i("Cannot find SD card file")
                    val storageItem = listLiveData.find { storageItem ->
                        storageItem.iconRes == R.drawable.ic_sdcard
                    }
                    if (storageItem != null) {
                        listLiveData.remove(storageItem)
                    }
                })
        }
    }

    /**
     * @return true if device support external storage, otherwise false
     */
    private fun isExternalMemoryAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * Get total bytes from internal or external storage.
     *
     * @param memoryType type of memory
     * @return full size of the storage
     */
    @Suppress("DEPRECATION")
    private fun getTotalMemorySize(memoryType: MemoryType): Long {
        val path: File = when (memoryType) {
            MemoryType.INTERNAL -> Environment.getDataDirectory()
            MemoryType.EXTERNAL -> Environment.getExternalStorageDirectory()
        }

        return path.totalSpace
    }

    /**
     * Get available bytes from internal or external storage.
     *
     * @param memoryType type of memory
     * @return available size of the storage
     */
    @Suppress("DEPRECATION")
    private fun getAvailableMemorySize(memoryType: MemoryType): Long {
        val path: File = when (memoryType) {
            MemoryType.INTERNAL -> Environment.getDataDirectory()
            MemoryType.EXTERNAL -> Environment.getExternalStorageDirectory()
        }

        return path.usableSpace
    }

    /**
     * @return [Single] with the SD card file or onError in case of missing that one.
     */
    private fun getSDCardFinder(): Single<File> {
        return Single.create { emitter: SingleEmitter<File> ->
            val mountedList = getExternalSDMounts()

            var strSDCardPath: String? = null
            if (mountedList.isNotEmpty()) {
                strSDCardPath = mountedList[0]
            }

            if (!strSDCardPath.isNullOrEmpty()) {
                if (strSDCardPath.contains(":")) {
                    strSDCardPath = strSDCardPath.substring(0, strSDCardPath.indexOf(":"))
                }

                val externalFilePath = File(strSDCardPath)

                if (externalFilePath.exists() && !emitter.isDisposed) {
                    emitter.onSuccess(externalFilePath)
                } else if (!emitter.isDisposed) {
                    emitter.onError(FileNotFoundException("Cannot find SD card file"))
                }
            } else if (!emitter.isDisposed) {
                emitter.onError(FileNotFoundException("Cannot find SD card file"))
            }
        }
    }

    /**
     * And there the magic starts :) TBH I'm not so sure that this is the only good solution but
     * from my testing it is the working one for most of the phones.
     */
    @Suppress("DEPRECATION")
    private fun getExternalSDMounts(): ArrayList<String> {
        val sdDirList = ArrayList<String>()
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
                        //&& path.canRead()
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

    override fun onCleared() {
        super.onCleared()
        sdCardFinderDisposable?.dispose()
    }

    companion object {
        /**
         * Upsert instead of blind add(): the mount broadcast can fire more than once for the
         * same card, add() alone would duplicate the row on every re-fire (B08).
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
        internal fun upsertSdCard(list: ListLiveData<StorageItem>, sdMemory: StorageItem) {
            val existingIndex = list.indexOfFirst { it.iconRes == R.drawable.ic_sdcard }
            if (existingIndex >= 0) {
                list[existingIndex] = sdMemory
            } else {
                list.add(sdMemory)
            }
        }

        /**
         * Applies the format/dedup filters to a single `/proc/mounts` line and returns the
         * candidate mount point, or null if the line should be skipped. Deliberately excludes
         * filesystem existence checks (File.exists()/canWrite()) so this stays pure and testable
         * — those still run in [getExternalSDMounts] afterwards.
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
