/*
 * Copyright (C) 2026 kerneldroid (https://github.com/kerneldroid/WallpaperGPU)
 *
 * This file is part of WallpaperGPU.
 *
 * WallpaperGPU is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * WallpaperGPU is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with WallpaperGPU.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.kerneldroid.wallpapergpu.services

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.BatteryManager
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import io.github.kerneldroid.wallpapergpu.Application
import io.github.kerneldroid.wallpapergpu.data.EnginePreferences
import io.github.kerneldroid.wallpapergpu.data.UserPreferencesRepository
import io.github.kerneldroid.wallpapergpu.data.WallpaperThemePackage
import io.github.kerneldroid.wallpapergpu.engine.WgpuNativeBridge
import io.github.kerneldroid.wallpapergpu.engine.WgpuWebBridge
import io.github.kerneldroid.wallpapergpu.util.SmartIdleCoordinator
import io.github.kerneldroid.wallpapergpu.util.TiltSensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "WallpaperGpuService"
private const val SECURE_THEME_DOMAIN = "wallpapergpu.local"

/**
 * Android Live Wallpaper Service (`WallpaperService`) for **WallpaperGPU**.
 *
 * Hosts the active `.theme` WebGPU wallpaper inside a hardware-accelerated surface backed by
 * `VirtualDisplay` + `Presentation` + `WebViewAssetLoader` (`https://wallpapergpu.local/themes/...`,
 * providing a secure origin so `navigator.gpu` WebGPU is enabled) and bridges touch events,
 * launcher home-screen offsets, gyroscope tilt, battery state, and Smart Static Idle Conversion.
 */
class WallpaperGpuService : WallpaperService() {

	override fun onCreateEngine(): Engine = WallpaperGpuEngine()

	inner class WallpaperGpuEngine : Engine() {

		private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
		private var nativeHandle: Long = 0L

		private var virtualDisplay: VirtualDisplay? = null
		private var presentation: Presentation? = null
		private var webView: WebView? = null
		private var loadedThemeSlug: String? = null

		private lateinit var webBridge: WgpuWebBridge
		private lateinit var smartIdleCoordinator: SmartIdleCoordinator
		private lateinit var tiltSensorManager: TiltSensorManager

		private var currentPrefs = EnginePreferences()
		private var surfaceWidth = 1080
		private var surfaceHeight = 2400

		private val batteryReceiver = object : BroadcastReceiver() {
			override fun onReceive(context: Context?, intent: Intent?) {
				if (intent == null) return
				val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 80)
				val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
				val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
				val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
					status == BatteryManager.BATTERY_STATUS_FULL
				val pct = ((level * 100f) / scale).toInt().coerceIn(0, 100)
				if (::webBridge.isInitialized) {
					webBridge.dispatchBattery(pct, charging)
				}
			}
		}

		override fun onCreate(surfaceHolder: SurfaceHolder) {
			super.onCreate(surfaceHolder)
			setTouchEventsEnabled(true)
			setOffsetNotificationsEnabled(true)

			nativeHandle = WgpuNativeBridge.createEngine(30_000L)
			webBridge = WgpuWebBridge(
				webViewProvider = { webView },
				onBridgeReadyCallback = { slug, _ ->
					pushInitialThemeState(slug)
				}
			)

			tiltSensorManager = TiltSensorManager(this@WallpaperGpuService)
			smartIdleCoordinator = SmartIdleCoordinator(this@WallpaperGpuService) { frozen, reason ->
				applySmartStaticFreeze(frozen, reason)
			}

			val app = applicationContext as Application
			val prefsRepo = UserPreferencesRepository(app.userPreferencesDataStore)

			engineScope.launch {
				prefsRepo.preferencesFlow.collectLatest { prefs ->
					currentPrefs = prefs
					smartIdleCoordinator.configure(
						smartFreezeEnabled = prefs.smartStaticFreezeEnabled,
						proximityFreezeEnabled = prefs.proximityPocketFreezeEnabled,
						idleTimeoutSeconds = prefs.idleTimeoutSeconds
					)
					webBridge.dispatchRuntimeConfig(
						fps = prefs.targetFps,
						dprScale = (prefs.renderScalePercent / 100f).coerceIn(0.4f, 1.0f),
						paused = !prefs.engineEnabled
					)
					updateTiltSensorSubscription()
					loadThemeIfNeeded(prefs.activeThemeSlug)
				}
			}

			engineScope.launch {
				app.themeRepository.sceneValues.collectLatest { allSceneValues ->
					val slug = loadedThemeSlug ?: return@collectLatest
					val map = allSceneValues[slug] ?: return@collectLatest
					for ((k, v) in map) {
						webBridge.dispatchSetting(k, v)
					}
				}
			}

			runCatching {
				registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
			}
		}

