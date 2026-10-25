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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import io.github.kerneldroid.wallpapergpu.Application
import io.github.kerneldroid.wallpapergpu.data.UserPreferencesRepository
import io.github.kerneldroid.wallpapergpu.ui.theme.WallpaperGpuTheme

class MainActivity : ComponentActivity() {

	private lateinit var mainViewModel: MainViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		installSplashScreen()
		enableEdgeToEdge()

		val app = applicationContext as Application
		mainViewModel = ViewModelProvider(
			this,
			MainViewModelFactory(
				application = app,
				preferencesRepository = UserPreferencesRepository(app.userPreferencesDataStore),
				themeRepository = app.themeRepository
			)
		)[MainViewModel::class.java]

		handleIncomingThemeIntent(intent)

		setContent {
			val state by mainViewModel.uiState.collectAsState()
			WallpaperGpuTheme {
				MainScreen(
					mainViewModel = mainViewModel,
					uiState = state
				)
			}
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		handleIncomingThemeIntent(intent)
	}

	override fun onResume() {
		super.onResume()
		if (this::mainViewModel.isInitialized) {
			mainViewModel.onUiResumed()
		}
	}

	override fun onPause() {
		if (this::mainViewModel.isInitialized) {
			mainViewModel.onUiPaused()
		}
		super.onPause()
	}

	private fun handleIncomingThemeIntent(intent: Intent?) {
		if (intent?.action == Intent.ACTION_VIEW) {
			val dataUri = intent.data ?: return
			if (this::mainViewModel.isInitialized) {
				mainViewModel.importThemeUri(dataUri)
			}
		}
	}
}
