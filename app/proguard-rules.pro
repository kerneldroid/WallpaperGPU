# Keep WallpaperGPU JNI Bridge and WebView @JavascriptInterface methods
-keep class io.github.kerneldroid.wallpapergpu.engine.WgpuNativeBridge {
    native <methods>;
}

-keepclassmembers class io.github.kerneldroid.wallpapergpu.engine.WgpuWebBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
