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

package io.github.kerneldroid.wallpapergpu.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.kerneldroid.wallpapergpu.Application
import io.github.kerneldroid.wallpapergpu.data.EnginePreferences
import io.github.kerneldroid.wallpapergpu.data.ThemeRepository
import io.github.kerneldroid.wallpapergpu.data.UserPreferencesRepository
import io.github.kerneldroid.wallpapergpu.data.WallpaperThemePackage
import io.github.kerneldroid.wallpapergpu.util.LightSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
	val engineEnabled: Boolean = true,
	val activeThemeSlug: String = "",
	val importedThemes: List<WallpaperThemePackage> = emptyList(),
	val selectedThemeForApply: WallpaperThemePackage? = null,
	val selectedThemeSceneValues: Map<String, String> = emptyMap(),
	val smartStaticFreezeEnabled: Boolean = true,
	val proximityPocketFreezeEnabled: Boolean = true,
	val idleTimeoutSeconds: Int = 30,
	val touchBridgeEnabled: Boolean = true,
	val parallaxBridgeEnabled: Boolean = true,
	val sensorTiltBridgeEnabled: Boolean = true,
	val targetFps: Int = 60,
	val renderScalePercent: Int = 100,
	val currentAmbientLux: Float = 120f,
	val showGitHubStarPrompt: Boolean = true,
	val statusMessage: String? = null
)

