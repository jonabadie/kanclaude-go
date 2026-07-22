package com.jonabadie.wallpapercycler.wallpapers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Original procedurally-drawn scenes inspired by the iconic Dragon Ball vehicles —
 * no copyrighted artwork, everything is Canvas math.
 */

class FlyingNimbus : AnimatedWallpaper {
    override val name = "Flying Nimbus"

    private data class Cloud(val y: Float, val scale: Float, val speed: Float, val offset: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clouds: List<Cloud> = Random(11).let { rng ->
        List(8) {
            Cloud(0.08f + rng.nextFloat() * 0.7f, 0.5f + rng.nextFloat(), 0.03f + rng.nextFloat() * 0.06f, rng.nextFloat())
        }
    }

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.rgb(90, 170, 255), Color.rgb(195, 230, 255),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        paint.color = Color.argb(235, 255, 255, 255)
        for (c in clouds) {
            val x = ((c.offset + 1f - (t * c.speed) % 1f) % 1f) * (width + 300f) - 150f
            drawPuffCloud(canvas, x, c.y * height, 55f * c.scale)
        }

        val nx = width * 0.5f + width * 0.06f * sin(t * 0.7f)
        val ny = height * 0.55f + height * 0.015f * sin(t * 2.3f)

        // speed lines trailing the nimbus
        paint.strokeWidth = 6f
        paint.color = Color.argb(120, 255, 255, 255)
        for (i in 0 until 3) {
            val phase = (t * 1.5f + i * 0.33f) % 1f
            val lx = nx + 90f + phase * 220f
            canvas.drawLine(lx, ny - 24f + i * 24f, lx + 90f * (1f - phase), ny - 24f + i * 24f, paint)
        }

        paint.color = Color.rgb(255, 195, 55)
        drawPuffCloud(canvas, nx, ny, 55f)
        paint.color = Color.rgb(255, 225, 120)
        drawPuffCloud(canvas, nx, ny - 14f, 40f)
    }

    private fun drawPuffCloud(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx - r, cy, r * 0.65f, paint)
        canvas.drawCircle(cx + r, cy, r * 0.65f, paint)
        canvas.drawCircle(cx - r * 0.4f, cy - r * 0.45f, r * 0.75f, paint)
        canvas.drawCircle(cx + r * 0.45f, cy - r * 0.35f, r * 0.7f, paint)
        canvas.drawCircle(cx, cy, r, paint)
    }
}

class CapsuleHovercar : AnimatedWallpaper {
    override val name = "Capsule Hovercar"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        val horizon = height * 0.7f

        // dusk sky
        paint.shader = LinearGradient(
            0f, 0f, 0f, horizon,
            Color.rgb(90, 50, 115), Color.rgb(255, 160, 80),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), horizon, paint)
        paint.shader = null

        paint.color = Color.rgb(255, 220, 130)
        canvas.drawCircle(width * 0.75f, horizon - height * 0.06f, height * 0.05f, paint)

        // two mountain layers scrolling at different speeds
        for (layer in 0 until 2) {
            paint.color = if (layer == 0) Color.rgb(105, 60, 110) else Color.rgb(75, 42, 85)
            val seg = width / 3f
            val speed = 25f + layer * 45f
            val shift = (t * speed) % seg
            path.reset()
            path.moveTo(-seg, horizon)
            var i = 0
            var x = -seg - shift
            while (x < width + seg) {
                val peak = horizon - height * (0.06f + 0.05f * ((sin(i * 2.7f + layer * 5f) + 1f) / 2f)) * (layer + 1)
                path.lineTo(x + seg / 2f, peak)
                path.lineTo(x + seg, horizon)
                x += seg
                i++
            }
            path.close()
            canvas.drawPath(path, paint)
        }

        // ground and scrolling road dashes
        paint.color = Color.rgb(58, 34, 60)
        canvas.drawRect(0f, horizon, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.argb(160, 255, 220, 150)
        val dashW = width / 8f
        val roadY = height * 0.78f
        val dashShift = (t * 220f) % (dashW * 2f)
        var dx = -dashShift
        while (dx < width) {
            canvas.drawRect(dx, roadY, dx + dashW, roadY + 8f, paint)
            dx += dashW * 2f
        }

        // the hovercar
        val cx = width * 0.4f
        val cy = height * 0.72f + 7f * sin(t * 3f)

        paint.color = Color.argb(70, 0, 0, 0)
        rect.set(cx - 75f, height * 0.77f, cx + 75f, height * 0.77f + 16f)
        canvas.drawOval(rect, paint)

        // thruster glow
        paint.color = Color.argb(170, 130, 210, 255)
        canvas.drawCircle(cx - 92f, cy - 10f, 10f + 3f * sin(t * 20f), paint)

        // body
        paint.color = Color.rgb(230, 60, 50)
        rect.set(cx - 85f, cy - 30f, cx + 85f, cy + 12f)
        canvas.drawRoundRect(rect, 26f, 26f, paint)

        // canopy
        paint.color = Color.rgb(175, 225, 255)
        rect.set(cx - 42f, cy - 62f, cx + 42f, cy - 4f)
        canvas.drawArc(rect, 180f, 180f, true, paint)
    }
}

