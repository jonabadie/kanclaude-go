package com.jonabadie.wallpapercycler.wallpapers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.graphics.Paint
import java.io.File
import kotlin.math.max

/**
 * Plays an animated GIF downloaded by the user, center-cropped to fill the screen.
 * Movie is deprecated but is the only GIF decoder that works on the software canvas
 * a WallpaperService engine provides, all the way down to minSdk 24.
 */
@Suppress("DEPRECATION")
class GifWallpaper(private val file: File) : AnimatedWallpaper {

    override val name: String = file.nameWithoutExtension

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var movie: Movie? = null
    private var loadAttempted = false

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        if (!loadAttempted) {
            loadAttempted = true
            movie = runCatching {
                val bytes = file.readBytes()
                Movie.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }

        canvas.drawColor(Color.BLACK)
        val m = movie
        if (m == null || m.width() <= 0 || m.height() <= 0) {
            paint.color = Color.WHITE
            paint.textSize = height / 40f
            canvas.drawText("Could not play $name", width * 0.1f, height / 2f, paint)
            return
        }

        val duration = if (m.duration() > 0) m.duration() else 1000
        m.setTime((t * 1000).toInt() % duration)

        val scale = max(width / m.width().toFloat(), height / m.height().toFloat())
        canvas.save()
        canvas.translate((width - m.width() * scale) / 2f, (height - m.height() * scale) / 2f)
        canvas.scale(scale, scale)
        m.draw(canvas, 0f, 0f)
        canvas.restore()
    }
}
