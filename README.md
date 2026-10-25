# WallpaperGPU

**WallpaperGPU** (`kerneldroid/WallpaperGPU`) is a smart, battery-conscious **WebGPU Live Wallpaper Engine** for Android written in **Kotlin** (Jetpack Compose + WebView) with an optional **Rust** native core.

It imports self-contained `.theme` live wallpaper archives, displays them in a **Material Design 3 Carousel**, provides a bidirectional **WallpaperGPU API Bridge** for screen touches, home-screen launcher swipe parallax, device tilt, and battery state, and automatically **converts live wallpapers into zero-power static frames** whenever nobody is looking at the wallpaper.

> **Clean Repository Policy**: `WallpaperGPU` ships strictly as an open-source engine and template runtime. No third-party or proprietary wallpaper assets are bundled in this repository. Users can build their own `.theme` packages or download compatible `.theme` files from external template studios.

## Key Features

- **`.theme` Package Importer & Material Design 3 Carousel**:
  - Import any `.theme` archive conforming to the WallpaperGPU Template Specification v1.0.
  - Browse imported wallpapers in a native Material 3 `HorizontalMultiBrowseCarousel`.
  - Tap any carousel card to open the **Apply as Wallpaper** bottom sheet, customize scene parameters (`select`, `segmented`, `slider`, `switch`), and launch the Android Live Wallpaper picker (`WallpaperGpuService`).
- **WallpaperGPU API Bridge (`window.WallpaperGPU`)**:
  - **Touch & Multi-Touch**: Forwards Android `WallpaperService.Engine.onTouchEvent` directly to the WebGPU scene via `window.WallpaperGPU.onTouch` / `onTap` and synthesized DOM `PointerEvent`, `TouchEvent`, and `MouseEvent` streams.
  - **Launcher Swipe Parallax**: Forwards `onOffsetsChanged` home-screen page swipe offsets (`offsetX`, `offsetY`) for smooth parallax camera tracking.
  - **Device Tilt & Battery**: Streams gyroscope/rotation-vector tilt (`gamma`, `beta`, `alpha`) and battery level/charging state.
  - The bridge is theme-supplied (`wgpu-bridge.js`); the host guards every call so a theme may implement only part of the API.
- **Smart Static Wallpaper Conversion (0% Idle GPU Draw)**:
  - Automatically freezes the live WebGPU canvas into a lightweight static snapshot and halts the render loop when nobody is looking at the wallpaper:
    1. Another application covers the home screen (`onVisibilityChanged(false)`).
    2. The device screen turns off or enters Doze (`ACTION_SCREEN_OFF`).
    3. The proximity sensor is covered (device in pocket or face-down on a desk).
    4. Configurable touch inactivity timeout expires.
  - Seamlessly wakes and resumes 60–120 Hz WebGPU rendering the instant the user views or touches the home screen.
- **Adaptive Theme UI Foundation**:
  - Built on the clean Material 3 Expressive layout and preference card architecture from **[Adaptive Theme](https://github.com/xLexip/Adaptive-Theme)** by **[xLexip](https://lexip.dev)**.

## Architecture

```text
WallpaperGPU/
├── app/                                  # Kotlin Android App & Live Wallpaper Service
│   └── src/main/kotlin/io/github/kerneldroid/wallpapergpu/
│       ├── data/                         # ThemeArchiveModel, ThemeRepository, UserPreferencesRepository
│       ├── engine/                       # WgpuNativeBridge (JNI), WgpuWebBridge (@JavascriptInterface)
│       ├── services/                     # WallpaperGpuService (Android Live Wallpaper Engine)
│       ├── ui/                           # Adaptive-Theme M3 UI + MD3 Carousel + ApplyWallpaperSheet
│       └── util/                         # SmartIdleCoordinator, ProximitySensorManager, LightSensorManager, TiltSensorManager
├── rust/
│   └── wallpapergpu-core/                # Rust native library (wgpu uniform bridge, .theme verifier, idle FSM, JNI)
└── docs/
    ├── THEME_SPEC.md                     # Standardized .theme archive & theme.json specification
    └── API_BRIDGE.md                     # window.WallpaperGPU & Native JNI Bridge reference
```

## Building

### Prerequisites

- JDK 21 (the Gradle daemon JVM is pinned in `app/gradle/gradle-daemon-jvm.properties`)
- Android SDK with platform 36 and build-tools 36.0.0
- `ANDROID_HOME` set, or `app/local.properties` containing `sdk.dir=/path/to/Android/Sdk`

### 1. Rust Native Core (optional, `libwallpapergpu_core.so`)

```bash
cd rust/wallpapergpu-core
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o ../../app/src/main/jniLibs build --release
```

`WgpuNativeBridge.kt` falls back to an equivalent Kotlin implementation when the native library is not present, so the app builds and runs without NDK/cargo-ndk.

### 2. Android Application

```bash
cd app
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew assembleRelease
```

The signed release APK is published on the [Releases](https://github.com/kerneldroid/WallpaperGPU/releases) page. The release keystore is intentionally not in this repository.

### Runtime requirement

Themes are rendered by the Android system WebView through WebGPU. WebGPU must be available in the WebView/Chromium build and the device GPU must not be blocklisted. Emulators with a software GPU (and Android WebView builds without WebGPU) can run the built-in Canvas2D demo but will show a blank surface for WebGPU themes. Use a physical Android 12+ device with a Vulkan-capable GPU for real WebGPU validation.

## Templates

The companion template studio is [kerneldroid/lw-webgpu-wallpapers](https://github.com/kerneldroid/lw-webgpu-wallpapers). Download a `<slug>.theme` file from the gallery and import it with **Import .theme**, or open the file directly from a file manager.

## Credits

- **UI Architecture & Preference Components**: Adapted from **[Adaptive Theme](https://github.com/xLexip/Adaptive-Theme)** by **[xLexip](https://lexip.dev)** (see [CREDITS.md](CREDITS.md) and [NOTICE](NOTICE)).
- **Typography**: [Nunito](https://github.com/googlefonts/nunito) (SIL Open Font License 1.1).

## License

WallpaperGPU is licensed under the **GNU General Public License v3.0 or later** (`GPL-3.0-or-later`).
See [LICENSE](LICENSE) for the full text. The UI architecture, preference cards, typography setup,
and sensor wrappers adapted from [Adaptive Theme](https://github.com/xLexip/Adaptive-Theme) are also
GPL-3.0-or-later; their copyright notices are retained in the affected source files and in
[CREDITS.md](CREDITS.md).
