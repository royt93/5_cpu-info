package com.galaxyjoy.cpuinfo.feat.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import javax.inject.Inject

/**
 * Reads an app's declared + runtime-granted permissions via [PackageManager] and hands them to
 * the pure [AppPermissionEvaluator]. [PackageInfo.requestedPermissionsFlags] is index-aligned
 * with [PackageInfo.requestedPermissions] — flag bit [PackageInfo.REQUESTED_PERMISSION_GRANTED]
 * tells us which of the requested permissions are actually granted right now.
 */
class AppPermissionProvider @Inject constructor(
    private val packageManager: PackageManager,
) {

    @Suppress("DEPRECATION")
    fun evaluate(packageName: String): AppPermissionEvaluator.Result? {
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
                )
            } else {
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }

        val requested = packageInfo.requestedPermissions?.toList() ?: emptyList()
        val grantedFlags = packageInfo.requestedPermissionsFlags
        val granted = requested.filterIndexed { index, _ ->
            grantedFlags != null &&
                index < grantedFlags.size &&
                (grantedFlags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
        }.toSet()

        return AppPermissionEvaluator.evaluate(requested, granted)
    }
}
