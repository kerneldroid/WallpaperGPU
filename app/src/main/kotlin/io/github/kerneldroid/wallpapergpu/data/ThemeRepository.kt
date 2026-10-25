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

package io.github.kerneldroid.wallpapergpu.data

import android.content.Context
import android.net.Uri
import io.github.kerneldroid.wallpapergpu.engine.WgpuNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Manages imported `.theme` packages stored in `context.filesDir/themes/<slug>/`.
 *
 * Strictly separated from external wallpaper packs: no third-party copyrighted wallpapers
 * are bundled with WallpaperGPU. Users import `.theme` archives via the system document picker.
 */
class ThemeRepository(private val context: Context) {

	private val themesRootDir = File(context.filesDir, "themes").apply { mkdirs() }
	private val settingsStoreFile = File(context.filesDir, "theme_scene_values.json")
	private val slugPattern = Regex("^[a-z0-9][a-z0-9._-]{0,63}$")
	private val maxArchiveEntries = 512
	private val maxExtractedBytes = 64L * 1024L * 1024L

	private val _importedThemes = MutableStateFlow<List<WallpaperThemePackage>>(emptyList())
	val importedThemes: StateFlow<List<WallpaperThemePackage>> = _importedThemes.asStateFlow()

	private val _sceneValues = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
	val sceneValues: StateFlow<Map<String, Map<String, String>>> = _sceneValues.asStateFlow()

	init {
		loadSceneValues()
		reloadInstalledThemes()
	}

	fun getThemesRootDirectory(): File = themesRootDir

	fun reloadInstalledThemes() {
		val list = mutableListOf<WallpaperThemePackage>()
		val children = themesRootDir.listFiles() ?: emptyArray()
		for (dir in children.sortedBy { it.name }) {
			if (!dir.isDirectory) continue
			val manifestFile = File(dir, "theme.json")
			if (!manifestFile.isFile) continue
			runCatching {
				val jsonStr = manifestFile.readText(Charsets.UTF_8)
				val pkg = parseManifestJson(jsonStr, dir)
				list.add(pkg)
			}
		}
		_importedThemes.value = list
	}

	/**
	 * Imports a `.theme` archive from a content [Uri] selected by the user.
	 */
	suspend fun importThemeFromUri(uri: Uri): Result<WallpaperThemePackage> = withContext(Dispatchers.IO) {
		runCatching {
			val inputStream = context.contentResolver.openInputStream(uri)
				?: throw IllegalArgumentException("Unable to open selected .theme file")
			inputStream.use { importThemeFromStream(it) }
		}
	}

	/**
	 * Extracts and validates a `.theme` ZIP stream into a temporary directory before moving it
	 * into `filesDir/themes/<slug>/`.
	 */
	fun importThemeFromStream(inputStream: InputStream): WallpaperThemePackage {
		val stagingDir = File(context.cacheDir, "theme_import_${System.currentTimeMillis()}").apply {
			deleteRecursively()
			mkdirs()
		}

		try {
			ZipInputStream(inputStream.buffered()).use { zis ->
				var entry = zis.nextEntry
				var entryCount = 0
				var extractedBytes = 0L
				while (entry != null) {
					entryCount++
					if (entryCount > maxArchiveEntries) {
						throw SecurityException("Rejected .theme archive with more than $maxArchiveEntries entries")
					}
					val cleanName = entry.name.trimStart('/', '\\')
					if (cleanName.contains("..") || cleanName.contains('\\')) {
						throw SecurityException("Rejected unsafe path in .theme archive: ${entry.name}")
					}
					val targetFile = File(stagingDir, cleanName)
					if (entry.isDirectory) {
						targetFile.mkdirs()
					} else {
						targetFile.parentFile?.mkdirs()
						FileOutputStream(targetFile).use { out ->
							extractedBytes += zis.copyTo(out)
							if (extractedBytes > maxExtractedBytes) {
								throw SecurityException("Rejected .theme archive larger than 64 MB")
							}
						}
					}
					zis.closeEntry()
					entry = zis.nextEntry
				}
			}

			val manifestFile = File(stagingDir, "theme.json")
			if (!manifestFile.isFile) {
				throw IllegalArgumentException("Invalid .theme archive: missing theme.json")
			}

			val manifestText = manifestFile.readText(Charsets.UTF_8)
			val (valid, validationError) = WgpuNativeBridge.validateThemeManifest(manifestText)
			if (!valid) {
				throw IllegalArgumentException(validationError ?: "Invalid theme.json manifest")
			}

			val previewPkg = parseManifestJson(manifestText, stagingDir)
			if (!slugPattern.matches(previewPkg.slug)) {
				throw SecurityException("Invalid theme slug '${previewPkg.slug}'")
			}
			val safeEntrypoint = previewPkg.entrypoint.replace('\\', '/')
			if (safeEntrypoint.startsWith('/') || safeEntrypoint.split('/').contains("..")) {
				throw SecurityException("Invalid theme entrypoint '${previewPkg.entrypoint}'")
			}
			val entrypointFile = File(stagingDir, safeEntrypoint)
			if (!entrypointFile.isFile) {
				throw IllegalArgumentException("Missing entrypoint '${previewPkg.entrypoint}' in .theme archive")
			}

			val finalDir = File(themesRootDir, previewPkg.slug).canonicalFile
			if (finalDir.parentFile != themesRootDir.canonicalFile) {
				throw SecurityException("Theme path escapes the themes directory")
			}
			if (finalDir.exists()) {
				finalDir.deleteRecursively()
			}
			if (!stagingDir.renameTo(finalDir)) {
				stagingDir.copyRecursively(finalDir, overwrite = true)
				stagingDir.deleteRecursively()
			}

			val installedPkg = parseManifestJson(manifestText, finalDir)
			reloadInstalledThemes()
			return installedPkg
		} finally {
			if (stagingDir.exists()) {
				stagingDir.deleteRecursively()
			}
		}
	}

