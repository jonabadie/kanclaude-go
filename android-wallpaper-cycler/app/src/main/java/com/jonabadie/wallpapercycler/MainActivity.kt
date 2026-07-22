package com.jonabadie.wallpapercycler

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jonabadie.wallpapercycler.wallpapers.WallpaperRegistry

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val todayView = findViewById<TextView>(R.id.todayName)
        val scheduleView = findViewById<TextView>(R.id.schedule)
        val playlistGroup = findViewById<RadioGroup>(R.id.playlists)

        fun refresh() {
            val playlist = WallpaperRegistry.activePlaylist(this)
            todayView.text = getString(R.string.todays_wallpaper, WallpaperRegistry.today(this).name)
            val todayIndex = WallpaperRegistry.todayIndex(playlist)
            scheduleView.text = playlist.wallpapers
                .mapIndexed { i, wp -> if (i == todayIndex) "▶ ${wp.name}" else "• ${wp.name}" }
                .joinToString("\n")
        }

        WallpaperRegistry.playlists.forEachIndexed { i, playlist ->
            playlistGroup.addView(RadioButton(this).apply {
                id = i + 1
                text = playlist.name
            })
        }
        playlistGroup.check(WallpaperRegistry.playlists.indexOf(WallpaperRegistry.activePlaylist(this)) + 1)
        playlistGroup.setOnCheckedChangeListener { _, checkedId ->
            WallpaperRegistry.setActivePlaylist(this, WallpaperRegistry.playlists[checkedId - 1])
            refresh()
        }
        refresh()

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
}
