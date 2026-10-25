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

package io.github.kerneldroid.wallpapergpu.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class EnginePreferences(
	val engineEnabled: Boolean = true,
	val activeThemeSlug: String = "",
	val smartStaticFreezeEnabled: Boolean = true,
	val proximityPocketFreezeEnabled: Boolean = true,
	val idleTimeoutSeconds: Int = 30,
	val touchBridgeEnabled: Boolean = true,
	val parallaxBridgeEnabled: Boolean = true,
	val sensorTiltBridgeEnabled: Boolean = true,
	val targetFps: Int = 60,
	val renderScalePercent: Int = 100,
	val githubStarPromptDismissed: Boolean = false
)

class UserPreferencesRepository(
	private val dataStore: DataStore<Preferences>
) {
	private object Keys {
		val ENGINE_ENABLED = booleanPreferencesKey("engine_enabled")
		val ACTIVE_THEME_SLUG = stringPreferencesKey("active_theme_slug")
		val SMART_STATIC_FREEZE = booleanPreferencesKey("smart_static_freeze")
		val PROXIMITY_POCKET_FREEZE = booleanPreferencesKey("proximity_pocket_freeze")
		val IDLE_TIMEOUT_SECONDS = intPreferencesKey("idle_timeout_seconds")
		val TOUCH_BRIDGE_ENABLED = booleanPreferencesKey("touch_bridge_enabled")
		val PARALLAX_BRIDGE_ENABLED = booleanPreferencesKey("parallax_bridge_enabled")
		val SENSOR_TILT_BRIDGE_ENABLED = booleanPreferencesKey("sensor_tilt_bridge_enabled")
		val TARGET_FPS = intPreferencesKey("target_fps")
		val RENDER_SCALE_PERCENT = intPreferencesKey("render_scale_percent")
		val GITHUB_STAR_DISMISSED = booleanPreferencesKey("github_star_dismissed")
	}

	val preferencesFlow: Flow<EnginePreferences> = dataStore.data.map { prefs ->
		EnginePreferences(
			engineEnabled = prefs[Keys.ENGINE_ENABLED] ?: true,
			activeThemeSlug = prefs[Keys.ACTIVE_THEME_SLUG] ?: "",
			smartStaticFreezeEnabled = prefs[Keys.SMART_STATIC_FREEZE] ?: true,
			proximityPocketFreezeEnabled = prefs[Keys.PROXIMITY_POCKET_FREEZE] ?: true,
			idleTimeoutSeconds = prefs[Keys.IDLE_TIMEOUT_SECONDS] ?: 30,
			touchBridgeEnabled = prefs[Keys.TOUCH_BRIDGE_ENABLED] ?: true,
			parallaxBridgeEnabled = prefs[Keys.PARALLAX_BRIDGE_ENABLED] ?: true,
			sensorTiltBridgeEnabled = prefs[Keys.SENSOR_TILT_BRIDGE_ENABLED] ?: true,
			targetFps = prefs[Keys.TARGET_FPS] ?: 60,
			renderScalePercent = prefs[Keys.RENDER_SCALE_PERCENT] ?: 100,
			githubStarPromptDismissed = prefs[Keys.GITHUB_STAR_DISMISSED] ?: false
		)
	}

	suspend fun setEngineEnabled(enabled: Boolean) {
		dataStore.edit { it[Keys.ENGINE_ENABLED] = enabled }
	}

	suspend fun setActiveThemeSlug(slug: String) {
		dataStore.edit { it[Keys.ACTIVE_THEME_SLUG] = slug }
	}

	suspend fun setSmartStaticFreezeEnabled(enabled: Boolean) {
		dataStore.edit { it[Keys.SMART_STATIC_FREEZE] = enabled }
	}

	suspend fun setProximityPocketFreezeEnabled(enabled: Boolean) {
		dataStore.edit { it[Keys.PROXIMITY_POCKET_FREEZE] = enabled }
	}

	suspend fun setIdleTimeoutSeconds(seconds: Int) {
		dataStore.edit { it[Keys.IDLE_TIMEOUT_SECONDS] = seconds.coerceIn(5, 300) }
	}

	suspend fun setTouchBridgeEnabled(enabled: Boolean) {
		dataStore.edit { it[Keys.TOUCH_BRIDGE_ENABLED] = enabled }
	}

	suspend fun setParallaxBridgeEnabled(enabled: Boolean) {
		dataStore.edit { it[Keys.PARALLAX_BRIDGE_ENABLED] = enabled }
	}

	suspend fun setSensorTiltBridgeEnabled(enabled: Boolean) {
		dataStore.edit { it[Keys.SENSOR_TILT_BRIDGE_ENABLED] = enabled }
	}

	suspend fun setTargetFps(fps: Int) {
		dataStore.edit { it[Keys.TARGET_FPS] = fps.coerceIn(15, 120) }
	}

	suspend fun setRenderScalePercent(scalePercent: Int) {
		dataStore.edit { it[Keys.RENDER_SCALE_PERCENT] = scalePercent.coerceIn(40, 100) }
	}

	suspend fun dismissGitHubStarPrompt() {
		dataStore.edit { it[Keys.GITHUB_STAR_DISMISSED] = true }
	}
}
