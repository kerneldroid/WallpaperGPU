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

package io.github.kerneldroid.wallpapergpu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kerneldroid.wallpapergpu.R
import io.github.kerneldroid.wallpapergpu.data.ThemeSettingField
import io.github.kerneldroid.wallpapergpu.data.WallpaperThemePackage
import kotlin.math.roundToInt

/**
 * Modal Bottom Sheet displayed when a user taps a wallpaper inside the Material 3 Carousel.
 *
 * Lets the user inspect the `.theme` package capabilities (`Touch`, `Parallax`, `Tilt`, `Smart Static`),
 * customize the wallpaper's scene settings, and tap **"Apply as Live Wallpaper"** to activate the
 * theme and launch Android's Live Wallpaper picker for `WallpaperGpuService`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApplyWallpaperSheet(
	pkg: WallpaperThemePackage,
	sceneValues: Map<String, String>,
	onSceneValueChange: (key: String, value: String) -> Unit,
	onApplyAsLiveWallpaper: (WallpaperThemePackage) -> Unit,
	onRemoveTheme: (WallpaperThemePackage) -> Unit,
	onDismiss: () -> Unit
) {
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
	val gradientColors = pkg.accentColors.mapNotNull { hex ->
		runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
	}.ifEmpty {
		listOf(Color(0xFF0A1936), Color(0xFF1D5AA8), Color(0xFF9BC4FF))
	}

	ModalBottomSheet(
		onDismissRequest = onDismiss,
		sheetState = sheetState,
		shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 24.dp, vertical = 12.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			// Preview Banner
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(148.dp)
					.clip(RoundedCornerShape(24.dp))
					.background(Brush.linearGradient(gradientColors))
					.padding(20.dp)
			) {
				Column(
					modifier = Modifier.align(Alignment.BottomStart),
					verticalArrangement = Arrangement.spacedBy(4.dp)
				) {
					Text(
						text = pkg.name,
						style = MaterialTheme.typography.headlineSmall,
						color = Color.White
					)
					Text(
						text = pkg.description,
						style = MaterialTheme.typography.bodyMedium,
						color = Color.White.copy(alpha = 0.88f)
					)
				}
			}

			// Capability badges
			FlowRow(
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalArrangement = Arrangement.spacedBy(6.dp)
			) {
				AssistChip(onClick = {}, label = { Text("WebGPU · ${pkg.category}") })
				if (pkg.textureCount > 0) {
					AssistChip(onClick = {}, label = { Text("${pkg.textureCount} AVIF Textures") })
				} else {
					AssistChip(onClick = {}, label = { Text("Procedural WGSL") })
				}
				if (pkg.capabilities.touch) {
					AssistChip(onClick = {}, label = { Text("Touch Bridge") })
				}
				if (pkg.capabilities.parallaxOffset) {
					AssistChip(onClick = {}, label = { Text("Launcher Parallax") })
				}
				if (pkg.capabilities.smartStaticIdle) {
					AssistChip(onClick = {}, label = { Text("Smart Static 0% Idle") })
				}
			}

			// Scene customization controls from theme.json
			if (pkg.settingsSchema.isNotEmpty()) {
				Text(
					text = "Scene Parameters",
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.onSurface
				)
				for (field in pkg.settingsSchema) {
					SceneSettingControl(
						field = field,
						currentValue = sceneValues[field.id] ?: pkg.defaultValues[field.id] ?: "",
						onValueChange = { next -> onSceneValueChange(field.id, next) }
					)
				}
			}

			// Primary Actions: Apply as Live Wallpaper + Remove
			Column(
				modifier = Modifier.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				Button(
					onClick = { onApplyAsLiveWallpaper(pkg) },
					modifier = Modifier
						.fillMaxWidth()
						.height(54.dp),
					shape = RoundedCornerShape(18.dp)
				) {
					Icon(
						imageVector = Icons.Outlined.Wallpaper,
						contentDescription = null,
						modifier = Modifier.size(20.dp)
					)
					Text(
						text = stringResource(id = R.string.action_set_live_wallpaper),
						style = MaterialTheme.typography.titleMedium,
						modifier = Modifier.padding(start = 8.dp)
					)
				}

				OutlinedButton(
					onClick = { onRemoveTheme(pkg) },
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(18.dp)
				) {
					Icon(
						imageVector = Icons.Outlined.DeleteOutline,
						contentDescription = null,
						modifier = Modifier.size(18.dp)
					)
					Text(
						text = stringResource(id = R.string.action_remove_theme),
						modifier = Modifier.padding(start = 6.dp)
					)
				}
			}

			Spacer(modifier = Modifier.height(16.dp))
		}
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SceneSettingControl(
	field: ThemeSettingField,
	currentValue: String,
	onValueChange: (String) -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(16.dp))
			.background(MaterialTheme.colorScheme.surfaceContainerHigh)
			.padding(14.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		when (field.type) {
			"switch" -> {
				val checked = currentValue.equals("true", ignoreCase = true) || currentValue == "1"
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Text(
						text = field.label,
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurface
					)
					Switch(
						checked = checked,
						onCheckedChange = { onValueChange(it.toString()) }
					)
				}
			}

			"slider" -> {
				val numeric = currentValue.toFloatOrNull() ?: field.min
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Text(
						text = field.label,
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurface
					)
					Text(
						text = numeric.roundToInt().toString(),
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.primary
					)
				}
				Slider(
					value = numeric.coerceIn(field.min, field.max),
					onValueChange = { onValueChange(it.toString()) },
					valueRange = field.min..field.max
				)
			}

			else -> {
				Text(
					text = field.label,
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurface
				)
				FlowRow(
					horizontalArrangement = Arrangement.spacedBy(8.dp),
					verticalArrangement = Arrangement.spacedBy(4.dp)
				) {
					for (opt in field.options) {
						FilterChip(
							selected = currentValue == opt.value,
							onClick = { onValueChange(opt.value) },
							label = { Text(opt.label) }
						)
					}
				}
			}
		}
	}
}
