# WallpaperGPU `.theme` Package Specification (v1.0)

`WallpaperGPU` imports self-contained `.theme` archives. A `.theme` file is a standard ZIP archive (using `STORE` or `DEFLATE`) with the following layout:

```text
<slug>.theme
├── theme.json          # Required: Manifest & schema v1.0
├── index.html          # Required: WebGPU wallpaper document entrypoint
├── shader.wgsl         # Required: Primary WGSL shader module
├── runtime.js          # Required: Frame rate cap, DPR scaler & virtual clock runtime
├── wgpu-bridge.js      # Required: WallpaperGPU API Bridge & Smart Static Snapshot controller
├── bridge.js           # Optional: Scene control adapter
└── assets/             # Optional: Compressed texture assets (.avif, .webp, .png, .ktx2)
    └── *.avif
```

## `theme.json` Schema

```json
{
  "format": "WallpaperGPU.Theme",
  "schemaVersion": "1.0",
  "engine": {
    "name": "WallpaperGPU",
    "repository": "https://github.com/kerneldroid/WallpaperGPU",
    "minVersion": "1.0.0",
    "renderer": "WebGPU",
    "nativeBackend": "Rust wgpu + Kotlin Android WallpaperService"
  },
  "id": "io.github.kerneldroid.wallpapergpu.theme.sample",
  "slug": "sample",
  "name": "Sample Procedural Aurora",
  "category": "Abstract",
  "description": "Procedural WebGPU ribbon reactive to touch and launcher swipe.",
  "author": "WallpaperGPU Community",
  "license": "GPL-3.0-or-later",
  "accent": ["#07243f", "#1b6ca8", "#7bdff2"],
  "entrypoint": "index.html",
  "shader": "shader.wgsl",
  "scripts": ["runtime.js", "wgpu-bridge.js", "bridge.js"],
  "capabilities": {
    "touch": true,
    "multiTouch": true,
    "parallaxOffset": true,
    "deviceTilt": true,
    "batteryLevel": false,
    "smartStaticIdle": true
  },
  "smartStaticPolicy": {
    "freezeOnVisibilityLost": true,
    "freezeOnScreenOff": true,
    "freezeOnProximityCovered": true,
    "inactivityTimeoutSeconds": 30,
    "captureStaticSnapshot": true
  },
  "textures": []
}
```
