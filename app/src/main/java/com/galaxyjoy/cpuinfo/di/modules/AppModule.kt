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
import android.hardware.ConsumerIrManager
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import android.os.storage.StorageManager
import android.view.WindowManager
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
    fun provideWindowManager(@ApplicationContext appContext: Context): WindowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

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

    companion object {
        const val USER_PREFERENCES_NAME = "user_preferences"
    }
}