class MainViewModel(
	application: Application,
	private val preferencesRepository: UserPreferencesRepository,
	private val themeRepository: ThemeRepository
) : ViewModel() {

	private val lightSensorManager = LightSensorManager(application)
	private val selectedSlugFlow = MutableStateFlow<String?>(null)
	private val ambientLuxFlow = MutableStateFlow(120f)
	private val statusMessageFlow = MutableStateFlow<String?>(null)

	val uiState: StateFlow<MainUiState> = combine(
		preferencesRepository.preferencesFlow,
		themeRepository.importedThemes,
		themeRepository.sceneValues,
		selectedSlugFlow,
		combine(ambientLuxFlow, statusMessageFlow) { lux, msg -> lux to msg }
	) { prefs: EnginePreferences,
		themes: List<WallpaperThemePackage>,
		allSceneValues: Map<String, Map<String, String>>,
		selectedSlug: String?,
		(lux, statusMsg) ->

		val selectedPkg = themes.firstOrNull { it.slug == selectedSlug }
		val sceneMap = if (selectedPkg != null) {
			selectedPkg.defaultValues + (allSceneValues[selectedPkg.slug] ?: emptyMap())
		} else {
			emptyMap()
		}

		MainUiState(
			engineEnabled = prefs.engineEnabled,
			activeThemeSlug = prefs.activeThemeSlug.ifBlank { themes.firstOrNull()?.slug ?: "" },
			importedThemes = themes,
			selectedThemeForApply = selectedPkg,
			selectedThemeSceneValues = sceneMap,
			smartStaticFreezeEnabled = prefs.smartStaticFreezeEnabled,
			proximityPocketFreezeEnabled = prefs.proximityPocketFreezeEnabled,
			idleTimeoutSeconds = prefs.idleTimeoutSeconds,
			touchBridgeEnabled = prefs.touchBridgeEnabled,
			parallaxBridgeEnabled = prefs.parallaxBridgeEnabled,
			sensorTiltBridgeEnabled = prefs.sensorTiltBridgeEnabled,
			targetFps = prefs.targetFps,
			renderScalePercent = prefs.renderScalePercent,
			currentAmbientLux = lux,
			showGitHubStarPrompt = !prefs.githubStarPromptDismissed,
			statusMessage = statusMsg
		)
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5000),
		initialValue = MainUiState()
	)

	fun onUiResumed() {
		themeRepository.reloadInstalledThemes()
		lightSensorManager.startListening(onLuxChanged = { lux ->
			ambientLuxFlow.value = lux
		})
	}

	fun onUiPaused() {
		lightSensorManager.stopListening()
	}

	fun importThemeUri(uri: Uri) {
		viewModelScope.launch {
			val result = themeRepository.importThemeFromUri(uri)
			result.onSuccess { pkg ->
				preferencesRepository.setActiveThemeSlug(pkg.slug)
				selectedSlugFlow.value = pkg.slug
				statusMessageFlow.value = "Imported '${pkg.name}' (.theme) into carousel"
			}.onFailure { err ->
				statusMessageFlow.value = "Failed to import .theme: ${err.message}"
			}
		}
	}

	fun installBuiltInSampleTheme() {
		viewModelScope.launch {
			val pkg = themeRepository.installBuiltInSampleTheme()
			preferencesRepository.setActiveThemeSlug(pkg.slug)
			selectedSlugFlow.value = pkg.slug
			statusMessageFlow.value = "Loaded '${pkg.name}' into carousel"
		}
	}

	fun openApplySheet(pkg: WallpaperThemePackage) {
		selectedSlugFlow.value = pkg.slug
	}

	fun dismissApplySheet() {
		selectedSlugFlow.value = null
	}

	fun updateSceneSetting(slug: String, key: String, value: String) {
		themeRepository.updateSceneValue(slug, key, value)
	}

	fun activateTheme(pkg: WallpaperThemePackage) {
		viewModelScope.launch {
			preferencesRepository.setActiveThemeSlug(pkg.slug)
			preferencesRepository.setEngineEnabled(true)
			selectedSlugFlow.value = null
		}
	}

	fun removeTheme(pkg: WallpaperThemePackage) {
		viewModelScope.launch {
			selectedSlugFlow.value = null
			themeRepository.removeTheme(pkg.slug)
			statusMessageFlow.value = "Removed '${pkg.name}'"
		}
	}

	fun clearStatusMessage() {
		statusMessageFlow.value = null
	}

	fun setEngineEnabled(enabled: Boolean) {
		viewModelScope.launch { preferencesRepository.setEngineEnabled(enabled) }
	}

	fun setSmartStaticFreezeEnabled(enabled: Boolean) {
		viewModelScope.launch { preferencesRepository.setSmartStaticFreezeEnabled(enabled) }
	}

	fun setProximityPocketFreezeEnabled(enabled: Boolean) {
		viewModelScope.launch { preferencesRepository.setProximityPocketFreezeEnabled(enabled) }
	}

	fun setIdleTimeoutSeconds(seconds: Int) {
		viewModelScope.launch { preferencesRepository.setIdleTimeoutSeconds(seconds) }
	}

	fun setTouchBridgeEnabled(enabled: Boolean) {
		viewModelScope.launch { preferencesRepository.setTouchBridgeEnabled(enabled) }
	}

	fun setParallaxBridgeEnabled(enabled: Boolean) {
		viewModelScope.launch { preferencesRepository.setParallaxBridgeEnabled(enabled) }
	}

	fun setSensorTiltBridgeEnabled(enabled: Boolean) {
		viewModelScope.launch { preferencesRepository.setSensorTiltBridgeEnabled(enabled) }
	}

	fun setTargetFps(fps: Int) {
		viewModelScope.launch { preferencesRepository.setTargetFps(fps) }
	}

	fun setRenderScalePercent(scalePercent: Int) {
		viewModelScope.launch { preferencesRepository.setRenderScalePercent(scalePercent) }
	}

	fun dismissGitHubStarPrompt() {
		viewModelScope.launch { preferencesRepository.dismissGitHubStarPrompt() }
	}
}

class MainViewModelFactory(
	private val application: Application,
	private val preferencesRepository: UserPreferencesRepository,
	private val themeRepository: ThemeRepository
) : ViewModelProvider.Factory {
	@Suppress("UNCHECKED_CAST")
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
			return MainViewModel(application, preferencesRepository, themeRepository) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
	}
}
