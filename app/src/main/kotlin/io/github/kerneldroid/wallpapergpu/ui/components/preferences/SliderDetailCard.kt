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

package io.github.kerneldroid.wallpapergpu.ui.components.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SliderDetailCard(
	title: String,
	description: String,
	icon: ImageVector,
	enabled: Boolean = true,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
	shape: Shape = RoundedCornerShape(24.dp),
	sliderValue: Float,
	valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
	steps: Int = 0,
	onValueChange: (Float) -> Unit
) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		shape = shape,
		colors = CardDefaults.cardColors(containerColor = containerColor)
	) {
		Row(
			modifier = Modifier
				.padding(20.dp)
				.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Icon(
				imageVector = icon,
				contentDescription = title,
				tint = if (enabled) {
					MaterialTheme.colorScheme.primary
				} else {
					MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
				},
				modifier = Modifier.size(28.dp)
			)

			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium,
					color = if (enabled) {
						MaterialTheme.colorScheme.onSurface
					} else {
						MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
					}
				)
				Text(
					text = description,
					style = MaterialTheme.typography.bodyMedium,
					color = if (enabled) {
						MaterialTheme.colorScheme.onSurfaceVariant
					} else {
						MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
					},
					modifier = Modifier.padding(top = 4.dp)
				)
				Slider(
					value = sliderValue.coerceIn(valueRange.start, valueRange.endInclusive),
					onValueChange = onValueChange,
					valueRange = valueRange,
					steps = steps,
					enabled = enabled,
					modifier = Modifier.padding(top = 4.dp)
				)
			}
		}
	}
}
