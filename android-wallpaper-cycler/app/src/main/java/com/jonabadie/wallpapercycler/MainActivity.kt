package com.jonabadie.wallpapercycler

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jonabadie.wallpapercycler.wallpapers.WallpaperRegistry

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.todayName).text =
            getString(R.string.todays_wallpaper, WallpaperRegistry.today().name)

        val todayIndex = WallpaperRegistry.todayIndex()
        findViewById<TextView>(R.id.schedule).text = WallpaperRegistry.all
            .mapIndexed { i, wp -> if (i == todayIndex) "▶ ${wp.name}" else "• ${wp.name}" }
            .joinToString("\n")

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
