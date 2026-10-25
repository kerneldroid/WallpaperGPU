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

package io.github.kerneldroid.wallpapergpu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedProgressIndicator(
	progress: Float,
	modifier: Modifier = Modifier,
	trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
	progressColor: Color = MaterialTheme.colorScheme.primary,
	gapWidth: Dp = 4.dp,
	cornerRadius: Dp = 2.dp
) {
	val clampedTarget = progress.coerceIn(0f, 1f)
	val animatedProgress by animateFloatAsState(
		targetValue = clampedTarget,
		animationSpec = tween(durationMillis = 350),
		label = "SegmentedProgressIndicator"
	)

	Canvas(modifier = modifier) {
		val width = size.width
		val height = size.height
		val gapPx = gapWidth.toPx()
		val radiusPx = cornerRadius.toPx()
		val progressWidth = width * animatedProgress
		val hasGap = animatedProgress > 0f && animatedProgress < 1f
		val effectiveGap = if (hasGap) gapPx else 0f

		if (progressWidth > 0f) {
			val filledWidth = (progressWidth - effectiveGap / 2f).coerceAtLeast(0f)
			if (filledWidth > 0f) {
				drawRoundRect(
					color = progressColor,
					topLeft = Offset(0f, 0f),
					size = Size(filledWidth, height),
					cornerRadius = CornerRadius(radiusPx, radiusPx)
				)
			}
		}

		val remainingStart = (progressWidth + effectiveGap / 2f).coerceAtMost(width)
		val remainingWidth = (width - remainingStart).coerceAtLeast(0f)
		if (remainingWidth > 0f) {
			drawRoundRect(
				color = trackColor,
				topLeft = Offset(remainingStart, 0f),
				size = Size(remainingWidth, height),
				cornerRadius = CornerRadius(radiusPx, radiusPx)
			)
		}
	}
}
