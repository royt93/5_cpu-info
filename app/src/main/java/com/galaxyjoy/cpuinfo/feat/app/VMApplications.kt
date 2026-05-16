package com.galaxyjoy.cpuinfo.feat.app

import android.content.ContentResolver
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.NonNullMutableLiveData
import com.galaxyjoy.cpuinfo.util.Prefs
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for [FrmApplications]
 *
 * @author galaxyjoy
 */
@HiltViewModel
class VMApplications @Inject constructor(
    private val dispatchersProvider: DispatchersProvider,
    private val prefs: Prefs,
    private val packageManager: PackageManager,
) : ViewModel() {

    companion object {
        private const val SORTING_APPS_KEY = "SORTING_APPS_KEY"
    }

    val isLoading = NonNullMutableLiveData(false)
    val applicationList = ListLiveData<ExtendedAppInfo>()

    private var isSortingAsc = prefs.get(SORTING_APPS_KEY, true)
    private var refreshingDisposable: Disposable? = null

    init {
        refreshApplicationsList()
    }

    @Synchronized
    fun refreshApplicationsList() {
        if (refreshingDisposable == null || !isLoading.value) {
            refreshingDisposable = getApplicationsListSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isLoading.value = true }
                .doFinally { isLoading.value = false }
                .subscribe({ appList ->
                    applicationList.replace(appList)
                }, Timber::e)
        }
    }

    /**
     * Get all user applications
     */
    @Suppress("DEPRECATION")
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    internal fun getApplicationsListSingle(): Single<List<ExtendedAppInfo>> {
        return Single.fromCallable {
            val appsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            }
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map {
                    val hasNativeLibs = if (it.nativeLibraryDir != null) {
                        val fileDir = File(it.nativeLibraryDir)
                        val list = fileDir.listFiles()
                        list != null && list.isNotEmpty()
                    } else false
                    ExtendedAppInfo(
                        it.loadLabel(packageManager).toString(), it.packageName,
                        it.sourceDir, it.nativeLibraryDir, hasNativeLibs,
                        getAppIconUri(it.packageName)
                    )
                }

            return@fromCallable if (isSortingAsc) {
                appsList.sortedBy { it.name.uppercase() }
            } else {
                appsList.sortedByDescending { it.name.uppercase() }
            }
        }
    }

    /**
     * Change apps list sorting type from ascending to descending or or vice versa
     */
    fun changeAppsSorting() {
        viewModelScope.launch {
            val sortedAppList = withContext(dispatchersProvider.io) {
                getAppSortedList(!isSortingAsc)
            }
            applicationList.replace(sortedAppList)
        }
    }

    /**
     * Store passed sorting method into [Prefs] and return sorted list (copy from [applicationList])
     *
     * @return sorted list of the apps from [applicationList]
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    internal fun getAppSortedList(sortingAsc: Boolean): List<ExtendedAppInfo> {
        isSortingAsc = sortingAsc
        prefs.insert(SORTING_APPS_KEY, sortingAsc)
        return if (sortingAsc) {
            applicationList.sortedBy { it.name.uppercase() }
        } else {
            applicationList.sortedByDescending { it.name.uppercase() }
        }
    }

    private fun getAppIconUri(packageName: String): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .path(getResourceId(packageName).toString())
            .build()
    }

    @Suppress("DEPRECATION")
    private fun getResourceId(packageName: String): Int {
        val packageInfo: PackageInfo
        try {
            packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return 0
        }
        return packageInfo.applicationInfo?.icon ?: 0
    }

    override fun onCleared() {
        super.onCleared()
        refreshingDisposable?.dispose()
    }
}
