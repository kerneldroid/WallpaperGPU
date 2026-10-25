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

package io.github.kerneldroid.wallpapergpu.engine

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

private const val TAG = "WgpuWebBridge"

/**
 * Bidirectional WallpaperGPU API Bridge connecting Android's `WallpaperGpuService`
 * with `window.WallpaperGPU` (`wgpu-bridge.js`) inside the active `.theme` WebGPU document.
 */
class WgpuWebBridge(
	private val webViewProvider: () -> WebView?,
	private val onBridgeReadyCallback: (slug: String, version: String) -> Unit = { _, _ -> },
	private val onStaticFrameCallback: (slug: String, frozen: Boolean, reason: String) -> Unit = { _, _, _ -> },
	private val onGpuStatusCallback: (supported: Boolean, error: String) -> Unit = { _, _ -> }
) {
	private val mainHandler = Handler(Looper.getMainLooper())

	@JavascriptInterface
	fun onBridgeReady(slug: String, version: String) {
		Log.i(TAG, "WallpaperGPU JS Bridge ready for theme '$slug' (v$version)")
		mainHandler.post { onBridgeReadyCallback(slug, version) }
	}

	@JavascriptInterface
	fun onStaticFrameState(slug: String, frozen: Boolean, reason: String) {
		Log.d(TAG, "Smart Static Snapshot state for '$slug': frozen=$frozen ($reason)")
		mainHandler.post { onStaticFrameCallback(slug, frozen, reason) }
	}

	@JavascriptInterface
	fun onGpuStatus(supported: Boolean, error: String) {
		Log.w(TAG, "WebGPU status: supported=$supported error=$error")
		mainHandler.post { onGpuStatusCallback(supported, error) }
	}

	fun dispatchTouch(action: String, x: Float, y: Float, pointerId: Int, pressure: Float) {
		evalBridgeCall("_dispatchTouch", "'$action',$x,$y,$pointerId,$pressure")
	}

	fun dispatchOffset(xOffset: Float, yOffset: Float, xStep: Float, yStep: Float) {
		evalBridgeCall("_dispatchOffset", "$xOffset,$yOffset,$xStep,$yStep")
	}

	fun dispatchTilt(gamma: Float, beta: Float, alpha: Float, ax: Float, ay: Float, az: Float) {
		evalBridgeCall("_dispatchTilt", "$gamma,$beta,$alpha,$ax,$ay,$az")
	}

	fun dispatchBattery(level: Int, charging: Boolean) {
		evalBridgeCall("_dispatchBattery", "$level,$charging")
	}

	fun dispatchIdleFreeze(frozen: Boolean, reason: String) {
		evalBridgeCall("_dispatchIdle", "$frozen,${JSONObject.quote(reason)}")
	}

	fun dispatchSetting(action: String, value: String) {
		val escapedKey = JSONObject.quote(action)
		val valueLiteral = when {
			value.equals("true", ignoreCase = true) -> "true"
			value.equals("false", ignoreCase = true) -> "false"
			value.toDoubleOrNull() != null -> value
			else -> JSONObject.quote(value)
		}
		evalBridgeCall("_dispatchSetting", "$escapedKey,$valueLiteral")
	}

	fun dispatchRuntimeConfig(fps: Int, dprScale: Float, paused: Boolean) {
		evalBridgeCall("_dispatchConfig", "{fps:$fps,dpr:$dprScale,paused:$paused}")
	}

	/** Themes may implement only part of the API (for example the built-in Canvas2D sample). */
	private fun evalBridgeCall(method: String, arguments: String) {
		val script =
			"(function(){var b=window.WallpaperGPU;if(b&&typeof b['$method']==='function')b['$method']($arguments);})();"
		evalOnMain(script)
	}

	private fun evalOnMain(script: String) {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			webViewProvider()?.evaluateJavascript(script, null)
		} else {
			mainHandler.post {
				webViewProvider()?.evaluateJavascript(script, null)
			}
		}
	}
}
