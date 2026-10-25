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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Streams device orientation / accelerometer tilt angles (`gamma`, `beta`, `alpha`) to the
 * WallpaperGPU API Bridge when the wallpaper is actively visible.
 */
class TiltSensorManager(context: Context) : SensorEventListener {

	private val sensorManager: SensorManager =
		context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
	private var callback: ((gamma: Float, beta: Float, ax: Float, ay: Float, az: Float) -> Unit)? = null

	fun startListening(
		onTilt: (gamma: Float, beta: Float, ax: Float, ay: Float, az: Float) -> Unit,
		sensorDelay: Int = SensorManager.SENSOR_DELAY_GAME
	): Boolean {
		val sensor = accelSensor ?: return false
		this.callback = onTilt
		return sensorManager.registerListener(this, sensor, sensorDelay)
	}

	fun stopListening() {
		callback = null
		sensorManager.unregisterListener(this)
	}

	override fun onSensorChanged(event: SensorEvent) {
		if (event.values.size < 3) return
		val ax = event.values[0]
		val ay = event.values[1]
		val az = event.values[2]
		val gamma = (-ax * 9f).coerceIn(-90f, 90f)
		val beta = (ay * 9f).coerceIn(-180f, 180f)
		callback?.invoke(gamma, beta, ax, ay, az)
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