		override fun onSurfaceChanged(
			holder: SurfaceHolder,
			format: Int,
			width: Int,
			height: Int
		) {
			super.onSurfaceChanged(holder, format, width, height)
			surfaceWidth = width.coerceAtLeast(1)
			surfaceHeight = height.coerceAtLeast(1)
			rebuildVirtualDisplayAndWebView(holder, surfaceWidth, surfaceHeight)
		}

		override fun onVisibilityChanged(visible: Boolean) {
			super.onVisibilityChanged(visible)
			smartIdleCoordinator.onVisibilityChanged(visible)
			updateTiltSensorSubscription()
		}

		override fun onTouchEvent(event: MotionEvent) {
			super.onTouchEvent(event)
			if (!currentPrefs.touchBridgeEnabled) return
			smartIdleCoordinator.onUserInteraction()

			val actionIndex = event.actionIndex
			val pointerId = event.getPointerId(actionIndex)
			val x = event.getX(actionIndex)
			val y = event.getY(actionIndex)
			val pressure = event.getPressure(actionIndex).coerceIn(0.1f, 1.0f)

			val (nativeAction, jsAction) = when (event.actionMasked) {
				MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> 0 to "down"
				MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> 1 to "up"
				MotionEvent.ACTION_MOVE -> 2 to "move"
				else -> 3 to "cancel"
			}

			WgpuNativeBridge.onTouch(
				handle = nativeHandle,
				action = nativeAction,
				x = x,
				y = y,
				pressure = pressure,
				timestampMs = event.eventTime.toFloat()
			)
			webBridge.dispatchTouch(jsAction, x, y, pointerId, pressure)
		}

		override fun onOffsetsChanged(
			xOffset: Float,
			yOffset: Float,
			xOffsetStep: Float,
			yOffsetStep: Float,
			xPixelOffset: Int,
			yPixelOffset: Int
		) {
			super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
			if (!currentPrefs.parallaxBridgeEnabled) return
			smartIdleCoordinator.onUserInteraction()
			WgpuNativeBridge.onOffsetsChanged(nativeHandle, xOffset, yOffset)
			webBridge.dispatchOffset(xOffset, yOffset, xOffsetStep, yOffsetStep)
		}

		override fun onSurfaceDestroyed(holder: SurfaceHolder) {
			teardownVirtualDisplay()
			super.onSurfaceDestroyed(holder)
		}

		override fun onDestroy() {
			runCatching { unregisterReceiver(batteryReceiver) }
			tiltSensorManager.stopListening()
			smartIdleCoordinator.destroy()
			teardownVirtualDisplay()
			WgpuNativeBridge.destroyEngine(nativeHandle)
			nativeHandle = 0L
			engineScope.cancel()
			super.onDestroy()
		}

