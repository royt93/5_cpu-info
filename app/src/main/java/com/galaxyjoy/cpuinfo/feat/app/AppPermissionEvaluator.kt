package com.galaxyjoy.cpuinfo.feat.app

object AppPermissionEvaluator {

    data class PermissionEntry(
        val name: String,
        val label: String,
        val isDangerous: Boolean,
        val isGranted: Boolean,
        val category: AppPermissionCatalog.Category,
    )

    data class Result(
        val entries: List<PermissionEntry>,
        val dangerousCount: Int,
        val totalCount: Int,
    )

    /**
     * @param requestedPermissions permissions declared in the app's manifest.
     * @param grantedPermissions subset of [requestedPermissions] currently granted at runtime.
     * Dangerous permissions sort first (then alphabetically by label) so the privacy-relevant
     * rows are visible without scrolling on apps with long normal-permission lists.
     */
    fun evaluate(requestedPermissions: List<String>, grantedPermissions: Set<String>): Result {
        val entries = requestedPermissions.map { name ->
            PermissionEntry(
                name = name,
                label = AppPermissionCatalog.shortLabel(name),
                isDangerous = AppPermissionCatalog.isDangerous(name),
                isGranted = name in grantedPermissions,
                category = AppPermissionCatalog.categoryFor(name),
            )
        }.sortedWith(compareByDescending<PermissionEntry> { it.isDangerous }.thenBy { it.label })

        return Result(
            entries = entries,
            dangerousCount = entries.count { it.isDangerous },
            totalCount = entries.size,
        )
    }
}
