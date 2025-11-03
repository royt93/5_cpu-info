package com.galaxyjoy.cpuinfo.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class ApkExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun extractApk(packageName: String): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Get package info
                val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
                val appInfo = packageInfo.applicationInfo ?: return@withContext Result.Error("App info not found")
                val appName = context.packageManager.getApplicationLabel(appInfo).toString()
                val versionName = packageInfo.versionName ?: "unknown"

                // Source APK path
                val sourceApk = File(appInfo.sourceDir ?: return@withContext Result.Error("APK source not found"))
                if (!sourceApk.exists()) {
                    return@withContext Result.Error("APK file not found")
                }

                // Destination folder
                val destFolder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ use Download directory
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "CPUInfo_APKs"
                    )
                } else {
                    File(
                        Environment.getExternalStorageDirectory(),
                        "Download/CPUInfo_APKs"
                    )
                }

                if (!destFolder.exists()) {
                    destFolder.mkdirs()
                }

                // Clean app name for filename
                val cleanAppName = appName.replace(Regex("[^a-zA-Z0-9]"), "_")
                val cleanPackageName = packageName.replace(Regex("[^a-zA-Z0-9.]"), "_")

                // Destination file: AppName_v1.0_com.package.name.apk
                val destFile = File(destFolder, "${cleanAppName}_v${versionName}_${cleanPackageName}.apk")

                // Copy APK file
                FileInputStream(sourceApk).use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L
                        val totalSize = sourceApk.length()

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                            // Progress: (totalBytes * 100 / totalSize)%
                        }
                        output.flush()
                    }
                }

                Timber.i("APK extracted successfully: ${destFile.absolutePath}")
                Result.Success(destFile, appName)

            } catch (e: Exception) {
                Timber.e(e, "Failed to extract APK")
                Result.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun shareApk(file: File) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                android.net.Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share APK"))
        } catch (e: Exception) {
            Timber.e(e, "Failed to share APK")
        }
    }

    sealed class Result {
        data class Success(val file: File, val appName: String) : Result()
        data class Error(val message: String) : Result()
    }
}
