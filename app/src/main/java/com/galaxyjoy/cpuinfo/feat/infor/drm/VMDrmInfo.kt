package com.galaxyjoy.cpuinfo.feat.infor.drm

import android.content.res.Resources
import android.media.MediaDrm
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.R
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
class VMDrmInfo @Inject constructor(
    private val resources: Resources,
) : ViewModel() {

    val listLiveData = ListLiveData<Pair<String, String>>()

    init {
        if (listLiveData.isEmpty()) {
            populate()
        }
    }

    private fun populate() {
        listLiveData.addAll(inspectScheme("Widevine", WIDEVINE_UUID))
        listLiveData.addAll(inspectScheme("PlayReady", PLAYREADY_UUID))
        listLiveData.addAll(inspectScheme("ClearKey", CLEARKEY_UUID))
    }

    private fun inspectScheme(name: String, uuid: UUID): List<Pair<String, String>> {
        val supportedLabel = resources.getString(R.string.drm_supported, name)
        if (!MediaDrm.isCryptoSchemeSupported(uuid)) {
            return listOf(supportedLabel to resources.getString(R.string.no))
        }

        val rows = mutableListOf<Pair<String, String>>()
        rows.add(supportedLabel to resources.getString(R.string.yes))

        val drm: MediaDrm? = try {
            MediaDrm(uuid)
        } catch (e: Exception) {
            Timber.w(e, "Cannot open MediaDrm for $name")
            null
        }

        drm?.let {
            it.readProperty("securityLevel")?.let { lvl ->
                rows.add(resources.getString(R.string.drm_security_level, name) to lvl)
            }
            it.readProperty("hdcpLevel")?.let { hdcp ->
                rows.add(resources.getString(R.string.drm_hdcp_level, name) to hdcp)
            }
            it.readProperty("maxHdcpLevel")?.let { max ->
                rows.add(resources.getString(R.string.drm_max_hdcp, name) to max)
            }
            it.readProperty("version")?.let { v ->
                rows.add(resources.getString(R.string.drm_version, name) to v)
            }
            it.releaseSafely()
        }

        return rows
    }

    private fun MediaDrm.readProperty(key: String): String? = try {
        getPropertyString(key).takeIf(String::isNotBlank)
    } catch (_: Exception) {
        null
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
