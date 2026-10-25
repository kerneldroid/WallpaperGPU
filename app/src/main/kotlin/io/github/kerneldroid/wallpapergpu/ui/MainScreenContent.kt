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

package io.github.kerneldroid.wallpapergpu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kerneldroid.wallpapergpu.R
import io.github.kerneldroid.wallpapergpu.data.WallpaperThemePackage
import io.github.kerneldroid.wallpapergpu.ui.components.GitHubStarPromptCard
import io.github.kerneldroid.wallpapergpu.ui.components.MainSwitchPreferenceCard
import io.github.kerneldroid.wallpapergpu.ui.components.WallpaperCarouselSection
import io.github.kerneldroid.wallpapergpu.ui.components.preferences.DetailPreferenceCard
import io.github.kerneldroid.wallpapergpu.ui.components.preferences.ProgressDetailCard
import io.github.kerneldroid.wallpapergpu.ui.components.preferences.SliderDetailCard
import kotlin.math.roundToInt

private val TopCardShape = RoundedCornerShape(
	topStart = 24.dp,
	topEnd = 24.dp,
	bottomStart = 4.dp,
	bottomEnd = 4.dp
)

private val MiddleCardShape = RoundedCornerShape(4.dp)

private val BottomCardShape = RoundedCornerShape(
	topStart = 4.dp,
	topEnd = 4.dp,
	bottomStart = 24.dp,
	bottomEnd = 24.dp
)

