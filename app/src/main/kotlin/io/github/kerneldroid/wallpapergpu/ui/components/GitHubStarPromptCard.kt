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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kerneldroid.wallpapergpu.R

@Composable
fun GitHubStarPromptCard(
	onStarClick: () -> Unit,
	onDismissClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		shape = RoundedCornerShape(24.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.secondaryContainer
		)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				Icon(
					imageVector = Icons.Outlined.StarBorder,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onSecondaryContainer
				)
				Text(
					text = stringResource(id = R.string.github_star_prompt_title),
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.onSecondaryContainer
				)
			}

			Text(
				text = stringResource(id = R.string.github_star_prompt_body),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSecondaryContainer
			)

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
				verticalAlignment = Alignment.CenterVertically
			) {
				TextButton(onClick = onDismissClick) {
					Text(text = stringResource(id = R.string.github_star_prompt_dismiss))
				}
				Button(onClick = onStarClick) {
					Text(text = stringResource(id = R.string.github_star_prompt_action))
				}
			}
		}
	}
}