		@SuppressLint("SetJavaScriptEnabled")
		private fun rebuildVirtualDisplayAndWebView(
			holder: SurfaceHolder,
			width: Int,
			height: Int
		) {
			teardownVirtualDisplay()
			val app = applicationContext as Application
			val assetLoader = WebViewAssetLoader.Builder()
				.setDomain(SECURE_THEME_DOMAIN)
				.addPathHandler(
					"/themes/",
					WebViewAssetLoader.InternalStoragePathHandler(
						this@WallpaperGpuService,
						app.themeRepository.getThemesRootDirectory()
					)
				)
				.build()

			val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
			val densityDpi = resources.displayMetrics.densityDpi

			val vd = displayManager.createVirtualDisplay(
				"WallpaperGPU_Surface",
				width,
				height,
				densityDpi,
				holder.surface,
				DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
			)
			virtualDisplay = vd

			val wv = WebView(this@WallpaperGpuService).apply {
				layoutParams = ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT
				)
				settings.apply {
					javaScriptEnabled = true
					domStorageEnabled = true
					mediaPlaybackRequiresUserGesture = false
					allowFileAccess = false
					allowContentAccess = false
					cacheMode = WebSettings.LOAD_DEFAULT
				}
				addJavascriptInterface(webBridge, "WallpaperGPUHost")
				webViewClient = object : WebViewClient() {
					override fun shouldInterceptRequest(
						view: WebView?,
						request: WebResourceRequest
					): WebResourceResponse? {
						return assetLoader.shouldInterceptRequest(request.url)
					}

					override fun onPageFinished(view: WebView?, url: String?) {
						super.onPageFinished(view, url)
						loadedThemeSlug?.let { pushInitialThemeState(it) }
					}
				}
			}
			webView = wv

			val pres = Presentation(this@WallpaperGpuService, vd.display).apply {
				setContentView(wv)
				show()
			}
			presentation = pres

			loadedThemeSlug = null
			loadThemeIfNeeded(currentPrefs.activeThemeSlug)
		}

		private fun loadThemeIfNeeded(requestedSlug: String) {
			val wv = webView ?: return
			val app = applicationContext as Application
			val themes = app.themeRepository.importedThemes.value
			val target: WallpaperThemePackage = themes.firstOrNull { it.slug == requestedSlug }
				?: themes.firstOrNull()
				?: return

			if (loadedThemeSlug == target.slug) return
			loadedThemeSlug = target.slug

			val url = "https://$SECURE_THEME_DOMAIN/themes/${target.slug}/${target.entrypoint}"
			Log.i(TAG, "Loading WebGPU .theme package '${target.name}' from $url")
			wv.loadUrl(url)
		}

		private fun pushInitialThemeState(slug: String) {
			val app = applicationContext as Application
			val pkg = app.themeRepository.importedThemes.value.firstOrNull { it.slug == slug } ?: return
			val values = app.themeRepository.getSceneValuesForTheme(pkg)
			for ((k, v) in values) {
				webBridge.dispatchSetting(k, v)
			}
			webBridge.dispatchRuntimeConfig(
				fps = currentPrefs.targetFps,
				dprScale = (currentPrefs.renderScalePercent / 100f).coerceIn(0.4f, 1.0f),
				paused = !currentPrefs.engineEnabled
			)
		}

		private fun applySmartStaticFreeze(frozen: Boolean, reason: String) {
			webBridge.dispatchIdleFreeze(frozen, reason)
			val wv = webView ?: return
			if (frozen) {
				// Give wgpu-bridge.js one frame (~32ms) to capture the static snapshot before pausing WebView timers
				wv.postDelayed({
					if (smartIdleCoordinator.status.value.isStaticFrozen) {
						wv.onPause()
					}
				}, 40L)
			} else {
				wv.onResume()
			}
		}

		private fun updateTiltSensorSubscription() {
			val shouldListen = isVisible &&
				currentPrefs.sensorTiltBridgeEnabled &&
				!smartIdleCoordinator.status.value.isStaticFrozen
			if (shouldListen) {
				tiltSensorManager.startListening(onTilt = { gamma, beta, ax, ay, az ->
					webBridge.dispatchTilt(gamma, beta, 0f, ax, ay, az)
				})
			} else {
				tiltSensorManager.stopListening()
			}
		}

		private fun teardownVirtualDisplay() {
			runCatching { presentation?.dismiss() }
			presentation = null
			runCatching {
				webView?.apply {
					stopLoading()
					removeJavascriptInterface("WallpaperGPUHost")
					destroy()
				}
			}
			webView = null
			runCatching { virtualDisplay?.release() }
			virtualDisplay = null
		}
	}
}
