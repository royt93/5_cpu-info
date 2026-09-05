package com.galaxyjoy.cpuinfo.feat.backup

import android.content.Context
import android.net.Uri
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.setting.FrmSettings
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.LocaleManager
import com.galaxyjoy.cpuinfo.util.Prefs
import com.galaxyjoy.cpuinfo.util.ThemeHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * U32 — writes the current [BackupBundle] to a user-picked SAF `Uri` (via
 * `ActivityResultContracts.CreateDocument`, wired in `FrmSettings`), unlike every other exporter
 * in this app which shares to another app instead of saving a re-importable file.
 */
class BackupExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: Prefs,
    private val throttlePrefs: ThrottleResultPrefs,
    private val storagePrefs: StorageBenchResultPrefs,
    private val ramPrefs: RamBenchResultPrefs,
    private val gpuPrefs: GpuBenchResultPrefs,
    private val dispatchersProvider: DispatchersProvider,
) {

    fun buildBundle(): BackupBundle = BackupBundle(
        version = BackupBundle.CURRENT_VERSION,
        throttleHistory = throttlePrefs.getHistory(),
        storageHistory = storagePrefs.getHistory(),
        ramHistory = ramPrefs.getHistory(),
        gpuHistory = gpuPrefs.getHistory(),
        temperatureUnit = prefs.get(FrmSettings.KEY_TEMPERATURE_UNIT, "0"),
        theme = prefs.get(FrmSettings.KEY_THEME_CONFIG, ThemeHelper.DEFAULT_MODE),
        languageTag = LocaleManager.currentTag(),
    )

    suspend fun writeTo(uri: Uri): Boolean = withContext(dispatchersProvider.io) {
        runCatching {
            val out = context.contentResolver.openOutputStream(uri) ?: return@runCatching false
            out.use { it.write(BackupBundle.encode(buildBundle()).toByteArray(Charsets.UTF_8)) }
            true
        }.getOrDefault(false)
    }
}
