package com.galaxyjoy.cpuinfo.data.provider

import android.media.MediaDrm
import android.os.Build
import com.galaxyjoy.cpuinfo.domain.model.DrmSchemeData
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * L1 = SD/HD/UHD playback OK (Netflix HD, Disney+ HD, Amazon HD).
 * L3 = SD only (software DRM, no TEE).
 */
class DataProviderDrm @Inject constructor() {

    fun getSchemes(): List<DrmSchemeData> = listOf(
        inspectScheme("Widevine", WIDEVINE_UUID),
        inspectScheme("PlayReady", PLAYREADY_UUID),
        inspectScheme("ClearKey", CLEARKEY_UUID),
    )

    private fun inspectScheme(name: String, uuid: UUID): DrmSchemeData {
        if (!MediaDrm.isCryptoSchemeSupported(uuid)) {
            return DrmSchemeData(name, supported = false, null, null, null, null)
        }

        val drm: MediaDrm? = try {
            MediaDrm(uuid)
        } catch (e: Exception) {
            Timber.w(e, "Cannot open MediaDrm for $name")
            null
        }

        val data = DrmSchemeData(
            name = name,
            supported = true,
            securityLevel = drm?.readProperty("securityLevel"),
            hdcpLevel = drm?.readProperty("hdcpLevel"),
            maxHdcpLevel = drm?.readProperty("maxHdcpLevel"),
            version = drm?.readProperty("version"),
        )
        drm?.releaseSafely()

        return data
    }

    private fun MediaDrm.readProperty(key: String): String? = try {
        getPropertyString(key).takeIf(String::isNotBlank)
    } catch (_: Exception) {
        null
    }

    private fun MediaDrm.releaseSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                close()
            } else {
                @Suppress("DEPRECATION")
                release()
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    companion object {
        // UUIDs from https://dashif.org/identifiers/content_protection/
        private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
        private val PLAYREADY_UUID = UUID(-0x65fb0f8667bfbd7aL, -0x546d19a41f77a06bL)
        private val CLEARKEY_UUID = UUID(-0x1d8e62a7567a4c37L, 0x781ab030af78d30eL)
    }
}
