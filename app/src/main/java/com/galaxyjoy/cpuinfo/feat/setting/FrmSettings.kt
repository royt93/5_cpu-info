package com.galaxyjoy.cpuinfo.feat.setting

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.galaxyjoy.cpuinfo.BuildConfig
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ext.openBrowserPolicy
import com.galaxyjoy.cpuinfo.util.ThemeHelper
import moreApp
import rateApp
import shareApp

class FrmSettings : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        const val KEY_TEMPERATURE_UNIT = "temperature_unit"
        const val KEY_THEME_CONFIG = "key_theme"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref)

        findPreference<Preference>("key_vip_management")?.setOnPreferenceClickListener {
            activity?.let { com.galaxyjoy.cpuinfo.feat.vip.ActVip.start(it) }
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
