package com.jonabadie.wallpapercycler

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jonabadie.wallpapercycler.wallpapers.WallpaperRegistry
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var todayView: TextView
    private lateinit var scheduleView: TextView
    private lateinit var playlistGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        todayView = findViewById(R.id.todayName)
        scheduleView = findViewById(R.id.schedule)
        playlistGroup = findViewById(R.id.playlists)
        rebuildPlaylists()

        val urlInput = findViewById<EditText>(R.id.urlInput)
        findViewById<Button>(R.id.downloadButton).setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                toast(getString(R.string.download_started))
                downloadGif(url)
            }
        }

        findViewById<Button>(R.id.setWallpaper).setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, CyclingWallpaperService::class.java),
            )
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Some OEM launchers don't handle the direct-preview intent.
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    private fun rebuildPlaylists() {
        val playlists = WallpaperRegistry.playlists(this)
        playlistGroup.setOnCheckedChangeListener(null)
        playlistGroup.removeAllViews()
        playlists.forEachIndexed { i, playlist ->
            playlistGroup.addView(RadioButton(this).apply {
                id = i + 1
                text = playlist.name
            })
        }
        playlistGroup.check(playlists.indexOf(WallpaperRegistry.activePlaylist(this)) + 1)
        playlistGroup.setOnCheckedChangeListener { _, checkedId ->
            WallpaperRegistry.setActivePlaylist(this, playlists[checkedId - 1])
            refresh()
        }
        refresh()
    }

    private fun refresh() {
        val playlist = WallpaperRegistry.activePlaylist(this)
        todayView.text = getString(R.string.todays_wallpaper, WallpaperRegistry.today(this).name)
        val todayIndex = WallpaperRegistry.todayIndex(playlist)
        scheduleView.text = playlist.wallpapers
            .mapIndexed { i, wp -> if (i == todayIndex) "▶ ${wp.name}" else "• ${wp.name}" }
            .joinToString("\n")
    }

    private fun downloadGif(url: String) {
        executor.execute {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000
                conn.instanceFollowRedirects = true
                val bytes = conn.inputStream.use { it.readBytes() }
                require(bytes.size > 6 && bytes.decodeToString(0, 3) == "GIF") {
                    getString(R.string.not_a_gif)
                }
                val name = url.substringAfterLast('/').substringBefore('?')
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .ifBlank { "wallpaper" }
                    .let { if (it.endsWith(".gif", ignoreCase = true)) it else "$it.gif" }
                File(WallpaperRegistry.downloadsDir(this), name).writeBytes(bytes)
                WallpaperRegistry.invalidate()
                runOnUiThread {
                    toast(getString(R.string.download_done, name))
                    rebuildPlaylists()
                }
            } catch (e: Exception) {
                runOnUiThread { toast(getString(R.string.download_failed, e.message ?: e.javaClass.simpleName)) }
            }
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
