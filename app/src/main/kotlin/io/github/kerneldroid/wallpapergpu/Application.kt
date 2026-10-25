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

package io.github.kerneldroid.wallpapergpu

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.github.kerneldroid.wallpapergpu.data.ThemeRepository

const val USER_PREFERENCES_NAME = "wallpapergpu_preferences"
private val Context.dataStore by preferencesDataStore(USER_PREFERENCES_NAME)

class Application : Application() {

	val userPreferencesDataStore: DataStore<Preferences>
		get() = this.dataStore

	lateinit var themeRepository: ThemeRepository
		private set

	override fun onCreate() {
		super.onCreate()
		themeRepository = ThemeRepository(this)
	}
}
