package com.galaxyjoy.cpuinfo.feat.setting

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.lifecycle.lifecycleScope
import com.galaxyjoy.cpuinfo.BuildConfig
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ext.openBrowserPolicy
import com.galaxyjoy.cpuinfo.feat.backup.BackupExporter
import com.galaxyjoy.cpuinfo.feat.backup.BackupImporter
import com.galaxyjoy.cpuinfo.feat.benchhistory.BenchHistoryExporter
import com.galaxyjoy.cpuinfo.feat.benchreminder.BenchReminderScheduler
import com.galaxyjoy.cpuinfo.feat.devicereport.DeviceReportExporter
import com.galaxyjoy.cpuinfo.feat.fleet.FleetCompareBottomSheet
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertScheduler
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.snapshot.HardwareSnapshotBottomSheet
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import com.galaxyjoy.cpuinfo.feat.usbbt.UsbBluetoothBottomSheet
import com.galaxyjoy.cpuinfo.feat.vip.ActVip
import com.galaxyjoy.cpuinfo.feat.vipreport.VipDiagnosticReportBottomSheet
import com.galaxyjoy.cpuinfo.util.ThemeHelper
import com.roy.sdkadbmob.AdManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import moreApp
import rateApp
import shareApp
import javax.inject.Inject

/** U28 — [DeviceReportExporter] is Hilt-injected (its own dependency graph — `DeviceCardProvider`
 * pulls in 6 further providers — is too deep to keep hand-constructing plain-`Context` instances
 * for, unlike the shallow single-`Context`-constructor classes U21/U25 hand-construct elsewhere in
 * this file). `FrmSettings` wasn't a Hilt entry point before; making it one is inert for every
 * existing preference here (none of them use `@Inject`) and is the standard way every other
 * screen in this app already gets its dependencies. */
