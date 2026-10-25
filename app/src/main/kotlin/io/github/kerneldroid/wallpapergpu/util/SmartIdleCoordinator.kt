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

package io.github.kerneldroid.wallpapergpu.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import io.github.kerneldroid.wallpapergpu.engine.WgpuNativeBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SmartStaticSnapshotStatus(
	val isStaticFrozen: Boolean = false,
	val reason: String = "active",
	val ambientLux: Float = 120f,
	val proximityCovered: Boolean = false
)

/**
 * Coordinates WallpaperGPU's Smart Static Wallpaper Conversion.
 *
 * Monitors surface visibility (`onVisibilityChanged`), screen interactive state (`PowerManager`),
 * proximity sensor coverage (pocket / face-down detection), and user touch/swipe inactivity.
 * When nobody is looking at the wallpaper, instructs the engine to capture a static frame and
 * freeze WebGPU rendering (0% GPU usage).
 */
class SmartIdleCoordinator(
	context: Context,
	private val onFreezeStateChanged: (frozen: Boolean, reason: String) -> Unit
) {
	private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
	private val proximitySensorManager = ProximitySensorManager(context)
	private val lightSensorManager = LightSensorManager(context)
	private val handler = Handler(Looper.getMainLooper())

	private var nativeEngineHandle: Long = WgpuNativeBridge.createEngine(30_000L)

	private var smartFreezeEnabled: Boolean = true
	private var proximityFreezeEnabled: Boolean = true
	private var idleTimeoutMs: Long = 30_000L

	private var surfaceVisible: Boolean = true
	private var proximityCovered: Boolean = false
	private var ambientLux: Float = 120f
	private var lastInteractionTimeMs: Long = System.currentTimeMillis()

	private val _status = MutableStateFlow(SmartStaticSnapshotStatus())
	val status: StateFlow<SmartStaticSnapshotStatus> = _status.asStateFlow()

	private val watchdogRunnable = object : Runnable {
		override fun run() {
			evaluateState()
			if (surfaceVisible) {
				handler.postDelayed(this, 1000L)
			}
		}
	}

	fun configure(
		smartFreezeEnabled: Boolean,
		proximityFreezeEnabled: Boolean,
		idleTimeoutSeconds: Int
	) {
		this.smartFreezeEnabled = smartFreezeEnabled
		this.proximityFreezeEnabled = proximityFreezeEnabled
		this.idleTimeoutMs = (idleTimeoutSeconds.coerceAtLeast(5) * 1000L)
		evaluateState()
	}

	fun start() {
		lastInteractionTimeMs = System.currentTimeMillis()
		proximitySensorManager.startListening(onChanged = { covered, _ ->
			proximityCovered = covered
			evaluateState()
		})
		lightSensorManager.startListening(onLuxChanged = { lux ->
			ambientLux = lux
			_status.value = _status.value.copy(ambientLux = lux)
		})
		handler.removeCallbacks(watchdogRunnable)
		handler.post(watchdogRunnable)
	}

	fun stop() {
		handler.removeCallbacks(watchdogRunnable)
		proximitySensorManager.stopListening()
		lightSensorManager.stopListening()
	}

	fun destroy() {
		stop()
		WgpuNativeBridge.destroyEngine(nativeEngineHandle)
		nativeEngineHandle = 0L
	}

	fun onVisibilityChanged(visible: Boolean) {
		surfaceVisible = visible
		if (visible) {
			lastInteractionTimeMs = System.currentTimeMillis()
			start()
		} else {
			stop()
		}
		evaluateState()
	}

	fun onUserInteraction() {
		lastInteractionTimeMs = System.currentTimeMillis()
		if (_status.value.isStaticFrozen) {
			evaluateState()
		}
	}

	private fun evaluateState() {
		val screenInteractive = powerManager.isInteractive
		val powerSave = powerManager.isPowerSaveMode
		val effectiveProximity = proximityFreezeEnabled && proximityCovered
		val idleElapsedMs = System.currentTimeMillis() - lastInteractionTimeMs

		val nativeFrozen = WgpuNativeBridge.evaluateSmartIdle(
			handle = nativeEngineHandle,
			visible = surfaceVisible,
			screenInteractive = screenInteractive,
			proximityCovered = effectiveProximity,
			powerSaveActive = powerSave && smartFreezeEnabled,
			deltaMs = 1000L
		)

		val timeoutFrozen = smartFreezeEnabled && idleElapsedMs >= idleTimeoutMs
		val shouldFreeze = !screenInteractive || !surfaceVisible || (smartFreezeEnabled && (nativeFrozen || timeoutFrozen))

		val reason = when {
			!screenInteractive -> "screen-off"
			!surfaceVisible -> "home-hidden"
			effectiveProximity -> "proximity-covered"
			powerSave && smartFreezeEnabled -> "battery-saver"
			timeoutFrozen -> "idle-timeout"
			else -> "active"
		}

		val prev = _status.value
		if (prev.isStaticFrozen != shouldFreeze || prev.reason != reason || prev.proximityCovered != effectiveProximity) {
			_status.value = SmartStaticSnapshotStatus(
				isStaticFrozen = shouldFreeze,
				reason = reason,
				ambientLux = ambientLux,
				proximityCovered = effectiveProximity
			)
			onFreezeStateChanged(shouldFreeze, reason)
		}
	}
}