class SaiyanPod : AnimatedWallpaper {
    override val name = "Saiyan Space Pod"

    private data class Star(val x: Float, val y: Float, val depth: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val stars: List<Star> = Random(5).let { rng ->
        List(90) { Star(rng.nextFloat(), rng.nextFloat(), 0.2f + rng.nextFloat() * 0.8f) }
    }

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        canvas.drawColor(Color.rgb(8, 8, 26))

        // streaking stars flying past
        paint.strokeWidth = 3f
        for (star in stars) {
            val x = ((star.x - t * 0.25f * star.depth) % 1f + 1f) % 1f * width
            val y = star.y * height
            paint.color = Color.argb((200 * star.depth).toInt(), 255, 255, 255)
            canvas.drawLine(x, y, x + 30f * star.depth, y, paint)
        }

        val cx = width * 0.58f + width * 0.04f * sin(t * 0.6f)
        val cy = height * 0.45f + height * 0.03f * cos(t * 0.8f)
        val r = min(width, height) * 0.14f

        // flame trail
        for (i in 5 downTo 0) {
            val fx = cx - r - 20f - i * r * 0.42f
            val flicker = 1f + 0.15f * sin(t * 25f + i * 2f)
            val fr = (r * 0.55f - i * r * 0.07f) * flicker
            paint.color = Color.argb(
                (200 - i * 30).coerceAtLeast(40),
                255, (170 - i * 22).coerceAtLeast(60), 40,
            )
            canvas.drawCircle(fx, cy + 4f * sin(t * 18f + i), fr, paint)
        }

        // pod body with shading
        paint.shader = RadialGradient(
            cx - r * 0.35f, cy - r * 0.35f, r * 1.9f,
            Color.rgb(245, 245, 250), Color.rgb(120, 125, 145),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        // round window
        paint.color = Color.rgb(190, 40, 45)
        canvas.drawCircle(cx + r * 0.38f, cy - r * 0.1f, r * 0.42f, paint)
        paint.color = Color.rgb(120, 20, 25)
        paint.strokeWidth = r * 0.08f
        canvas.drawLine(cx + r * 0.38f, cy - r * 0.52f, cx + r * 0.38f, cy + r * 0.32f, paint)
        canvas.drawLine(cx - r * 0.04f, cy - r * 0.1f, cx + r * 0.8f, cy - r * 0.1f, paint)

        // hatch rim
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.06f
        paint.color = Color.rgb(90, 95, 115)
        canvas.drawCircle(cx + r * 0.38f, cy - r * 0.1f, r * 0.52f, paint)
        paint.style = Paint.Style.FILL
    }
}

class CloudSkimmer : AnimatedWallpaper {
    override val name = "Cloud Skimmer"

