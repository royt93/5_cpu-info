package com.galaxyjoy.cpuinfo.feat.infor.audio

import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.Spatializer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Surface audio hardware capabilities via [AudioManager]/[AudioDeviceInfo] — replaces the old
 * `/proc/asound` (ALSA) read attempted in [com.galaxyjoy.cpuinfo.feat.infor.hardware.VMHardwareInfo],
 * which SELinux blocks outright since Android 7.
 *
 * Scoped to zero-permission info: native audio pipeline params, low-latency/pro-audio/spatial
 * feature flags, and per-output-device channel/sample-rate/encoding capabilities. Bluetooth codec
 * (LDAC/aptX) is deliberately out of scope — reading it needs BLUETOOTH_CONNECT + a paired A2DP
 * device, which doesn't fit a read-only metadata tab.
 */
@HiltViewModel
class VMAudioInfo @Inject constructor(
    private val audioManager: AudioManager,
    private val packageManager: PackageManager,
    private val resources: Resources,
) : ViewModel() {

    val listLiveData = ListLiveData<Pair<String, String>>()

    init {
        if (listLiveData.isEmpty()) {
            populate()
        }
    }

    private fun populate() {
        listLiveData.addAll(generalSection())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            listLiveData.addAll(spatialSection())
        }
        outputDevices().forEach { listLiveData.addAll(deviceSection(it)) }
    }

    private fun generalSection(): List<Pair<String, String>> {
        val yes = resources.getString(R.string.yes)
        val no = resources.getString(R.string.no)
        val rows = mutableListOf<Pair<String, String>>()
        rows.add(resources.getString(R.string.audio_section_general) to "")
        audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.let {
            rows.add(resources.getString(R.string.audio_native_sample_rate) to resources.getString(R.string.audio_hz_value, it))
        }
        audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.let {
            rows.add(resources.getString(R.string.audio_native_buffer_size) to it)
        }
        rows.add(
            resources.getString(R.string.audio_low_latency_support) to
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)) yes else no,
        )
        rows.add(
            resources.getString(R.string.audio_pro_audio_support) to
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)) yes else no,
        )
        val micCount = if (packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            audioManager.getDevicesSafe(AudioManager.GET_DEVICES_INPUTS).size
        } else {
            0
        }
        rows.add(resources.getString(R.string.audio_microphone_count) to micCount.toString())
        rows.add(resources.getString(R.string.audio_max_music_volume_steps) to audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toString())
        return rows
    }

    @RequiresApi(Build.VERSION_CODES.S_V2)
    private fun spatialSection(): List<Pair<String, String>> {
        val spatializer = audioManager.spatializer
        val yes = resources.getString(R.string.yes)
        val no = resources.getString(R.string.no)
        val rows = mutableListOf<Pair<String, String>>()
        rows.add(resources.getString(R.string.audio_section_spatial) to "")
        rows.add(resources.getString(R.string.audio_spatial_available) to if (spatializer.isAvailable) yes else no)
        rows.add(resources.getString(R.string.audio_spatial_enabled) to if (spatializer.isEnabled) yes else no)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rows.add(resources.getString(R.string.audio_spatial_head_tracker) to if (spatializer.isHeadTrackerAvailable) yes else no)
        }
        return rows
    }

    private fun outputDevices(): List<AudioDeviceInfo> =
        audioManager.getDevicesSafe(AudioManager.GET_DEVICES_OUTPUTS).sortedBy { it.deviceTypeLabel() }

    private fun deviceSection(device: AudioDeviceInfo): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        rows.add(device.deviceTypeLabel() to "")
        if (device.channelCounts.isNotEmpty()) {
            rows.add(
                resources.getString(R.string.audio_device_channels) to
                    device.channelCounts.sorted().joinToString(", "),
            )
        }
        if (device.sampleRates.isNotEmpty()) {
            rows.add(
                resources.getString(R.string.audio_device_sample_rates) to
                    resources.getString(R.string.audio_hz_value, device.sampleRates.sorted().joinToString(", ")),
            )
        }
        val encodingLabels = device.encodings.toList().mapNotNull { it.encodingLabel() }.distinct()
        if (encodingLabels.isNotEmpty()) {
            rows.add(resources.getString(R.string.audio_device_encodings) to encodingLabels.joinToString(", "))
        }
        return rows
    }

    private fun AudioManager.getDevicesSafe(flags: Int): Array<AudioDeviceInfo> =
        try {
            getDevices(flags)
        } catch (_: Exception) {
            emptyArray()
        }

    private fun AudioDeviceInfo.deviceTypeLabel(): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in speaker"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_DOCK -> "Dock"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB accessory"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB device"
        AudioDeviceInfo.TYPE_TELEPHONY -> "Telephony"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "Line analog"
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Line digital"
        AudioDeviceInfo.TYPE_FM -> "FM"
        AudioDeviceInfo.TYPE_AUX_LINE -> "Aux line"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
        AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing aid"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE headset"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE speaker"
        AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
        AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI eARC"
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> "Bluetooth LE broadcast"
        AudioDeviceInfo.TYPE_DOCK_ANALOG -> "Dock (analog)"
        else -> "Other (type $type)"
    }

    private fun Int.encodingLabel(): String? = when (this) {
        AudioFormat.ENCODING_PCM_16BIT -> "PCM 16-bit"
        AudioFormat.ENCODING_PCM_8BIT -> "PCM 8-bit"
        AudioFormat.ENCODING_PCM_FLOAT -> "PCM float"
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM 24-bit"
        AudioFormat.ENCODING_PCM_32BIT -> "PCM 32-bit"
        AudioFormat.ENCODING_AC3 -> "Dolby Digital (AC-3)"
        AudioFormat.ENCODING_E_AC3 -> "Dolby Digital Plus (E-AC-3)"
        AudioFormat.ENCODING_DTS -> "DTS"
        AudioFormat.ENCODING_DTS_HD -> "DTS-HD"
        AudioFormat.ENCODING_AAC_LC -> "AAC"
        AudioFormat.ENCODING_DOLBY_TRUEHD -> "Dolby TrueHD"
        AudioFormat.ENCODING_E_AC3_JOC -> "Dolby Atmos (E-AC-3 JOC)"
        AudioFormat.ENCODING_AC4 -> "Dolby AC-4"
        else -> null
    }
}
