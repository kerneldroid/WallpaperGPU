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

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.kerneldroid.wallpapergpu.R
import io.github.kerneldroid.wallpapergpu.data.WallpaperThemePackage
import io.github.kerneldroid.wallpapergpu.services.WallpaperGpuService
import io.github.kerneldroid.wallpapergpu.ui.components.ApplyWallpaperSheet
import io.github.kerneldroid.wallpapergpu.ui.components.ThreeDotMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
	mainViewModel: MainViewModel,
	uiState: MainUiState
) {
	val context = LocalContext.current
	val snackbarHostState = remember { SnackbarHostState() }
	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

	val themeFilePicker = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenDocument()
	) { uri ->
		if (uri != null) {
			mainViewModel.importThemeUri(uri)
		}
	}

	LaunchedEffect(uiState.statusMessage) {
		val msg = uiState.statusMessage ?: return@LaunchedEffect
		snackbarHostState.showSnackbar(msg)
		mainViewModel.clearStatusMessage()
	}

	val githubRepoUrl = stringResource(id = R.string.github_repo_url)
	val adaptiveThemeUrl = stringResource(id = R.string.adaptive_theme_repo_url)

	Scaffold(
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(scrollBehavior.nestedScrollConnection),
		snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
		topBar = {
			LargeTopAppBar(
				title = {
					Text(
						text = stringResource(id = R.string.app_name),
						style = MaterialTheme.typography.headlineMedium
					)
				},
				actions = {
					ThreeDotMenu(
						onOpenGitHubRepo = { openExternalUrl(context, githubRepoUrl) },
						onOpenAdaptiveThemeCredits = { openExternalUrl(context, adaptiveThemeUrl) },
						onLoadSampleTheme = { mainViewModel.installBuiltInSampleTheme() }
					)
				},
				scrollBehavior = scrollBehavior
			)
		}
	) { innerPadding ->
		MainScreenContent(
			uiState = uiState,
			onEngineEnabledChange = mainViewModel::setEngineEnabled,
			onImportThemeClick = { themeFilePicker.launch(arrayOf("*/*")) },
			onLoadSampleThemeClick = mainViewModel::installBuiltInSampleTheme,
			onSelectCarouselTheme = mainViewModel::openApplySheet,
			onSmartStaticFreezeChange = mainViewModel::setSmartStaticFreezeEnabled,
			onProximityPocketFreezeChange = mainViewModel::setProximityPocketFreezeEnabled,
			onIdleTimeoutChange = mainViewModel::setIdleTimeoutSeconds,
			onTouchBridgeChange = mainViewModel::setTouchBridgeEnabled,
			onParallaxBridgeChange = mainViewModel::setParallaxBridgeEnabled,
			onSensorTiltBridgeChange = mainViewModel::setSensorTiltBridgeEnabled,
			onTargetFpsChange = mainViewModel::setTargetFps,
			onRenderScaleChange = mainViewModel::setRenderScalePercent,
			onOpenGitHubClick = { openExternalUrl(context, githubRepoUrl) },
			onDismissGitHubPrompt = mainViewModel::dismissGitHubStarPrompt,
			onOpenAdaptiveThemeCredits = { openExternalUrl(context, adaptiveThemeUrl) },
			modifier = Modifier
				.padding(innerPadding)
				.padding(horizontal = 16.dp)
				.verticalScroll(rememberScrollState())
		)

		val selectedPkg: WallpaperThemePackage? = uiState.selectedThemeForApply
		if (selectedPkg != null) {
			ApplyWallpaperSheet(
				pkg = selectedPkg,
				sceneValues = uiState.selectedThemeSceneValues,
				onSceneValueChange = { key, value ->
					mainViewModel.updateSceneSetting(selectedPkg.slug, key, value)
				},
				onApplyAsLiveWallpaper = { pkg ->
					mainViewModel.activateTheme(pkg)
					launchLiveWallpaperPicker(context)
				},
				onRemoveTheme = { pkg ->
					mainViewModel.removeTheme(pkg)
				},
				onDismiss = mainViewModel::dismissApplySheet
			)
		}
	}
}

private fun launchLiveWallpaperPicker(context: Context) {
	val component = ComponentName(context, WallpaperGpuService::class.java)
	val changeIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
		putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
		addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	}
	runCatching {
		context.startActivity(changeIntent)
	}.onFailure {
		val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		runCatching { context.startActivity(chooserIntent) }
	}
}

private fun openExternalUrl(context: Context, url: String) {
	runCatching {
		val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		context.startActivity(intent)
	}
}
