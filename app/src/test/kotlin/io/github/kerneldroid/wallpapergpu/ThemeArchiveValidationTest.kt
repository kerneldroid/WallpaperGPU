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

import io.github.kerneldroid.wallpapergpu.engine.WgpuNativeBridge
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ThemeArchiveValidationTest {

	@Test
	fun validManifestPassesValidation() {
		val json = """
			{
			  "format": "WallpaperGPU.Theme",
			  "schemaVersion": "1.0",
			  "slug": "aurora-grid-sample",
			  "name": "Aurora Grid",
			  "entrypoint": "index.html"
			}
		""".trimIndent()
		val (valid, error) = WgpuNativeBridge.validateThemeManifest(json)
		assertTrue(valid)
		assertNull(error)
	}

	@Test
	fun invalidFormatFailsValidation() {
		val json = """
			{
			  "format": "UnknownFormat",
			  "slug": "bad-theme",
			  "entrypoint": "index.html"
			}
		""".trimIndent()
		val (valid, _) = WgpuNativeBridge.validateThemeManifest(json)
		assertFalse(valid)
	}
}
