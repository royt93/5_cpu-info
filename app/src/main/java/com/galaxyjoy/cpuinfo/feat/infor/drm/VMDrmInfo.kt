package com.galaxyjoy.cpuinfo.feat.infor.drm

import android.media.MediaDrm
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Surface DRM info: Widevine security level (L1/L2/L3), HDCP level, supported schemes.
 *
 * L1 = SD/HD/UHD playback OK (Netflix HD, Disney+ HD, Amazon HD).
 * L3 = SD only (software DRM, no TEE).
 */
@HiltViewModel
class VMDrmInfo @Inject constructor() : ViewModel() {

    val listLiveData = ListLiveData<Pair<String, String>>()

    init {
        if (listLiveData.isEmpty()) {
            populate()
        }
    }

    private fun populate() {
        val widevine = inspectScheme("Widevine", WIDEVINE_UUID)
        listLiveData.addAll(widevine)

        val playReady = inspectScheme("PlayReady", PLAYREADY_UUID)
        listLiveData.addAll(playReady)

        val clearKey = inspectScheme("ClearKey", CLEARKEY_UUID)
        listLiveData.addAll(clearKey)
    }

    private fun inspectScheme(name: String, uuid: UUID): List<Pair<String, String>> {
        if (!MediaDrm.isCryptoSchemeSupported(uuid)) {
            return listOf("$name supported" to "No")
        }

        val rows = mutableListOf<Pair<String, String>>()
        rows.add("$name supported" to "Yes")

        val drm: MediaDrm? = try {
            MediaDrm(uuid)
        } catch (e: Exception) {
            Timber.w(e, "Cannot open MediaDrm for $name")
            null
        }

        drm?.let {
            try {
                it.getPropertyString("securityLevel").takeIf(String::isNotBlank)?.let { lvl ->
                    rows.add("$name security level" to lvl)
                }
            } catch (_: Exception) {
                // property not supported by this scheme
            }
            try {
                it.getPropertyString("hdcpLevel").takeIf(String::isNotBlank)?.let { hdcp ->
                    rows.add("$name HDCP level" to hdcp)
                }
            } catch (_: Exception) {
                // ignore
            }
            try {
                it.getPropertyString("maxHdcpLevel").takeIf(String::isNotBlank)?.let { max ->
                    rows.add("$name max HDCP" to max)
                }
            } catch (_: Exception) {
                // ignore
            }
            try {
                it.getPropertyString("version").takeIf(String::isNotBlank)?.let { v ->
                    rows.add("$name version" to v)
                }
            } catch (_: Exception) {
                // ignore
            }
            it.releaseSafely()
        }

        return rows
    }

    private fun MediaDrm.releaseSafely() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
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
