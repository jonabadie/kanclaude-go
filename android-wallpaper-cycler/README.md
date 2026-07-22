# Wallpaper Cycler

An Android **live wallpaper** that shows a different animated scene every day, organized into
themed **playlists** you pick in the app (stored in `SharedPreferences`):

- **Classic** — Starfield, Ocean Waves, Lava Lamp, Matrix Rain, Fireflies
- **Dragon Ball Vehicles** — original scenes inspired by the iconic rides: Flying Nimbus,
  Capsule Hovercar, Saiyan Space Pod, Cloud Skimmer, Dragon Radar

Everything is procedurally drawn with Canvas — no image assets, no copyrighted artwork.

## How it works

Animated wallpapers on Android are *live wallpapers*: a `WallpaperService` whose `Engine` draws
directly onto the home-screen surface.

- **`CyclingWallpaperService`** — the live wallpaper service. Its engine runs a ~30 fps draw loop
  with a `Handler`: lock the surface canvas, draw a frame, post the next frame. It pauses when the
  wallpaper isn't visible (screen off, app in front), so it costs nothing most of the time.
- **`AnimatedWallpaper`** — the scene interface: `draw(canvas, width, height, t)` where `t` is
  seconds elapsed. Each scene is a small piece of procedural Canvas drawing.
- **`WallpaperRegistry`** — playlists and daily cycling. Today's scene is simply
  `activePlaylist[localEpochDay % playlist.size]`, re-resolved on every frame. When the local
  date flips at midnight, the modulo changes and the next scene appears automatically — no
  `AlarmManager`, `WorkManager`, or background jobs needed. Switching playlists in the app
  takes effect immediately.
- **`MainActivity`** — a tiny launcher screen that shows the rotation and opens the system's live
  wallpaper preview so the user can apply it.

The manifest declares the service with the `android.permission.BIND_WALLPAPER` permission and the
`android.service.wallpaper.WallpaperService` intent filter; `res/xml/cycling_wallpaper.xml`
provides the metadata shown in the system wallpaper picker.

## Build & install

Open the project in Android Studio and run it on a device, or from the command line
(requires the Android SDK; run `gradle wrapper` once if you need `./gradlew`):

```sh
gradle assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Then open **Wallpaper Cycler** on the device and tap *Set as live wallpaper*.

- minSdk 24 (Android 7.0), targetSdk 35, Kotlin 2.0, AGP 8.5.

## Adding your own wallpapers

Implement `AnimatedWallpaper` in `wallpapers/` and add it to a playlist in
`WallpaperRegistry.playlists` (or add a whole new playlist) — it joins the rotation
immediately. Keep `draw()` cheap (allocate paints/paths once, not per frame).

## Variations

- **Video wallpapers** — instead of Canvas drawing, attach a `MediaPlayer`/ExoPlayer to the
  engine's `SurfaceHolder` and loop an MP4; swap the file per day with the same modulo trick.
- **GPU scenes** — use `GLSurfaceView`-style rendering in the engine (e.g. via
  [`GLWallpaperService`] patterns) for shader-based wallpapers.
- **Static daily wallpapers** — if animation isn't required, skip the service entirely: schedule a
  daily `WorkManager` job that calls `WallpaperManager.setBitmap()`.
