package com.galaxyjoy.cpuinfo.util

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.media.MediaCodecList
import android.media.MediaDrm
import android.os.Build
import android.os.Environment
import android.view.Display
import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderGpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class SystemInfoExporter @Inject constructor(
    private val dataProviderCpu: DataProviderCpu,
    private val dataProviderRam: DataProviderRam,
    private val dataProviderGpu: DataProviderGpu,
    private val dataNativeProviderCpu: DataNativeProviderCpu,
    private val sensorManager: SensorManager,
    private val cameraManager: CameraManager,
    private val displayManager: DisplayManager,
    private val dispatchersProvider: DispatchersProvider,
) {

    enum class Format(val mime: String, val ext: String) {
        TEXT("text/plain", "txt"),
        JSON("application/json", "json"),
    }

    fun exportSystemInfo(context: Context, scope: CoroutineScope, format: Format = Format.TEXT) {
        scope.launch {
            val body = withContext(dispatchersProvider.io) {
                when (format) {
                    Format.TEXT -> buildSystemInfoText()
                    Format.JSON -> buildSystemInfoJson().toString(2)
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mime
                putExtra(Intent.EXTRA_SUBJECT, "System Information - ${Build.MODEL}")
                putExtra(Intent.EXTRA_TEXT, body)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share System Info"))
        }
    }

    private fun buildSystemInfoText(): String = buildString {
        appendLine("=== SYSTEM INFORMATION ===")
        appendLine()

        appendLine("📱 DEVICE")
        appendLine("Model: ${Build.MODEL}")
        appendLine("Brand: ${Build.BRAND}")
        appendLine("Manufacturer: ${Build.MANUFACTURER}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine()

        appendLine("⚙️ CPU")
        appendLine("Processor: ${dataNativeProviderCpu.getCpuName()}")
        appendLine("ABI: ${dataProviderCpu.getAbi()}")
        val cpuCount = dataProviderCpu.getNumberOfCores()
        appendLine("Cores: $cpuCount")
        for (i in 0 until cpuCount) {
            val freq = dataProviderCpu.getCurrentFreq(i)
            if (freq > 0) appendLine("Core $i: ${freq}MHz")
        }
        appendLine()

        appendLine("💾 RAM")
        val totalRam = dataProviderRam.getTotalBytes() / GIGA
        val availRam = dataProviderRam.getAvailableBytes() / GIGA
        appendLine("Total: ${formatTwoDecimals(totalRam)}GB")
        appendLine("Available: ${formatTwoDecimals(availRam)}GB")
        appendLine("Usage: ${100 - dataProviderRam.getAvailablePercentage()}%")
        appendLine()

        appendLine("💿 STORAGE")
        val internalPath = Environment.getDataDirectory()
        appendLine("Internal Total: ${formatTwoDecimals(internalPath.totalSpace / GIGA)}GB")
        appendLine("Internal Free: ${formatTwoDecimals(internalPath.usableSpace / GIGA)}GB")
        appendLine()

        appendLine("🎮 GPU")
        appendLine("GLES Version: ${dataProviderGpu.getGlEsVersion()}")
        appendLine("Vulkan Version: ${dataProviderGpu.getVulkanVersion()}")
        appendLine()

        appendLine("🖥️ DISPLAY")
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        if (display != null) {
            appendLine("Refresh rate: ${formatTwoDecimals(display.refreshRate.toDouble())} Hz")
            appendLine("Supported modes: ${display.supportedModes.size}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val hdr = display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
                appendLine("HDR types: " + if (hdr.isEmpty()) "None" else hdr.size.toString())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appendLine("Wide color gamut: " + display.isWideColorGamut.yesNo())
            }
        }
        appendLine()

        appendLine("📡 SENSORS")
        appendLine("Total: ${sensorManager.getSensorList(Sensor.TYPE_ALL).size}")
        appendLine()

        appendLine("📷 CAMERAS")
        appendLine("Total: ${cameraIds().size}")
        appendLine()

        appendLine("🎬 MEDIA CODECS")
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        appendLine("Total: ${codecs.size}")
        appendLine("Decoders: ${codecs.count { !it.isEncoder }}")
        appendLine("Encoders: ${codecs.count { it.isEncoder }}")
        appendLine()

        appendLine("🔒 DRM")
        appendLine("Widevine: ${widevineSecurityLevel() ?: "Not supported"}")
        appendLine()

        appendLine("Generated by CPU Info App")
    }

    private fun buildSystemInfoJson(): JSONObject {
        val root = JSONObject()

        root.put(
            "device",
            JSONObject()
                .put("model", Build.MODEL)
                .put("brand", Build.BRAND)
                .put("manufacturer", Build.MANUFACTURER)
                .put("androidRelease", Build.VERSION.RELEASE)
                .put("sdkInt", Build.VERSION.SDK_INT),
        )

        val cpuJson = JSONObject()
            .put("name", dataNativeProviderCpu.getCpuName())
            .put("abi", dataProviderCpu.getAbi())
            .put("cores", dataProviderCpu.getNumberOfCores())
        val freqs = JSONArray()
        for (i in 0 until dataProviderCpu.getNumberOfCores()) {
            val freq = dataProviderCpu.getCurrentFreq(i)
            freqs.put(JSONObject().put("core", i).put("freqMhz", freq))
        }
        cpuJson.put("currentFrequencies", freqs)
        root.put("cpu", cpuJson)

        root.put(
            "ram",
            JSONObject()
                .put("totalBytes", dataProviderRam.getTotalBytes())
                .put("availableBytes", dataProviderRam.getAvailableBytes())
                .put("usagePercent", 100 - dataProviderRam.getAvailablePercentage()),
        )

        val internalPath = Environment.getDataDirectory()
        root.put(
            "storage",
            JSONObject()
                .put("internalTotalBytes", internalPath.totalSpace)
                .put("internalFreeBytes", internalPath.usableSpace),
        )

        root.put(
            "gpu",
            JSONObject()
                .put("glesVersion", dataProviderGpu.getGlEsVersion())
                .put("vulkanVersion", dataProviderGpu.getVulkanVersion()),
        )

        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        if (display != null) {
            val displayJson = JSONObject()
                .put("refreshRate", display.refreshRate)
                .put("supportedModesCount", display.supportedModes.size)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val hdrTypes = display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
                displayJson.put("hdrTypesCount", hdrTypes.size)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                displayJson.put("wideColorGamut", display.isWideColorGamut)
            }
            root.put("display", displayJson)
        }

        root.put(
            "sensors",
            JSONObject().put("total", sensorManager.getSensorList(Sensor.TYPE_ALL).size),
        )

        root.put(
            "cameras",
            JSONObject().put("total", cameraIds().size),
        )

        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        root.put(
            "mediaCodecs",
            JSONObject()
                .put("total", codecs.size)
                .put("decoders", codecs.count { !it.isEncoder })
                .put("encoders", codecs.count { it.isEncoder }),
        )

        root.put(
            "drm",
            JSONObject().put("widevineSecurityLevel", widevineSecurityLevel() ?: JSONObject.NULL),
        )

        return root
    }

    private fun cameraIds(): Array<String> = try {
        cameraManager.cameraIdList
    } catch (e: Exception) {
        Timber.w(e, "cameraIdList failed")
        emptyArray()
    }

    private fun widevineSecurityLevel(): String? {
        if (!MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) return null
        return try {
            val drm = MediaDrm(WIDEVINE_UUID)
            try {
                drm.getPropertyString("securityLevel").takeIf(String::isNotBlank)
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) drm.close() else @Suppress(
                    "DEPRECATION",
                ) drm.release()
            }
        } catch (e: Exception) {
            Timber.w(e, "widevine probe failed")
            null
        }
    }

    private fun Boolean.yesNo() = if (this) "Yes" else "No"

    /** Always renders with a "." decimal point, regardless of the device's default locale. */
    internal fun formatTwoDecimals(value: Double): String = "%.2f".format(Locale.US, value)

    companion object {
        private const val GIGA = 1024.0 * 1024.0 * 1024.0
        private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
    }
}
