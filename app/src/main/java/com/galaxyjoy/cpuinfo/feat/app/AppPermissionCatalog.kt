package com.galaxyjoy.cpuinfo.feat.app

/**
 * F05 "App Permission & SDK Inventory" — classifies Android's stable, well-known runtime
 * ("dangerous") permission constants into privacy-relevant categories. Hardcoded rather than
 * queried via [android.content.pm.PackageManager.getPermissionInfo] (which would require an
 * Android context to unit test) — these constant names and their dangerous/normal split have
 * been stable across API levels, matching the same reference-table approach as [ChipCatalog][
 * com.galaxyjoy.cpuinfo.feat.truth.ChipCatalog].
 */
object AppPermissionCatalog {

    enum class Category { LOCATION, CAMERA, MICROPHONE, CONTACTS, CALL_SMS, STORAGE, CALENDAR, BODY_SENSORS, OTHER }

    private val DANGEROUS_PERMISSIONS: Set<String> = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.CALL_PHONE",
        "android.permission.ANSWER_PHONE_CALLS",
        "android.permission.ADD_VOICEMAIL",
        "android.permission.USE_SIP",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_WAP_PUSH",
        "android.permission.RECEIVE_MMS",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
        "android.permission.BODY_SENSORS",
        "android.permission.BODY_SENSORS_BACKGROUND",
        "android.permission.ACTIVITY_RECOGNITION",
        "android.permission.BLUETOOTH_ADVERTISE",
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.UWB_RANGING",
    )

    private val CATEGORY_MAP: Map<String, Category> = buildMap {
        putCategory(Category.LOCATION, "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "ACCESS_BACKGROUND_LOCATION")
        putCategory(Category.CAMERA, "CAMERA")
        putCategory(Category.MICROPHONE, "RECORD_AUDIO")
        putCategory(Category.CONTACTS, "READ_CONTACTS", "WRITE_CONTACTS", "GET_ACCOUNTS")
        putCategory(
            Category.CALL_SMS,
            "READ_CALL_LOG", "WRITE_CALL_LOG", "PROCESS_OUTGOING_CALLS", "READ_PHONE_STATE",
            "READ_PHONE_NUMBERS", "CALL_PHONE", "ANSWER_PHONE_CALLS", "ADD_VOICEMAIL", "USE_SIP",
            "SEND_SMS", "RECEIVE_SMS", "READ_SMS", "RECEIVE_WAP_PUSH", "RECEIVE_MMS",
        )
        putCategory(
            Category.STORAGE,
            "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "READ_MEDIA_IMAGES",
            "READ_MEDIA_VIDEO", "READ_MEDIA_AUDIO", "READ_MEDIA_VISUAL_USER_SELECTED",
        )
        putCategory(Category.CALENDAR, "READ_CALENDAR", "WRITE_CALENDAR")
        putCategory(Category.BODY_SENSORS, "BODY_SENSORS", "BODY_SENSORS_BACKGROUND", "ACTIVITY_RECOGNITION")
    }

    private fun MutableMap<String, Category>.putCategory(category: Category, vararg shortNames: String) {
        shortNames.forEach { put("android.permission.$it", category) }
    }

    fun isDangerous(permission: String): Boolean = permission in DANGEROUS_PERMISSIONS

    fun categoryFor(permission: String): Category = CATEGORY_MAP[permission] ?: Category.OTHER

    /** e.g. "android.permission.ACCESS_FINE_LOCATION" -> "Access Fine Location". */
    fun shortLabel(permission: String): String {
        val raw = permission.substringAfterLast('.')
        return raw.split('_').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar(Char::uppercase)
        }
    }
}