	fun removeTheme(slug: String) {
		if (!slugPattern.matches(slug)) return
		val targetDir = File(themesRootDir, slug).canonicalFile
		if (targetDir.parentFile != themesRootDir.canonicalFile) return
		if (targetDir.exists()) {
			targetDir.deleteRecursively()
		}
		reloadInstalledThemes()
	}

	fun getSceneValuesForTheme(pkg: WallpaperThemePackage): Map<String, String> {
		val saved = _sceneValues.value[pkg.slug] ?: emptyMap()
		return pkg.defaultValues + saved
	}

	fun updateSceneValue(slug: String, key: String, value: String) {
		val current = _sceneValues.value.toMutableMap()
		val themeMap = (current[slug] ?: emptyMap()).toMutableMap()
		themeMap[key] = value
		current[slug] = themeMap
		_sceneValues.value = current
		saveSceneValues(current)
	}

	/**
	 * Generates a clean, open-source procedural WebGPU sample `.theme` ("Aurora Grid")
	 * on demand so users can test the MD3 Carousel and Live Wallpaper Engine immediately.
	 */
	suspend fun installBuiltInSampleTheme(): WallpaperThemePackage = withContext(Dispatchers.IO) {
		val slug = "aurora-grid-sample"
		val dir = File(themesRootDir, slug).apply {
			deleteRecursively()
			mkdirs()
		}

		val manifestJson = JSONObject().apply {
			put("format", "WallpaperGPU.Theme")
			put("schemaVersion", "1.0")
			put("id", "io.github.kerneldroid.wallpapergpu.theme.aurora_grid_sample")
			put("slug", slug)
			put("name", "Aurora Grid (Sample)")
			put("category", "Procedural")
			put(
				"description",
				"Open-source procedural WebGPU wave grid demonstrating touch ripples, parallax offsets, and Smart Static Freeze."
			)
			put("author", "kerneldroid/WallpaperGPU")
			put("license", "GPL-3.0-or-later")
			put("accent", JSONArray(listOf("#091526", "#164e63", "#38bdf8")))
			put("entrypoint", "index.html")
			put("shader", "shader.wgsl")
			put(
				"capabilities",
				JSONObject().apply {
					put("touch", true)
					put("multiTouch", true)
					put("parallaxOffset", true)
					put("deviceTilt", true)
					put("batteryLevel", false)
					put("smartStaticIdle", true)
				}
			)
			put("textures", JSONArray())
		}

		File(dir, "theme.json").writeText(manifestJson.toString(2), Charsets.UTF_8)
		File(dir, "shader.wgsl").writeText(
			"""
			// Aurora Grid Procedural WGSL Sample (GPL-3.0-or-later)
			struct Uniforms { res: vec2f, time: f32, offsetX: f32, touch: vec2f, down: f32, _pad: f32 };
			@group(0) @binding(0) var<uniform> u: Uniforms;
			@vertex fn vs(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
			  let p = vec2f(f32((i << 1u) & 2u), f32(i & 2u));
			  return vec4f(p * 2.0 - 1.0, 0.0, 1.0);
			}
			@fragment fn fs(@builtin(position) pos: vec4f) -> @location(0) vec4f {
			  let uv = pos.xy / u.res;
			  let d = distance(uv, u.touch);
			  let ripple = sin(d * 42.0 - u.time * 5.0) * exp(-d * 6.0) * u.down;
			  let wave = 0.5 + 0.5 * sin((uv.x + u.offsetX) * 6.283 + u.time + uv.y * 4.0 + ripple);
			  let col = mix(vec3f(0.035, 0.08, 0.15), vec3f(0.22, 0.74, 0.97), wave);
			  return vec4f(col, 1.0);
			}
			""".trimIndent(),
			Charsets.UTF_8
		)

		File(dir, "index.html").writeText(
			"""
			<!DOCTYPE html>
			<html lang="en" data-lw-slug="aurora-grid-sample" data-wgpu-theme="aurora-grid-sample">
			<head>
			<meta charset="utf-8">
			<meta name="viewport" content="width=device-width,initial-scale=1">
			<title>Aurora Grid (Sample)</title>
			<style>html,body,canvas{margin:0;width:100%;height:100%;display:block;background:#091526;overflow:hidden}</style>
			</head>
			<body>
			<canvas id="c"></canvas>
			<script>
			window.WallpaperGPU = window.WallpaperGPU || {
			  state: { offsetX: 0.5, pointer: { normX: 0.5, normY: 0.5, down: false }, idleFrozen: false },
			  _dispatchTouch(action, x, y) {
			    this.state.pointer.normX = x / Math.max(1, innerWidth);
			    this.state.pointer.normY = y / Math.max(1, innerHeight);
			    this.state.pointer.down = (action === 'down' || action === 0 || action === 'move' || action === 2);
			  },
			  _dispatchOffset(ox) { this.state.offsetX = ox; },
			  _dispatchIdle(frozen) { this.state.idleFrozen = !!frozen; }
			};
			const cv = document.getElementById('c');
			const ctx = cv.getContext('2d');
			let t = 0;
			function loop() {
			  requestAnimationFrame(loop);
			  if (window.WallpaperGPU.state.idleFrozen) return;
			  t += 0.025;
			  cv.width = innerWidth; cv.height = innerHeight;
			  const ox = window.WallpaperGPU.state.offsetX || 0.5;
			  const px = (window.WallpaperGPU.state.pointer.normX || 0.5) * cv.width;
			  const py = (window.WallpaperGPU.state.pointer.normY || 0.5) * cv.height;
			  const g = ctx.createLinearGradient(0, 0, cv.width, cv.height);
			  g.addColorStop(0, '#091526');
			  g.addColorStop(0.5, '#164e63');
			  g.addColorStop(1, '#38bdf8');
			  ctx.fillStyle = g;
			  ctx.fillRect(0, 0, cv.width, cv.height);
			  ctx.strokeStyle = 'rgba(255,255,255,0.22)';
			  ctx.lineWidth = 2;
			  for (let i = 0; i < 12; i++) {
			    ctx.beginPath();
			    const baseY = (cv.height / 12) * i;
			    for (let x = 0; x <= cv.width; x += 24) {
			      const d = Math.hypot(x - px, baseY - py);
			      const ripple = Math.sin(d * 0.04 - t * 3) * Math.exp(-d * 0.006) * 28;
			      const y = baseY + Math.sin(x * 0.008 + t + ox * 4 + i * 0.5) * 18 + ripple;
			      if (x === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
			    }
			    ctx.stroke();
			  }
			}
			requestAnimationFrame(loop);
			</script>
			</body>
			</html>
			""".trimIndent(),
			Charsets.UTF_8
		)

		val pkg = parseManifestJson(manifestJson.toString(), dir)
		reloadInstalledThemes()
		pkg
	}

