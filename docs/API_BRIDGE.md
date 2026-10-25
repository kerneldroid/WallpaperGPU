# WallpaperGPU API Bridge Reference (`window.WallpaperGPU`)

Every `.theme` wallpaper executed by `WallpaperGPU` has access to `window.WallpaperGPU` (injected via `wgpu-bridge.js` and backed by `WallpaperGpuService` + Rust `wallpapergpu-core` JNI).

## State Object (`window.WallpaperGPU.state`)

```ts
interface WallpaperGPUState {
  slug: string;
  engineVersion: string;
  visible: boolean;
  idleFrozen: boolean;
  idleReason: string | null;
  offsetX: number; // 0.0 .. 1.0 (Android launcher home screen horizontal page offset)
  offsetY: number; // 0.0 .. 1.0
  offsetStepX: number;
  offsetStepY: number;
  pointer: {
    x: number;
    y: number;
    normX: number;
    normY: number;
    vx: number;
    vy: number;
    down: boolean;
    pressure: number;
    pointerId: number;
  };
  tilt: {
    gamma: number; // Left-to-right tilt in degrees (-90..90)
    beta: number;  // Front-to-back tilt in degrees (-180..180)
    alpha: number; // Compass direction (0..360)
    ax: number;
    ay: number;
    az: number;
  };
  battery: {
    level: number; // 0 .. 100
    charging: boolean;
  };
  settings: Record<string, string | number | boolean>;
}
```

## Event Subscriptions

- `window.WallpaperGPU.onTouch((payload, state) => { ... })`
- `window.WallpaperGPU.onTap((payload, state) => { ... })`
- `window.WallpaperGPU.onParallaxOffset((payload, state) => { ... })`
- `window.WallpaperGPU.onTilt((payload, state) => { ... })`
- `window.WallpaperGPU.onBattery((payload, state) => { ... })`
- `window.WallpaperGPU.onVisibilityChange((payload, state) => { ... })`
- `window.WallpaperGPU.onIdleFreeze((payload, state) => { ... })`
- `window.WallpaperGPU.onSettingChange((payload, state) => { ... })`

## Smart Static Conversion (`captureStaticFrame` / `setStaticFreeze`)

When nobody is looking at the wallpaper (`onVisibilityChanged(false)`, screen off, proximity sensor covered, or touch inactivity timeout), `WallpaperGPU` calls `window.WallpaperGPU._dispatchIdle(true, reason)`:
1. Captures the current WebGPU `<canvas>` buffer onto `#wgpu-static-snapshot`.
2. Halts the `requestAnimationFrame` loop and WebGPU command queue (`0% GPU usage`).
3. Notifies the native host (`WallpaperGPUHost.onStaticFrameState(slug, true, reason)`).
4. Automatically releases the snapshot and resumes live rendering on visibility or touch wake.
