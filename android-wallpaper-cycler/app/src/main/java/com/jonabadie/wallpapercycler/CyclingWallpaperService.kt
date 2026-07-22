package com.jonabadie.wallpapercycler

import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.jonabadie.wallpapercycler.wallpapers.WallpaperRegistry

class CyclingWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = CyclingEngine()

    private inner class CyclingEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val drawFrame = Runnable { draw() }
        private var visible = false
        private var startTime = 0L

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            startTime = SystemClock.elapsedRealtime()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) draw() else handler.removeCallbacks(drawFrame)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawFrame)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawFrame)
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val t = (SystemClock.elapsedRealtime() - startTime) / 1000f
                    // Re-resolved every frame, so the scene flips on its own at midnight
                    // and playlist changes apply immediately.
                    WallpaperRegistry.today(applicationContext)
                        .draw(canvas, canvas.width, canvas.height, t)
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
            handler.removeCallbacks(drawFrame)
            if (visible) handler.postDelayed(drawFrame, FRAME_DELAY_MS)
        }
    }

    private companion object {
        const val FRAME_DELAY_MS = 33L // ~30 fps
    }
}
