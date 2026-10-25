/*
 * Copyright (C) 2026 kerneldroid (https://github.com/kerneldroid/WallpaperGPU)
 * Portions Copyright (C) 2024-2026 xLexip <https://lexip.dev>
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
 *
 * Adapted from Adaptive Theme (https://github.com/xLexip/Adaptive-Theme), GPL-3.0-or-later.
 */

package io.github.kerneldroid.wallpapergpu.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ProximitySensorManager(context: Context) : SensorEventListener {

	private val sensorManager: SensorManager =
		context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
	private var callback: ((Boolean, Float) -> Unit)? = null

	val hasProximitySensor: Boolean
		get() = proximitySensor != null

	val maximumRange: Float
		get() = proximitySensor?.maximumRange ?: 5f

	fun startListening(
		onChanged: (isCovered: Boolean, distanceCm: Float) -> Unit,
		sensorDelay: Int = SensorManager.SENSOR_DELAY_NORMAL
	): Boolean {
		val sensor = proximitySensor ?: return false
		this.callback = onChanged
		return sensorManager.registerListener(this, sensor, sensorDelay)
	}

	fun stopListening() {
		callback = null
		sensorManager.unregisterListener(this)
	}

	override fun onSensorChanged(event: SensorEvent) {
		val distance = event.values.firstOrNull() ?: return
		val threshold = (maximumRange * 0.5f).coerceAtMost(2.5f).coerceAtLeast(0.5f)
		val covered = distance < threshold
		callback?.invoke(covered, distance)
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
