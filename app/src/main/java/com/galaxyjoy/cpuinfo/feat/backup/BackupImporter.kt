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
 * U32 — reads a [BackupBundle] from a user-picked SAF `Uri` and, if valid, [apply]s it to real
 * app state. [readFrom] alone never mutates anything — `FrmSettings` decides whether to call
 * [apply] (e.g. after the user confirms), keeping "parse" and "commit" separate so a malformed
 * file can be rejected with zero side effects.
 */
class BackupImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: Prefs,
    private val throttlePrefs: ThrottleResultPrefs,
    private val storagePrefs: StorageBenchResultPrefs,
    private val ramPrefs: RamBenchResultPrefs,
    private val gpuPrefs: GpuBenchResultPrefs,
    private val dispatchersProvider: DispatchersProvider,
) {

    suspend fun readFrom(uri: Uri): BackupBundle? = withContext(dispatchersProvider.io) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?.toString(Charsets.UTF_8)
                ?.let(BackupBundle::decode)
        }.getOrNull()
    }

    /** Replaces (not merges) each benchmark's history, and overwrites the temperature-unit/theme
     * prefs + app locale only for the fields actually present in [bundle] — an older/partial
     * backup with `null` settings fields leaves those untouched rather than resetting them. */
    fun apply(bundle: BackupBundle) {
        throttlePrefs.replaceHistory(bundle.throttleHistory ?: emptyList())
        storagePrefs.replaceHistory(bundle.storageHistory ?: emptyList())
        ramPrefs.replaceHistory(bundle.ramHistory ?: emptyList())
        gpuPrefs.replaceHistory(bundle.gpuHistory ?: emptyList())
        bundle.temperatureUnit?.let { prefs.insert(FrmSettings.KEY_TEMPERATURE_UNIT, it) }
        bundle.theme?.let {
            prefs.insert(FrmSettings.KEY_THEME_CONFIG, it)
            ThemeHelper.applyTheme(it)
        }
        bundle.languageTag?.let { LocaleManager.apply(it) }
    }
}
