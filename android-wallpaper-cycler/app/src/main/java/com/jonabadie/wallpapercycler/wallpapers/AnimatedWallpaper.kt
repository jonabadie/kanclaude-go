package com.jonabadie.wallpapercycler.wallpapers

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

/**
 * The rotation. Which wallpaper is shown depends only on the current local date,
 * so the scene changes automatically at midnight — no alarms or jobs needed.
 */
object WallpaperRegistry {

    val all: List<AnimatedWallpaper> = listOf(
        Starfield(),
        OceanWaves(),
        LavaLamp(),
        MatrixRain(),
        Fireflies(),
    )

    fun todayIndex(): Int {
        val cal = Calendar.getInstance()
        val localEpochDay = (cal.timeInMillis + cal.timeZone.getOffset(cal.timeInMillis)) / MS_PER_DAY
        return (localEpochDay % all.size).toInt()
    }

    fun today(): AnimatedWallpaper = all[todayIndex()]

    private const val MS_PER_DAY = 24L * 60 * 60 * 1000
}
