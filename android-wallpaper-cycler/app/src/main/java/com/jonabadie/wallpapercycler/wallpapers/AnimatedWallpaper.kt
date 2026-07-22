package com.jonabadie.wallpapercycler.wallpapers

import android.content.Context
import android.graphics.Canvas
import java.util.Calendar

/**
 * One animated scene. Implementations must be cheap to draw (~30 fps on a phone)
 * and deterministic given [t], so the engine can drive them frame by frame.
 */
interface AnimatedWallpaper {
    val name: String

    /** Draw one frame. [t] is seconds since the wallpaper engine started. */
    fun draw(canvas: Canvas, width: Int, height: Int, t: Float)
}

/** A themed pack of scenes the user can pick as their daily rotation. */
class Playlist(val name: String, val wallpapers: List<AnimatedWallpaper>)

/**
 * The rotation. Which wallpaper is shown depends only on the active playlist and
 * the current local date, so the scene changes automatically at midnight — no
 * alarms or jobs needed.
 */
object WallpaperRegistry {

    val playlists: List<Playlist> = listOf(
        Playlist(
            "Classic",
            listOf(Starfield(), OceanWaves(), LavaLamp(), MatrixRain(), Fireflies()),
        ),
        Playlist(
            "Dragon Ball Vehicles",
            listOf(FlyingNimbus(), CapsuleHovercar(), SaiyanPod(), CloudSkimmer(), DragonRadar()),
        ),
    )

    fun activePlaylist(context: Context): Playlist {
        val name = prefs(context).getString(KEY_PLAYLIST, null)
        return playlists.firstOrNull { it.name == name } ?: playlists.first()
    }

    fun setActivePlaylist(context: Context, playlist: Playlist) {
        prefs(context).edit().putString(KEY_PLAYLIST, playlist.name).apply()
    }

    fun todayIndex(playlist: Playlist): Int {
        val cal = Calendar.getInstance()
        val localEpochDay = (cal.timeInMillis + cal.timeZone.getOffset(cal.timeInMillis)) / MS_PER_DAY
        return (localEpochDay % playlist.wallpapers.size).toInt()
    }

    fun today(context: Context): AnimatedWallpaper {
        val playlist = activePlaylist(context)
        return playlist.wallpapers[todayIndex(playlist)]
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("wallpaper_cycler", Context.MODE_PRIVATE)

    private const val KEY_PLAYLIST = "active_playlist"
    private const val MS_PER_DAY = 24L * 60 * 60 * 1000
}
