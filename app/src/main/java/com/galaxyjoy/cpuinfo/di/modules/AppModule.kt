package com.galaxyjoy.cpuinfo.di.modules

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothManager
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.app.KeyguardManager
import android.hardware.ConsumerIrManager
import android.hardware.SensorManager
import android.hardware.biometrics.BiometricManager
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.UserManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.storage.StorageManager
import android.telephony.TelephonyManager
import android.view.inputmethod.InputMethodManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module which can provide all singletons
 *
 * @author galaxyjoy
 */
@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideResources(@ApplicationContext appContext: Context): Resources =
        appContext.resources

    @Provides
    @Singleton
    fun provideActivityManager(@ApplicationContext appContext: Context): ActivityManager =
        appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    @Provides
    @Singleton
    fun provideDevicePolicyManager(@ApplicationContext appContext: Context): DevicePolicyManager =
        appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    @Provides
    @Singleton
    fun providePackageManager(@ApplicationContext appContext: Context): PackageManager =
        appContext.packageManager

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext appContext: Context): ContentResolver =
        appContext.contentResolver

    @Provides
    @Singleton
    fun provideSensorManager(@ApplicationContext appContext: Context): SensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    @Provides
    @Singleton
    fun provideCameraManager(@ApplicationContext appContext: Context): CameraManager =
        appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    @Provides
    @Singleton
    fun provideUsbManager(@ApplicationContext appContext: Context): UsbManager =
        appContext.getSystemService(Context.USB_SERVICE) as UsbManager

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext appContext: Context): BluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    @Provides
    @Singleton
    fun provideConnectivityManager(@ApplicationContext appContext: Context): ConnectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    @Singleton
    fun provideTelephonyManager(@ApplicationContext appContext: Context): TelephonyManager =
        appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @Provides
    @Singleton
    fun provideDisplayManager(@ApplicationContext appContext: Context): DisplayManager =
        appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    @SuppressLint("WifiManagerPotentialLeak")
    @Provides
    @Singleton
    fun provideWifiManager(@ApplicationContext appContext: Context): WifiManager =
        appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext appContext: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(appContext)

    @Provides
    @Singleton
    fun provideStorageManager(@ApplicationContext appContext: Context): StorageManager =
        appContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = {
                appContext.preferencesDataStoreFile(USER_PREFERENCES_NAME)
            }
        )

    @Provides
    @Singleton
    fun provideIrManager(@ApplicationContext appContext: Context): ConsumerIrManager? =
        appContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager?

    @Provides
    @Singleton
    fun provideAudioManager(@ApplicationContext appContext: Context): AudioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Provides
    @Singleton
    fun provideBatteryManager(@ApplicationContext appContext: Context): BatteryManager =
        appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    /** E05 Haptics: `VIBRATOR_SERVICE` is deprecated (API31+) in favor of `VIBRATOR_MANAGER_SERVICE`
     * -> `.defaultVibrator`, but both resolve to the same [Vibrator] API surface this app reads
     * (`hasAmplitudeControl()`/`areAllPrimitivesSupported()`), so callers don't need to branch. */
    @Suppress("DEPRECATION")
    @Provides
    @Singleton
    fun provideVibrator(@ApplicationContext appContext: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    /** E06/E15 Biometric inventory: `null` pre-API29 (`BIOMETRIC_SERVICE` doesn't exist yet) —
     * callers report "not supported on this Android version" rather than guessing. */
    @Provides
    @Singleton
    fun provideBiometricManager(@ApplicationContext appContext: Context): BiometricManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appContext.getSystemService(Context.BIOMETRIC_SERVICE) as? BiometricManager
        } else {
            null
        }

    @Provides
    @Singleton
    fun provideKeyguardManager(@ApplicationContext appContext: Context): KeyguardManager =
        appContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    @Provides
    @Singleton
    fun provideInputMethodManager(@ApplicationContext appContext: Context): InputMethodManager =
        appContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    @Provides
    @Singleton
    fun provideUserManager(@ApplicationContext appContext: Context): UserManager =
        appContext.getSystemService(Context.USER_SERVICE) as UserManager

    companion object {
        const val USER_PREFERENCES_NAME = "user_preferences"
    }
}
