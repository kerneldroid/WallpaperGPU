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

import android.util.Log
import org.json.JSONObject

private const val TAG = "WgpuNativeBridge"

/**
 * JNI Bridge to the native Rust `wallpapergpu-core` library (`libwallpapergpu_core.so`).
 *
 * Provides native `.theme` manifest validation, 64-byte WebGPU uniform buffer synchronization,
 * and the Rust Smart Static Conversion state machine. Gracefully falls back to equivalent Kotlin
 * verification if the native shared library is not present on the current ABI.
 */
object WgpuNativeBridge {

	val isNativeLibraryLoaded: Boolean = try {
		System.loadLibrary("wallpapergpu_core")
		Log.i(TAG, "Loaded native Rust library libwallpapergpu_core.so")
		true
	} catch (e: UnsatisfiedLinkError) {
		Log.w(TAG, "libwallpapergpu_core.so not found for ABI; using Kotlin bridge fallback.")
		false
	}

	@JvmStatic
	private external fun nativeCreateEngine(idleTimeoutMs: Long): Long

	@JvmStatic
	private external fun nativeDestroyEngine(handle: Long)

	@JvmStatic
	private external fun nativeOnTouch(
		handle: Long,
		action: Int,
		x: Float,
		y: Float,
		pressure: Float,
		timestampMs: Float
	)

	@JvmStatic
	private external fun nativeOnOffsetsChanged(
		handle: Long,
		xOffset: Float,
		yOffset: Float
	)

	@JvmStatic
	private external fun nativeEvaluateSmartIdle(
		handle: Long,
		visible: Boolean,
		screenInteractive: Boolean,
		proximityCovered: Boolean,
		powerSaveActive: Boolean,
		deltaMs: Long
	): Boolean

	@JvmStatic
	private external fun nativeValidateThemeManifest(manifestJson: String): String

	fun createEngine(idleTimeoutMs: Long): Long {
		return if (isNativeLibraryLoaded) {
			runCatching { nativeCreateEngine(idleTimeoutMs) }.getOrDefault(1L)
		} else {
			1L
		}
	}

	fun destroyEngine(handle: Long) {
		if (isNativeLibraryLoaded && handle != 0L) {
			runCatching { nativeDestroyEngine(handle) }
		}
	}

	fun onTouch(handle: Long, action: Int, x: Float, y: Float, pressure: Float, timestampMs: Float) {
		if (isNativeLibraryLoaded && handle != 0L) {
			runCatching { nativeOnTouch(handle, action, x, y, pressure, timestampMs) }
		}
	}

	fun onOffsetsChanged(handle: Long, xOffset: Float, yOffset: Float) {
		if (isNativeLibraryLoaded && handle != 0L) {
			runCatching { nativeOnOffsetsChanged(handle, xOffset, yOffset) }
		}
	}

	fun evaluateSmartIdle(
		handle: Long,
		visible: Boolean,
		screenInteractive: Boolean,
		proximityCovered: Boolean,
		powerSaveActive: Boolean,
		deltaMs: Long
	): Boolean {
		if (isNativeLibraryLoaded && handle != 0L) {
			return runCatching {
				nativeEvaluateSmartIdle(
					handle,
					visible,
					screenInteractive,
					proximityCovered,
					powerSaveActive,
					deltaMs
				)
			}.getOrDefault(!visible || !screenInteractive || proximityCovered || powerSaveActive)
		}
		return !visible || !screenInteractive || proximityCovered || powerSaveActive
	}

	fun validateThemeManifest(manifestJson: String): Pair<Boolean, String?> {
		if (isNativeLibraryLoaded) {
			runCatching {
				val resStr = nativeValidateThemeManifest(manifestJson)
				val obj = JSONObject(resStr)
				val valid = obj.optBoolean("valid", false)
				val err = if (obj.isNull("error")) null else obj.optString("error")
				return Pair(valid, err)
			}
		}
		return try {
			val obj = JSONObject(manifestJson)
			val format = obj.optString("format", "")
			val slug = obj.optString("slug", "")
			val entrypoint = obj.optString("entrypoint", "index.html")
			if (format != "WallpaperGPU.Theme") {
				Pair(false, "Unsupported theme format '$format'; expected 'WallpaperGPU.Theme'")
			} else if (slug.isBlank() || entrypoint.isBlank()) {
				Pair(false, "Missing 'slug' or 'entrypoint' in theme.json")
			} else {
				Pair(true, null)
			}
		} catch (e: Exception) {
			Pair(false, "Invalid theme.json: ${e.message}")
		}
	}
}
