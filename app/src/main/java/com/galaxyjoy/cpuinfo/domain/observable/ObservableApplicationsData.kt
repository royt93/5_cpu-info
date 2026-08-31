package com.galaxyjoy.cpuinfo.domain.observable

import android.content.ContentResolver
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import com.galaxyjoy.cpuinfo.data.provider.DataProviderApplications
import com.galaxyjoy.cpuinfo.domain.MutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.ExtendedApplicationData
import com.galaxyjoy.cpuinfo.domain.model.SortOrder
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.wrapToResultFlow
import com.galaxyjoy.cpuinfo.util.wrapper.MyResult
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class ObservableApplicationsData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderApplications: DataProviderApplications,
    private val packageManager: PackageManager,
) : MutableInteractor<ObservableApplicationsData.Params, MyResult<List<ExtendedApplicationData>>>() {

    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Params): Flow<MyResult<List<ExtendedApplicationData>>> {
        return wrapToResultFlow {
            val apps = dataProviderApplications.getInstalledApplications(params.withSystemApps)
                .map {
                    ExtendedApplicationData(
                        name = it.loadLabel(packageManager).toString(),
                        packageName = it.packageName,
                        sourceDir = it.sourceDir,
                        nativeLibraryDir = it.nativeLibraryDir,
                        hasNativeLibs = it.hasNativeLibs(),
                        appIconUri = buildAppIconUri(it.packageName, it.icon)
                    )
                }
            when (params.sortOrder) {
                SortOrder.ASCENDING -> apps.sortedBy { it.name.lowercase() }
                SortOrder.DESCENDING -> apps.sortedByDescending { it.name.lowercase() }
                else -> apps
            }
        }
    }

    private fun ApplicationInfo.hasNativeLibs(): Boolean {
        return if (nativeLibraryDir != null) {
            val fileDir = File(nativeLibraryDir)
            val list = fileDir.listFiles()
            list != null && list.isNotEmpty()
        } else {
            false
        }
    }

    data class Params(
        val withSystemApps: Boolean,
        val sortOrder: SortOrder = SortOrder.NONE,
    )
}

/**
 * T2.30: [iconResId] comes straight off the [ApplicationInfo] already returned by
 * [DataProviderApplications.getInstalledApplications] — this used to re-fetch it via a
 * separate [PackageManager.getPackageInfo] Binder call per app (hundreds of redundant IPCs
 * on a 200+-app device) even though the exact same `icon` field was already in hand.
 *
 * Top-level (not a class member) and `internal` so it can be exercised directly from an
 * instrumented test: `Uri.Builder` is unmockable on the JVM unit-test stub (every fluent
 * setter returns null under `isReturnDefaultValues = true`), so this can only be verified
 * against the real `android.net.Uri` implementation on-device.
 */
internal fun buildAppIconUri(packageName: String, iconResId: Int): Uri {
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(packageName)
        .path(iconResId.toString())
        .build()
}