    private data class Glint(val x: Float, val y: Float, val phase: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val glints: List<Glint> = Random(9).let { rng ->
        List(30) { Glint(rng.nextFloat(), rng.nextFloat(), rng.nextFloat() * 6.3f) }
    }

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        val horizon = height * 0.55f

        paint.shader = LinearGradient(
            0f, 0f, 0f, horizon,
            Color.rgb(120, 190, 255), Color.rgb(210, 240, 255),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), horizon, paint)
        paint.shader = LinearGradient(
            0f, horizon, 0f, height.toFloat(),
            Color.rgb(30, 110, 190), Color.rgb(10, 55, 120),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // sun glints on the water
        for (g in glints) {
            val alpha = ((sin(t * 2f + g.phase) + 1f) / 2f * 180f).toInt()
            paint.color = Color.argb(alpha, 255, 255, 230)
            val gy = horizon + g.y * (height - horizon) * 0.9f + 10f
            canvas.drawRect(g.x * width, gy, g.x * width + 24f, gy + 4f, paint)
        }

        // the plane, banking gently
        val cx = width * 0.45f + width * 0.05f * sin(t * 0.5f)
        val cy = height * 0.34f + height * 0.02f * sin(t * 1.6f)

        paint.color = Color.argb(60, 0, 0, 0)
        rect.set(cx - 60f, horizon + 30f, cx + 60f, horizon + 44f)
        canvas.drawOval(rect, paint)

        // wing behind body
        paint.color = Color.rgb(215, 165, 30)
        rect.set(cx - 30f, cy - 55f, cx + 20f, cy)
        canvas.drawOval(rect, paint)

        // fuselage
        paint.color = Color.rgb(250, 200, 40)
        rect.set(cx - 95f, cy - 24f, cx + 95f, cy + 24f)
        canvas.drawOval(rect, paint)

        // cockpit and tail
        paint.color = Color.rgb(175, 225, 255)
        rect.set(cx + 30f, cy - 20f, cx + 78f, cy + 6f)
        canvas.drawOval(rect, paint)
        paint.color = Color.rgb(215, 165, 30)
        rect.set(cx - 95f, cy - 48f, cx - 60f, cy)
        canvas.drawOval(rect, paint)

        // propeller blur
        paint.color = Color.argb(110, 220, 220, 220)
        rect.set(cx + 92f, cy - 34f + 6f * sin(t * 40f), cx + 100f, cy + 34f - 6f * sin(t * 40f))
        canvas.drawOval(rect, paint)
    }
}

class DragonRadar : AnimatedWallpaper {
    override val name = "Dragon Radar"

    private data class Blip(val x: Float, val y: Float, val phase: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val blips: List<Blip> = Random(77).let { rng ->
        List(7) {
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val dist = 0.25f + rng.nextFloat() * 0.65f
            Blip(cos(angle) * dist, sin(angle) * dist, rng.nextFloat() * 6.3f)
        }
    }

    override fun draw(canvas: Canvas, width: Int, height: Int, t: Float) {
        canvas.drawColor(Color.rgb(6, 22, 10))

        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) * 0.42f

        // screen
        paint.shader = RadialGradient(
            cx, cy, r,
            Color.rgb(90, 215, 110), Color.rgb(30, 120, 55),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        // grid
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.argb(120, 15, 70, 30)
        for (i in 1..3) canvas.drawCircle(cx, cy, r * i / 3f, paint)
        canvas.drawLine(cx - r, cy, cx + r, cy, paint)
        canvas.drawLine(cx, cy - r, cx, cy + r, paint)
        paint.style = Paint.Style.FILL

        // rotating sweep with fading trail
        val sweep = (t * 80f) % 360f
        rect.set(cx - r, cy - r, cx + r, cy + r)
        for (i in 0 until 24) {
            paint.color = Color.argb(90 - i * 4, 200, 255, 210)
            canvas.drawArc(rect, sweep - i * 3f, 3.5f, true, paint)
        }

        // blinking dragon ball blips
        for (b in blips) {
            val pulse = (sin(t * 4f + b.phase) + 1f) / 2f
            paint.color = Color.argb((120 + 135 * pulse).toInt(), 255, 200, 40)
            canvas.drawCircle(cx + b.x * r, cy + b.y * r, 9f + 4f * pulse, paint)
        }

        // bezel
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.06f
        paint.color = Color.rgb(200, 205, 215)
        canvas.drawCircle(cx, cy, r + r * 0.03f, paint)
        paint.style = Paint.Style.FILL
    }
}
