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

package io.github.kerneldroid.wallpapergpu.data

data class ThemeCapabilities(
	val touch: Boolean = true,
	val multiTouch: Boolean = true,
	val parallaxOffset: Boolean = true,
	val deviceTilt: Boolean = true,
	val batteryLevel: Boolean = false,
	val smartStaticIdle: Boolean = true
)

data class SmartStaticPolicy(
	val freezeOnVisibilityLost: Boolean = true,
	val freezeOnScreenOff: Boolean = true,
	val freezeOnProximityCovered: Boolean = true,
	val inactivityTimeoutSeconds: Int = 30,
	val captureStaticSnapshot: Boolean = true
)

data class ThemeSettingOption(
	val value: String,
	val label: String
)

data class ThemeSettingField(
	val id: String,
	val type: String, // "select" | "segmented" | "slider" | "switch"
	val group: String,
	val label: String,
	val options: List<ThemeSettingOption> = emptyList(),
	val min: Float = 0f,
	val max: Float = 100f,
	val step: Float = 1f
)

data class WallpaperThemePackage(
	val id: String,
	val slug: String,
	val name: String,
	val category: String,
	val description: String,
	val author: String,
	val license: String,
	val accentColors: List<String>,
	val entrypoint: String,
	val shaderFile: String,
	val textureCount: Int,
	val installDirPath: String,
	val importedAtMillis: Long,
	val capabilities: ThemeCapabilities = ThemeCapabilities(),
	val smartStaticPolicy: SmartStaticPolicy = SmartStaticPolicy(),
	val settingsSchema: List<ThemeSettingField> = emptyList(),
	val defaultValues: Map<String, String> = emptyMap()
)
