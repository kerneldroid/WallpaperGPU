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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kerneldroid.wallpapergpu.R

@Composable
fun ThreeDotMenu(
	onOpenGitHubRepo: () -> Unit,
	onOpenAdaptiveThemeCredits: () -> Unit,
	onLoadSampleTheme: () -> Unit
) {
	var expanded by remember { mutableStateOf(false) }

	Box {
		IconButton(onClick = { expanded = true }) {
			Icon(
				imageVector = Icons.Default.MoreVert,
				contentDescription = stringResource(id = R.string.more_options),
				tint = MaterialTheme.colorScheme.onSurface
			)
		}

		MaterialTheme(
			shapes = MaterialTheme.shapes.copy(
				extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
			)
		) {
			DropdownMenu(
				expanded = expanded,
				onDismissRequest = { expanded = false },
				modifier = Modifier.defaultMinSize(minWidth = 220.dp)
			) {
				DropdownMenuItem(
					text = { Text(text = stringResource(id = R.string.menu_import_sample_theme)) },
					leadingIcon = {
						Icon(
							imageVector = Icons.Outlined.AutoAwesome,
							contentDescription = null
						)
					},
					onClick = {
						expanded = false
						onLoadSampleTheme()
					}
				)
				DropdownMenuItem(
					text = { Text(text = stringResource(id = R.string.menu_github_repo)) },
					leadingIcon = {
						Icon(
							imageVector = Icons.Outlined.Code,
							contentDescription = null
						)
					},
					onClick = {
						expanded = false
						onOpenGitHubRepo()
					}
				)
				DropdownMenuItem(
					text = { Text(text = stringResource(id = R.string.menu_adaptive_theme_credits)) },
					leadingIcon = {
						Icon(
							imageVector = Icons.Outlined.FavoriteBorder,
							contentDescription = null
						)
					},
					onClick = {
						expanded = false
						onOpenAdaptiveThemeCredits()
					}
				)
			}
		}
	}
}
