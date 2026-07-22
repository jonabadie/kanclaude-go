package com.jonabadie.wallpapercycler.wallpapers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class Starfield : AnimatedWallpaper {
    override val name = "Starfield"

    private data class Star(val x: Float, val y: Float, val depth: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stars: List<Star> = Random(1).let { rng ->
        List(220) { Star(rng.nextFloat(), rng.nextFloat(), 0.15f + rng.nextFloat() * 0.85f) }
    }

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        canvas.drawColor(Color.rgb(4, 6, 20))
        for (star in stars) {
            val y = ((star.y + t * 0.02f * star.depth) % 1f) * height
            val twinkle = 0.6f + 0.4f * sin(t * 3f + star.x * 40f)
            paint.color = Color.WHITE
            paint.alpha = (255 * star.depth * twinkle).toInt().coerceIn(0, 255)
            canvas.drawCircle(star.x * width, y, star.depth * 3f, paint)
        }
    }
}

class OceanWaves : AnimatedWallpaper {
    override val name = "Ocean Waves"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        canvas.drawColor(Color.rgb(8, 20, 40))
        for (layer in 0 until 4) {
            val baseY = height * (0.45f + 0.13f * layer)
            val amp = height * 0.02f * (layer + 1)
            val speed = 0.6f + layer * 0.25f
            path.reset()
            path.moveTo(0f, height.toFloat())
            path.lineTo(0f, baseY)
            var x = 0f
            while (x <= width) {
                val y = baseY + amp * sin(x / width * 4f * PI.toFloat() + t * speed + layer * 1.7f)
                path.lineTo(x, y)
                x += width / 40f
            }
            path.lineTo(width.toFloat(), height.toFloat())
            path.close()
            paint.color = Color.argb(160, 20, 60 + layer * 25, 110 + layer * 30)
            canvas.drawPath(path, paint)
        }
    }
}

class LavaLamp : AnimatedWallpaper {
    override val name = "Lava Lamp"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colors = intArrayOf(
        0xFFFF6D00.toInt(), 0xFFD500F9.toInt(), 0xFF00B8D4.toInt(),
        0xFFFF1744.toInt(), 0xFF76FF03.toInt(),
    )

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        canvas.drawColor(Color.rgb(24, 8, 32))
        for (i in colors.indices) {
            val cx = width * (0.5f + 0.35f * sin(t * 0.21f + i * 2.1f))
            val cy = height * (0.5f + 0.38f * cos(t * 0.17f + i * 1.3f))
            val radius = min(width, height) * (0.22f + 0.06f * sin(t * 0.5f + i))
            val rgb = colors[i] and 0x00FFFFFF
            paint.shader = RadialGradient(
                cx, cy, radius,
                rgb or (0xB0 shl 24), rgb,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
        }
        paint.shader = null
    }
}

class MatrixRain : AnimatedWallpaper {
    override val name = "Matrix Rain"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE }
    private val glyphs = "アイウエオカキクケコサシスセソ0123456789"
    private val speeds: List<Float>
    private val offsets: List<Float>

    init {
        val rng = Random(7)
        speeds = List(COLS) { 4f + rng.nextFloat() * 8f }
        offsets = List(COLS) { rng.nextFloat() * 100f }
    }

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        canvas.drawColor(Color.BLACK)
        val cell = width / COLS.toFloat()
        paint.textSize = cell
        val rows = (height / cell).toInt() + TRAIL
        for (c in 0 until COLS) {
            val head = ((t * speeds[c] + offsets[c]) % rows).toInt()
            for (i in 0 until TRAIL) {
                val r = head - i
                if (r < 0) continue
                val glyph = glyphs[(c * 31 + r * 17 + (t * 2).toInt() * 7).mod(glyphs.length)]
                paint.color = if (i == 0) {
                    Color.rgb(200, 255, 200)
                } else {
                    Color.argb((255 - i * (220 / TRAIL)).coerceAtLeast(0), 0, 220, 70)
                }
                canvas.drawText(glyph.toString(), c * cell, (r + 1) * cell, paint)
            }
        }
    }

    private companion object {
        const val COLS = 20
        const val TRAIL = 16
    }
}

class Fireflies : AnimatedWallpaper {
    override val name = "Fireflies"

    private data class Fly(val ox: Float, val oy: Float, val phase: Float, val speed: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flies: List<Fly> = Random(3).let { rng ->
        List(40) {
            Fly(rng.nextFloat(), rng.nextFloat(), rng.nextFloat() * 2f * PI.toFloat(), 0.5f + rng.nextFloat())
        }
    }

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        canvas.drawColor(Color.rgb(6, 16, 12))
        for (fly in flies) {
            val x = (fly.ox + 0.08f * sin(t * fly.speed * 0.3f + fly.phase) +
                0.04f * sin(t * 0.7f + fly.phase * 3f)) * width
            val y = (fly.oy + 0.08f * cos(t * fly.speed * 0.23f + fly.phase * 2f)) * height
            val glow = (0.5f + 0.5f * sin(t * fly.speed * 2f + fly.phase)).coerceIn(0f, 1f)
            paint.color = Color.argb((60 * glow).toInt(), 180, 255, 120)
            canvas.drawCircle(x, y, 4f + 14f * glow, paint)
            paint.color = Color.argb((255 * glow).toInt(), 220, 255, 160)
            canvas.drawCircle(x, y, 3.5f, paint)
        }
    }
}
