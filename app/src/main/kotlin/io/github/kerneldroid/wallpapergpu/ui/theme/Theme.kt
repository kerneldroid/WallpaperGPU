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

package io.github.kerneldroid.wallpapergpu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.kerneldroid.wallpapergpu.R

private val DarkColorScheme = darkColorScheme(
	primary = Color(0xFFA8C8FF),
	secondary = Color(0xFFBCC7DC),
	tertiary = Color(0xFFDCBCE1)
)

private val LightColorScheme = lightColorScheme(
	primary = Color(0xFF1E5AA8),
	secondary = Color(0xFF545F71),
	tertiary = Color(0xFF6E5676)
)

private val NunitoFontFamily = FontFamily(
	Font(R.font.nunito_bold, weight = FontWeight.Bold),
	Font(R.font.nunito_semibold, weight = FontWeight.SemiBold),
	Font(R.font.nunito_regular, weight = FontWeight.Normal)
)

private val BaseTypography = Typography()

private val WallpaperGpuTypography = Typography(
	displayLarge = BaseTypography.displayLarge.copy(
		fontFamily = NunitoFontFamily,
		fontWeight = FontWeight.Bold
	),
	displayMedium = BaseTypography.displayMedium.copy(
		fontFamily = NunitoFontFamily,
		fontWeight = FontWeight.Bold
	),
	displaySmall = BaseTypography.displaySmall.copy(
		fontFamily = NunitoFontFamily,
		fontWeight = FontWeight.Bold
	),
	headlineLarge = BaseTypography.headlineLarge.copy(
		fontFamily = NunitoFontFamily,
		fontWeight = FontWeight.Bold
	),
	headlineMedium = BaseTypography.headlineMedium.copy(
		fontFamily = NunitoFontFamily,
		fontWeight = FontWeight.Bold
	),
	headlineSmall = BaseTypography.headlineSmall.copy(
		fontFamily = NunitoFontFamily,
		fontWeight = FontWeight.SemiBold
	),
	titleLarge = BaseTypography.titleLarge.copy(
		fontFamily = NunitoFontFamily,
		fontWeight = FontWeight.Bold
	),
	titleMedium = BaseTypography.titleMedium.copy(
		fontFamily = NunitoFontFamily,
		fontWeight = FontWeight.SemiBold
	),
	titleSmall = BaseTypography.titleSmall.copy(
		fontFamily = NunitoFontFamily,
		fontWeight = FontWeight.SemiBold
	)
)

@Composable
fun WallpaperGpuTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	dynamicColor: Boolean = true,
	content: @Composable () -> Unit
) {
	val colorScheme = when {
		dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
			val context = LocalContext.current
			if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
		}

		darkTheme -> DarkColorScheme
		else -> LightColorScheme
	}

	MaterialTheme(
		colorScheme = colorScheme,
		typography = WallpaperGpuTypography,
		content = content
	)
}