	private fun parseManifestJson(jsonStr: String, installDir: File): WallpaperThemePackage {
		val obj = JSONObject(jsonStr)
		val slug = obj.optString("slug", installDir.name)
		val accentArr = obj.optJSONArray("accent")
		val accents = mutableListOf<String>()
		if (accentArr != null) {
			for (i in 0 until accentArr.length()) {
				accents.add(accentArr.optString(i))
			}
		}
		if (accents.isEmpty()) {
			accents.addAll(listOf("#0f172a", "#1e3a8a", "#60a5fa"))
		}

		val capObj = obj.optJSONObject("capabilities")
		val capabilities = if (capObj != null) {
			ThemeCapabilities(
				touch = capObj.optBoolean("touch", true),
				multiTouch = capObj.optBoolean("multiTouch", true),
				parallaxOffset = capObj.optBoolean("parallaxOffset", true),
				deviceTilt = capObj.optBoolean("deviceTilt", true),
				batteryLevel = capObj.optBoolean("batteryLevel", false),
				smartStaticIdle = capObj.optBoolean("smartStaticIdle", true)
			)
		} else {
			ThemeCapabilities()
		}

		val policyObj = obj.optJSONObject("smartStaticPolicy")
		val policy = if (policyObj != null) {
			SmartStaticPolicy(
				freezeOnVisibilityLost = policyObj.optBoolean("freezeOnVisibilityLost", true),
				freezeOnScreenOff = policyObj.optBoolean("freezeOnScreenOff", true),
				freezeOnProximityCovered = policyObj.optBoolean("freezeOnProximityCovered", true),
				inactivityTimeoutSeconds = policyObj.optInt("inactivityTimeoutSeconds", 30),
				captureStaticSnapshot = policyObj.optBoolean("captureStaticSnapshot", true)
			)
		} else {
			SmartStaticPolicy()
		}

		val settingsSchema = mutableListOf<ThemeSettingField>()
		val schemaArr = obj.optJSONArray("settingsSchema")
		if (schemaArr != null) {
			for (i in 0 until schemaArr.length()) {
				val sObj = schemaArr.optJSONObject(i) ?: continue
				val type = sObj.optString("type", "select")
				if (type == "action") continue
				val opts = mutableListOf<ThemeSettingOption>()
				val optArr = sObj.optJSONArray("options")
				if (optArr != null) {
					for (j in 0 until optArr.length()) {
						val o = optArr.optJSONObject(j) ?: continue
						opts.add(
							ThemeSettingOption(
								value = o.opt("value")?.toString() ?: "",
								label = o.optString("label", "")
							)
						)
					}
				}
				settingsSchema.add(
					ThemeSettingField(
						id = sObj.optString("id", ""),
						type = type,
						group = sObj.optString("group", "Scene"),
						label = sObj.optString("label", ""),
						options = opts,
						min = sObj.optDouble("min", 0.0).toFloat(),
						max = sObj.optDouble("max", 100.0).toFloat(),
						step = sObj.optDouble("step", 1.0).toFloat()
					)
				)
			}
		}

		val defaultsMap = mutableMapOf<String, String>()
		val defaultsObj = obj.optJSONObject("initialValues") ?: obj.optJSONObject("defaults")
		if (defaultsObj != null) {
			val keys = defaultsObj.keys()
			while (keys.hasNext()) {
				val k = keys.next()
				defaultsMap[k] = defaultsObj.opt(k)?.toString() ?: ""
			}
		}

		val texturesArr = obj.optJSONArray("textures")
		val textureCount = texturesArr?.length() ?: File(installDir, "assets").listFiles()?.size ?: 0

		return WallpaperThemePackage(
			id = obj.optString("id", "io.github.kerneldroid.wallpapergpu.theme.$slug"),
			slug = slug,
			name = obj.optString("name", slug),
			category = obj.optString("category", "WebGPU"),
			description = obj.optString("description", "Interactive WebGPU live wallpaper"),
			author = obj.optString("author", "WallpaperGPU Template"),
			license = obj.optString("license", "GPL-3.0-or-later"),
			accentColors = accents,
			entrypoint = obj.optString("entrypoint", "index.html"),
			shaderFile = obj.optString("shader", "shader.wgsl"),
			textureCount = textureCount,
			installDirPath = installDir.absolutePath,
			importedAtMillis = installDir.lastModified(),
			capabilities = capabilities,
			smartStaticPolicy = policy,
			settingsSchema = settingsSchema,
			defaultValues = defaultsMap
		)
	}

	private fun loadSceneValues() {
		if (!settingsStoreFile.isFile) return
		runCatching {
			val root = JSONObject(settingsStoreFile.readText(Charsets.UTF_8))
			val out = mutableMapOf<String, Map<String, String>>()
			val slugs = root.keys()
			while (slugs.hasNext()) {
				val slug = slugs.next()
				val sub = root.optJSONObject(slug) ?: continue
				val map = mutableMapOf<String, String>()
				val keys = sub.keys()
				while (keys.hasNext()) {
					val k = keys.next()
					map[k] = sub.optString(k, "")
				}
				out[slug] = map
			}
			_sceneValues.value = out
		}
	}

	private fun saveSceneValues(values: Map<String, Map<String, String>>) {
		runCatching {
			val root = JSONObject()
			for ((slug, map) in values) {
				val sub = JSONObject()
				for ((k, v) in map) {
					sub.put(k, v)
				}
				root.put(slug, sub)
			}
			settingsStoreFile.writeText(root.toString(2), Charsets.UTF_8)
		}
	}
}
