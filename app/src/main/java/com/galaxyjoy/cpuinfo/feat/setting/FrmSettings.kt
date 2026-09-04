package com.galaxyjoy.cpuinfo.feat.setting

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.galaxyjoy.cpuinfo.feat.benchhistory.BenchHistoryExporter
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
import moreApp
import rateApp
import shareApp

class FrmSettings : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        const val KEY_TEMPERATURE_UNIT = "temperature_unit"
        const val KEY_THEME_CONFIG = "key_theme"
        const val KEY_HEALTH_ALERT = "key_health_alert"
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
        wireExportBenchHistoryPref()

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