@AndroidEntryPoint
class FrmSettings : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var deviceReportExporter: DeviceReportExporter

    @Inject
    lateinit var backupExporter: BackupExporter

    @Inject
    lateinit var backupImporter: BackupImporter

    companion object {
        const val KEY_TEMPERATURE_UNIT = "temperature_unit"
        const val KEY_THEME_CONFIG = "key_theme"
        const val KEY_HEALTH_ALERT = "key_health_alert"
        const val KEY_BENCH_REMINDER = "key_bench_reminder"
    }

    /** Must be registered before the fragment reaches CREATED — a class-level property, not
     * something created lazily inside [onCreatePreferences]. */
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pref = findPreference<SwitchPreferenceCompat>(KEY_HEALTH_ALERT)
        if (granted) {
            HealthAlertScheduler.schedule(requireContext())
            pref?.isChecked = true
        } else {
            pref?.isChecked = false
        }
    }

    /** U30 — separate launcher from [requestNotificationPermission] rather than a shared one
     * branching on "which pref triggered this": keeps each toggle's wiring independently readable
     * and independently testable, at the cost of a few duplicated lines (same tradeoff already
     * made by the 4 near-identical `*ResultPrefs` classes elsewhere in this codebase). */
    private val requestBenchReminderNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pref = findPreference<SwitchPreferenceCompat>(KEY_BENCH_REMINDER)
        if (granted) {
            BenchReminderScheduler.schedule(requireContext())
            pref?.isChecked = true
        } else {
            pref?.isChecked = false
        }
    }

    /** U32 — `"application/json"` is just the SAF picker's suggested filename/MIME, the user can
     * rename it on save. */
    private val createBackupDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val success = backupExporter.writeTo(uri)
            Toast.makeText(
                requireContext(),
                if (success) R.string.backup_export_success else R.string.backup_export_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    /** U32 — [BackupImporter.readFrom] only parses; this callback decides whether to actually
     * [BackupImporter.apply] it, so a malformed/foreign file is rejected with zero side effects
     * on existing data. */
    private val openBackupDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val bundle = backupImporter.readFrom(uri)
            if (bundle == null) {
                Toast.makeText(requireContext(), R.string.backup_import_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            backupImporter.apply(bundle)
            findPreference<Preference>(KEY_TEMPERATURE_UNIT)?.summary =
                readTemperatureUnitLabel(currentTemperatureUnitValue())
            findPreference<Preference>(KEY_THEME_CONFIG)?.summary = readThemeLabel(currentThemeValue())
            Toast.makeText(requireContext(), R.string.backup_import_success, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref)

        findPreference<Preference>("key_vip_management")?.setOnPreferenceClickListener {
            activity?.let { ActVip.start(it) }
            true
        }

        val rateAppPreference: Preference? = findPreference("key_rate_app")
        rateAppPreference?.setOnPreferenceClickListener {
            activity?.let {
                it.rateApp(it.packageName)
            }
            true
        }
        val moreAppPreference: Preference? = findPreference("key_more_app")
        moreAppPreference?.setOnPreferenceClickListener {
            activity?.moreApp()
            true
        }
        val shareAppPreference: Preference? = findPreference("key_share_app")
        shareAppPreference?.setOnPreferenceClickListener {
            activity?.shareApp()
            true
        }
        val policyAppPreference: Preference? = findPreference("key_policy_app")
        policyAppPreference?.setOnPreferenceClickListener {
            activity?.openBrowserPolicy()
            true
        }
        val versionAppPreference: Preference? = findPreference("key_version_app")
        versionAppPreference?.title = BuildConfig.VERSION_NAME
        versionAppPreference?.setOnPreferenceClickListener {
            //do nothing
            true
        }

        wireLanguagePref()
        wireTemperatureUnitPref()
        wireThemePref()
        wireHardwareSnapshotPref()
        wireUsbBluetoothPref()
        wireFleetComparePref()
        wireVipDiagnosticHistoryPref()
        wireHealthAlertPref()
        wireBenchReminderPref()
        wireExportBenchHistoryPref()
        wireExportFullReportPref()
        wireBackupPrefs()

        listenForBottomSheetResults()
    }

    private fun wireLanguagePref() {
        findPreference<Preference>("key_change_language")?.setOnPreferenceClickListener {
            val fm = requireActivity().supportFragmentManager
            if (!fm.isStateSaved && fm.findFragmentByTag(LanguagePickerBottomSheet.TAG) == null) {
                LanguagePickerBottomSheet().show(fm, LanguagePickerBottomSheet.TAG)
            }
            true
        }
    }

    private fun wireTemperatureUnitPref() {
        val pref = findPreference<Preference>(KEY_TEMPERATURE_UNIT) ?: return
        pref.summary = readTemperatureUnitLabel(currentTemperatureUnitValue())
        pref.setOnPreferenceClickListener {
            val fm = childFragmentManager
            if (!fm.isStateSaved && fm.findFragmentByTag(TemperatureUnitBottomSheet.TAG) == null) {
                TemperatureUnitBottomSheet.newInstance(currentTemperatureUnitValue())
                    .show(fm, TemperatureUnitBottomSheet.TAG)
            }
            true
        }
    }

    private fun wireThemePref() {
        val pref = findPreference<Preference>(KEY_THEME_CONFIG) ?: return
        pref.summary = readThemeLabel(currentThemeValue())
        pref.setOnPreferenceClickListener {
            val fm = childFragmentManager
            if (!fm.isStateSaved && fm.findFragmentByTag(ThemePickerBottomSheet.TAG) == null) {
                ThemePickerBottomSheet.newInstance(currentThemeValue())
                    .show(fm, ThemePickerBottomSheet.TAG)
            }
            true
        }
    }

    private fun wireHardwareSnapshotPref() {
        findPreference<Preference>("key_hardware_snapshot")?.setOnPreferenceClickListener {
            val fm = childFragmentManager
            if (!fm.isStateSaved && fm.findFragmentByTag(HardwareSnapshotBottomSheet.TAG) == null) {
                HardwareSnapshotBottomSheet().show(fm, HardwareSnapshotBottomSheet.TAG)
            }
            true
        }
    }

    private fun wireUsbBluetoothPref() {
        findPreference<Preference>("key_usb_bt")?.setOnPreferenceClickListener {
            val fm = childFragmentManager
            if (!fm.isStateSaved && fm.findFragmentByTag(UsbBluetoothBottomSheet.TAG) == null) {
                UsbBluetoothBottomSheet().show(fm, UsbBluetoothBottomSheet.TAG)
            }
            true
        }
    }

    private fun wireFleetComparePref() {
        findPreference<Preference>("key_fleet_compare")?.setOnPreferenceClickListener {
            val fm = childFragmentManager
            if (!fm.isStateSaved && fm.findFragmentByTag(FleetCompareBottomSheet.TAG) == null) {
                FleetCompareBottomSheet().show(fm, FleetCompareBottomSheet.TAG)
            }
            true
        }
    }

    /**
     * U07 — the only content feature in the app actually gated on live VIP status (everywhere
     * else `AdManager.isVipByKeyActive()` only affects ad display / the VIP screen itself). Not a
     * VIP member -> route straight to [ActVip] to upsell, same as tapping "VIP Management".
     */
    private fun wireVipDiagnosticHistoryPref() {
        findPreference<Preference>("key_vip_diagnostic_history")?.setOnPreferenceClickListener {
            if (!AdManager.isVipByKeyActive()) {
                activity?.let { ActVip.start(it) }
                return@setOnPreferenceClickListener true
            }
            val fm = childFragmentManager
            if (!fm.isStateSaved && fm.findFragmentByTag(VipDiagnosticReportBottomSheet.TAG) == null) {
                VipDiagnosticReportBottomSheet().show(fm, VipDiagnosticReportBottomSheet.TAG)
            }
            true
        }
    }

    /**
     * U19 — turning this on requests `POST_NOTIFICATIONS` (API 33+ only; older devices need no
     * runtime grant) rather than asking at first launch. Declining leaves the switch off — no
     * repeated nagging, matches CLAUDE.md's "im lặng bỏ qua" guidance for this exact feature.
     */
    private fun wireHealthAlertPref() {
        val pref = findPreference<SwitchPreferenceCompat>(KEY_HEALTH_ALERT) ?: return
        pref.setOnPreferenceChangeListener { _, newValue ->
            val enabling = newValue as Boolean
            if (!enabling) {
                HealthAlertScheduler.cancel(requireContext())
                return@setOnPreferenceChangeListener true
            }

            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                false // async callback flips the switch itself once the user answers
            } else {
                HealthAlertScheduler.schedule(requireContext())
                true
            }
        }
    }

    /**
     * U30 — same permission-request shape as [wireHealthAlertPref], own scheduler/launcher (see
     * [requestBenchReminderNotificationPermission] for why not shared).
     */
    private fun wireBenchReminderPref() {
        val pref = findPreference<SwitchPreferenceCompat>(KEY_BENCH_REMINDER) ?: return
        pref.setOnPreferenceChangeListener { _, newValue ->
            val enabling = newValue as Boolean
            if (!enabling) {
                BenchReminderScheduler.cancel(requireContext())
                return@setOnPreferenceChangeListener true
            }

            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                requestBenchReminderNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                false
            } else {
                BenchReminderScheduler.schedule(requireContext())
                true
            }
        }
    }

    /** U25 — direct share-sheet action, no confirmation dialog, matching the app's other
     * one-tap exporters (`DeviceCardExporter`/`SystemInfoExporter`). `*ResultPrefs` constructors
     * are plain `(Context)` despite being `@Inject`-annotated for Hilt elsewhere (same fact U21's
     * `LastBenchWidgetProvider` already relies on) — `FrmSettings` isn't a Hilt entry point, so
     * they're built directly here instead of injected. */
    private fun wireExportBenchHistoryPref() {
        findPreference<Preference>("key_export_bench_history")?.setOnPreferenceClickListener {
            val context = requireContext().applicationContext
            val exporter = BenchHistoryExporter(
                throttlePrefs = ThrottleResultPrefs(context),
                storagePrefs = StorageBenchResultPrefs(context),
                ramPrefs = RamBenchResultPrefs(context),
                gpuPrefs = GpuBenchResultPrefs(context),
            )
            exporter.exportHistory(requireContext(), lifecycleScope)
            true
        }
    }

    /** U28 — Hilt-injected [deviceReportExporter], unlike [wireExportBenchHistoryPref]'s
     * hand-constructed `*ResultPrefs` (see this file's top-level doc comment for why). */
    private fun wireExportFullReportPref() {
        findPreference<Preference>("key_export_full_report")?.setOnPreferenceClickListener {
            deviceReportExporter.exportFullReport(lifecycleScope)
            true
        }
    }

    /** U32 — export writes a re-importable JSON file via SAF; import reads one back and
     * overwrites the 4 benchmark histories + carried-over settings. */
    private fun wireBackupPrefs() {
        findPreference<Preference>("key_export_backup")?.setOnPreferenceClickListener {
            createBackupDocument.launch("cpuinfo_backup.json")
            true
        }
        findPreference<Preference>("key_import_backup")?.setOnPreferenceClickListener {
            openBackupDocument.launch(arrayOf("application/json"))
            true
        }
    }

    private fun listenForBottomSheetResults() {
        val fm: FragmentManager = childFragmentManager
        fm.setFragmentResultListener(
            TemperatureUnitBottomSheet.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val value = bundle.getString(TemperatureUnitBottomSheet.ARG_VALUE) ?: return@setFragmentResultListener
            preferenceScreen.sharedPreferences
                ?.edit()?.putString(KEY_TEMPERATURE_UNIT, value)?.apply()
            findPreference<Preference>(KEY_TEMPERATURE_UNIT)?.summary = readTemperatureUnitLabel(value)
        }
        fm.setFragmentResultListener(
            ThemePickerBottomSheet.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val value = bundle.getString(ThemePickerBottomSheet.ARG_VALUE) ?: return@setFragmentResultListener
            preferenceScreen.sharedPreferences
                ?.edit()?.putString(KEY_THEME_CONFIG, value)?.apply()
            findPreference<Preference>(KEY_THEME_CONFIG)?.summary = readThemeLabel(value)
        }
    }

    private fun currentTemperatureUnitValue(): String =
        preferenceScreen.sharedPreferences?.getString(KEY_TEMPERATURE_UNIT, "0") ?: "0"

    private fun currentThemeValue(): String =
        preferenceScreen.sharedPreferences?.getString(KEY_THEME_CONFIG, ThemeHelper.DEFAULT_MODE)
            ?: ThemeHelper.DEFAULT_MODE

    private fun readTemperatureUnitLabel(value: String): String {
        val names = resources.getStringArray(R.array.prefTemperatureNames)
        val values = resources.getStringArray(R.array.prefTemperatureValues)
        val idx = values.indexOf(value).takeIf { it >= 0 } ?: 0
        return names.getOrElse(idx) { names.first() }
    }

    private fun readThemeLabel(value: String): String {
        val names = resources.getStringArray(R.array.themeListArray)
        val values = resources.getStringArray(R.array.themeEntryArray)
        val idx = values.indexOf(value).takeIf { it >= 0 } ?: 0
        return names.getOrElse(idx) { names.first() }
    }

    override fun onResume() {
        super.onResume()

        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()

        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            KEY_THEME_CONFIG -> {
                ThemeHelper.applyTheme(
                    sharedPreferences.getString(
                        ThemeHelper.KEY_THEME,
                        ThemeHelper.DEFAULT_MODE,
                    )!!,
                )
            }
        }
    }
}
