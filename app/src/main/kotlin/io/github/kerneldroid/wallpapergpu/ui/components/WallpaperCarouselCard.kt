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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kerneldroid.wallpapergpu.R
import io.github.kerneldroid.wallpapergpu.data.WallpaperThemePackage

/**
 * Material Design 3 Carousel section (`HorizontalMultiBrowseCarousel`) displaying all imported
 * `.theme` WebGPU wallpapers.
 *
 * Tapping any wallpaper item in the carousel opens the "Apply as Wallpaper" sheet (`onSelectTheme`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperCarouselSection(
	themes: List<WallpaperThemePackage>,
	activeThemeSlug: String,
	onImportThemeClick: () -> Unit,
	onLoadSampleThemeClick: () -> Unit,
	onSelectTheme: (WallpaperThemePackage) -> Unit,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		shape = RoundedCornerShape(24.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainer
		)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = stringResource(id = R.string.section_carousel_title),
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
					Text(
						text = stringResource(id = R.string.section_carousel_subtitle),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(top = 2.dp)
					)
				}

				FilledTonalButton(
					onClick = onImportThemeClick,
					contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
				) {
					Icon(
						imageVector = Icons.Outlined.AddPhotoAlternate,
						contentDescription = null,
						modifier = Modifier.size(18.dp)
					)
					Text(
						text = "Import .theme",
						style = MaterialTheme.typography.labelLarge,
						modifier = Modifier.padding(start = 6.dp)
					)
				}
			}

			if (themes.isEmpty()) {
				EmptyCarouselPlaceholder(
					onImportClick = onImportThemeClick,
					onLoadSampleClick = onLoadSampleThemeClick
				)
			} else {
				val carouselState = rememberCarouselState { themes.size }

				HorizontalMultiBrowseCarousel(
					state = carouselState,
					preferredItemWidth = 228.dp,
					itemSpacing = 12.dp,
					contentPadding = PaddingValues(horizontal = 2.dp),
					modifier = Modifier
						.fillMaxWidth()
						.height(216.dp)
				) { index ->
					val pkg = themes[index]
					val isActive = pkg.slug == activeThemeSlug
					CarouselWallpaperItem(
						pkg = pkg,
						isActive = isActive,
						onClick = { onSelectTheme(pkg) },
						modifier = Modifier
							.fillMaxSize()
							.maskClip(MaterialTheme.shapes.extraLarge)
					)
				}
			}
		}
	}
}

@Composable
private fun CarouselWallpaperItem(
	pkg: WallpaperThemePackage,
	isActive: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	val gradientColors = pkg.accentColors.mapNotNull { hex ->
		runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
	}.ifEmpty {
		listOf(Color(0xFF0A1936), Color(0xFF1D5AA8), Color(0xFF9BC4FF))
	}

	Box(
		modifier = modifier
			.clip(RoundedCornerShape(24.dp))
			.background(Brush.linearGradient(gradientColors))
			.clickable(onClick = onClick)
			.padding(16.dp)
	) {
		// Top row: Category chip + Active badge
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.TopStart),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Surface(
				shape = RoundedCornerShape(10.dp),
				color = Color.Black.copy(alpha = 0.45f)
			) {
				Text(
					text = pkg.category,
					style = MaterialTheme.typography.labelSmall,
					color = Color.White,
					modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
				)
			}

			if (isActive) {
				Surface(
					shape = RoundedCornerShape(10.dp),
					color = Color(0xFF10B981).copy(alpha = 0.9f)
				) {
					Row(
						modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(4.dp)
					) {
						Icon(
							imageVector = Icons.Outlined.CheckCircle,
							contentDescription = null,
							tint = Color.White,
							modifier = Modifier.size(12.dp)
						)
						Text(
							text = stringResource(id = R.string.badge_active_wallpaper),
							style = MaterialTheme.typography.labelSmall,
							fontWeight = FontWeight.Bold,
							color = Color.White
						)
					}
				}
			}
		}

		// Bottom overlay: Title, blurb, and "Apply as Wallpaper" action hint
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomStart),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			Text(
				text = pkg.name,
				style = MaterialTheme.typography.titleMedium,
				color = Color.White,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			Text(
				text = pkg.description,
				style = MaterialTheme.typography.bodySmall,
				color = Color.White.copy(alpha = 0.82f),
				maxLines = 2,
				overflow = TextOverflow.Ellipsis
			)
			Spacer(modifier = Modifier.height(4.dp))
			Surface(
				shape = RoundedCornerShape(50),
				color = Color.White.copy(alpha = 0.22f)
			) {
				Row(
					modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(6.dp)
				) {
					Icon(
						imageVector = Icons.Outlined.Wallpaper,
						contentDescription = null,
						tint = Color.White,
						modifier = Modifier.size(14.dp)
					)
					Text(
						text = stringResource(id = R.string.action_apply_as_wallpaper),
						style = MaterialTheme.typography.labelMedium,
						color = Color.White
					)
				}
			}
		}
	}
}

@Composable
private fun EmptyCarouselPlaceholder(
	onImportClick: () -> Unit,
	onLoadSampleClick: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(20.dp))
			.background(MaterialTheme.colorScheme.surfaceContainerHigh)
			.padding(20.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		Icon(
			imageVector = Icons.Outlined.TouchApp,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary,
			modifier = Modifier.size(32.dp)
		)
		Text(
			text = stringResource(id = R.string.carousel_empty_title),
			style = MaterialTheme.typography.titleSmall,
			color = MaterialTheme.colorScheme.onSurface
		)
		Text(
			text = stringResource(id = R.string.carousel_empty_body),
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(8.dp),
			modifier = Modifier.fillMaxWidth()
		) {
			Button(
				onClick = onImportClick,
				modifier = Modifier.fillMaxWidth()
			) {
				Icon(
					imageVector = Icons.Outlined.AddPhotoAlternate,
					contentDescription = null,
					modifier = Modifier.size(18.dp)
				)
				Text(
					text = stringResource(id = R.string.action_import_theme),
					modifier = Modifier.padding(start = 6.dp)
				)
			}
			FilledTonalButton(
				onClick = onLoadSampleClick,
				modifier = Modifier.fillMaxWidth()
			) {
				Icon(
					imageVector = Icons.Outlined.AutoAwesome,
					contentDescription = null,
					modifier = Modifier.size(18.dp)
				)
				Text(
					text = "Demo .theme",
					modifier = Modifier.padding(start = 6.dp)
				)
			}
		}
	}
}
