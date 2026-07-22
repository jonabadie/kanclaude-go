package com.jonabadie.wallpapercycler

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-updating for sideloaded builds: CI publishes version.json next to the APK
 * on the GitHub release; we compare it to our own versionCode, download the new
 * APK, and hand it to the system installer (Android always asks the user to
 * confirm the install — silent updates are not possible for sideloaded apps).
 */
object UpdateManager {

    private const val RELEASE_BASE =
        "https://github.com/jonabadie/kanclaude-go/releases/download/wallpaper-apk"
    private const val VERSION_URL = "$RELEASE_BASE/version.json"
    private const val APK_URL = "$RELEASE_BASE/app-debug.apk"

    class Update(val versionCode: Int, val versionName: String)

    /** Returns the available update, or null if we're current. Call off the main thread. */
    fun checkForUpdate(): Update? {
        val json = JSONObject(fetch(VERSION_URL).use { it.readBytes().decodeToString() })
        val code = json.getInt("versionCode")
        if (code <= BuildConfig.VERSION_CODE) return null
        return Update(code, json.optString("versionName", code.toString()))
    }

    /** Downloads the new APK and opens the system install screen. Call off the main thread. */
    fun downloadAndInstall(context: Context) {
        val apk = File(context.cacheDir, "update.apk")
        fetch(APK_URL).use { input -> apk.outputStream().use { input.copyTo(it) } }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun fetch(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 60_000
        instanceFollowRedirects = true
    }.inputStream
}
