package com.galaxyjoy.cpuinfo.feat.componentaudit

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import javax.inject.Inject

data class UnguardedComponent(
    val packageName: String,
    val appLabel: String,
    val componentName: String,
    val type: Type,
) {
    enum class Type { PROVIDER, RECEIVER }
}

/**
 * E19 "Exported Content-Provider/Receiver Audit" — see `doc/task/epic-05-new-ideas.md`. Deliberately
 * scoped WITHOUT `QUERY_ALL_PACKAGES` (Play Store restricts that permission heavily — same category
 * of risk that got the Floating Overlay idea skipped twice on this app; see `doc/task/quick_win.md`
 * #1). Reuses the exact mechanism [com.galaxyjoy.cpuinfo.data.provider.DataProviderApplications]
 * already relies on for the Applications tab: the `<queries><intent action=MAIN></queries>` entry
 * in `AndroidManifest.xml` makes ordinary launchable-ish apps visible to `PackageManager` without
 * any extra permission — so this only audits the same set of apps already visible there, not the
 * whole device. That scope limit is real and shown to the user in the sheet, not hidden.
 */
class ExportedComponentAuditProvider @Inject constructor(private val packageManager: PackageManager) {

    @SuppressLint("QueryPermissionsNeeded")
    @Suppress("DEPRECATION")
    fun scan(): ComponentAuditResult {
        val flags = PackageManager.GET_PROVIDERS or PackageManager.GET_RECEIVERS
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            packageManager.getInstalledPackages(flags)
        }

        return ComponentAuditResult(
            scannedPackageCount = packages.size,
            findings = packages.flatMap { pkg -> findUnguardedComponents(pkg, packageManager) },
        )
    }
}

data class ComponentAuditResult(val scannedPackageCount: Int, val findings: List<UnguardedComponent>)

/** Pure over a single already-fetched [PackageInfo] — testable without a real [PackageManager]. */
internal fun findUnguardedComponents(pkg: PackageInfo, packageManager: PackageManager): List<UnguardedComponent> {
    val label = pkg.applicationInfo?.loadLabel(packageManager)?.toString() ?: pkg.packageName

    val providers = pkg.providers.orEmpty()
        .filter { it.exported && it.readPermission.isNullOrEmpty() && it.writePermission.isNullOrEmpty() }
        .map { UnguardedComponent(pkg.packageName, label, it.name, UnguardedComponent.Type.PROVIDER) }

    val receivers = pkg.receivers.orEmpty()
        .filter { it.exported && it.permission.isNullOrEmpty() }
        .map { UnguardedComponent(pkg.packageName, label, it.name, UnguardedComponent.Type.RECEIVER) }

    return providers + receivers
}