@Composable
fun MainScreenContent(
	uiState: MainUiState,
	onEngineEnabledChange: (Boolean) -> Unit,
	onImportThemeClick: () -> Unit,
	onLoadSampleThemeClick: () -> Unit,
	onSelectCarouselTheme: (WallpaperThemePackage) -> Unit,
	onSmartStaticFreezeChange: (Boolean) -> Unit,
	onProximityPocketFreezeChange: (Boolean) -> Unit,
	onIdleTimeoutChange: (Int) -> Unit,
	onTouchBridgeChange: (Boolean) -> Unit,
	onParallaxBridgeChange: (Boolean) -> Unit,
	onSensorTiltBridgeChange: (Boolean) -> Unit,
	onTargetFpsChange: (Int) -> Unit,
	onRenderScaleChange: (Int) -> Unit,
	onOpenGitHubClick: () -> Unit,
	onDismissGitHubPrompt: () -> Unit,
	onOpenAdaptiveThemeCredits: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier.fillMaxWidth(),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		// 1. Master Engine Switch (Adaptive Theme MainSwitchPreferenceCard)
		MainSwitchPreferenceCard(
			title = stringResource(id = R.string.title_enable_wallpapergpu),
			checked = uiState.engineEnabled,
			onCheckedChange = onEngineEnabledChange
		)

		// 2. Material Design 3 Carousel of Imported .theme Wallpapers
		WallpaperCarouselSection(
			themes = uiState.importedThemes,
			activeThemeSlug = uiState.activeThemeSlug,
			onImportThemeClick = onImportThemeClick,
			onLoadSampleThemeClick = onLoadSampleThemeClick,
			onSelectTheme = onSelectCarouselTheme
		)

		// 3. Smart Static Wallpaper Conversion Group (Adaptive Theme Expressive Connected Cards)
		SectionTitle(text = stringResource(id = R.string.section_smart_static))

		Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
			DetailPreferenceCard(
				title = stringResource(id = R.string.title_smart_static_freeze),
				description = stringResource(id = R.string.description_smart_static_freeze),
				icon = Icons.Outlined.BatterySaver,
				enabled = uiState.engineEnabled,
				shape = TopCardShape,
				isSwitchChecked = uiState.smartStaticFreezeEnabled,
				onSwitchChange = onSmartStaticFreezeChange
			)

			DetailPreferenceCard(
				title = stringResource(id = R.string.title_proximity_pocket_freeze),
				description = stringResource(id = R.string.description_proximity_pocket_freeze),
				icon = Icons.Outlined.ScreenLockPortrait,
				enabled = uiState.engineEnabled && uiState.smartStaticFreezeEnabled,
				shape = MiddleCardShape,
				isSwitchChecked = uiState.proximityPocketFreezeEnabled,
				onSwitchChange = onProximityPocketFreezeChange
			)

			SliderDetailCard(
				title = stringResource(id = R.string.title_idle_timeout),
				description = stringResource(
					id = R.string.description_idle_timeout,
					uiState.idleTimeoutSeconds
				),
				icon = Icons.Outlined.Timer,
				enabled = uiState.engineEnabled && uiState.smartStaticFreezeEnabled,
				shape = MiddleCardShape,
				sliderValue = uiState.idleTimeoutSeconds.toFloat(),
				valueRange = 10f..120f,
				steps = 10,
				onValueChange = { onIdleTimeoutChange(it.roundToInt()) }
			)

			ProgressDetailCard(
				title = stringResource(id = R.string.title_gpu_runtime_status),
				subtitle = stringResource(
					id = R.string.status_live_rendering,
					uiState.targetFps
				) + " · Ambient ${uiState.currentAmbientLux.roundToInt()} lx",
				icon = Icons.Outlined.Memory,
				enabled = uiState.engineEnabled,
				shape = BottomCardShape,
				progress = (uiState.targetFps / 120f).coerceIn(0.15f, 1f)
			)
		}

		// 4. WallpaperGPU API Bridge & Performance Group
		SectionTitle(text = stringResource(id = R.string.section_bridge_settings))

		Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
			DetailPreferenceCard(
				title = stringResource(id = R.string.title_touch_bridge),
				description = stringResource(id = R.string.description_touch_bridge),
				icon = Icons.Outlined.TouchApp,
				enabled = uiState.engineEnabled,
				shape = TopCardShape,
				isSwitchChecked = uiState.touchBridgeEnabled,
				onSwitchChange = onTouchBridgeChange
			)

			DetailPreferenceCard(
				title = stringResource(id = R.string.title_parallax_bridge),
				description = stringResource(id = R.string.description_parallax_bridge),
				icon = Icons.Outlined.Swipe,
				enabled = uiState.engineEnabled,
				shape = MiddleCardShape,
				isSwitchChecked = uiState.parallaxBridgeEnabled,
				onSwitchChange = onParallaxBridgeChange
			)

			DetailPreferenceCard(
				title = stringResource(id = R.string.title_sensor_tilt_bridge),
				description = stringResource(id = R.string.description_sensor_tilt_bridge),
				icon = Icons.Outlined.Explore,
				enabled = uiState.engineEnabled,
				shape = MiddleCardShape,
				isSwitchChecked = uiState.sensorTiltBridgeEnabled,
				onSwitchChange = onSensorTiltBridgeChange
			)

			SliderDetailCard(
				title = stringResource(id = R.string.title_target_fps),
				description = stringResource(id = R.string.description_target_fps, uiState.targetFps),
				icon = Icons.Outlined.Speed,
				enabled = uiState.engineEnabled,
				shape = MiddleCardShape,
				sliderValue = uiState.targetFps.toFloat(),
				valueRange = 30f..120f,
				steps = 2,
				onValueChange = { onTargetFpsChange(it.roundToInt()) }
			)

			SliderDetailCard(
				title = stringResource(id = R.string.title_render_scale),
				description = stringResource(
					id = R.string.description_render_scale,
					uiState.renderScalePercent
				),
				icon = Icons.Outlined.AspectRatio,
				enabled = uiState.engineEnabled,
				shape = BottomCardShape,
				sliderValue = uiState.renderScalePercent.toFloat(),
				valueRange = 50f..100f,
				steps = 4,
				onValueChange = { onRenderScaleChange(it.roundToInt()) }
			)
		}

		// 5. GitHub Star Prompt Card (kerneldroid/WallpaperGPU)
		if (uiState.showGitHubStarPrompt) {
			GitHubStarPromptCard(
				onStarClick = onOpenGitHubClick,
				onDismissClick = onDismissGitHubPrompt
			)
		}

		// 6. Preserved Adaptive Theme Credits Card (xLexip)
		DetailPreferenceCard(
			title = stringResource(id = R.string.credits_card_title),
			description = stringResource(id = R.string.credits_card_body),
			icon = Icons.Outlined.Info,
			onClick = onOpenAdaptiveThemeCredits
		)

		Spacer(modifier = Modifier.height(16.dp))
	}
}

@Composable
private fun SectionTitle(text: String) {
	Text(
		text = text,
		style = MaterialTheme.typography.labelLarge,
		color = MaterialTheme.colorScheme.primary,
		modifier = Modifier.padding(start = 12.dp, top = 4.dp)
	)
}
