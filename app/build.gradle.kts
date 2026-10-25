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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.compose.compiler)
}

android {
	namespace = "io.github.kerneldroid.wallpapergpu"
	compileSdk = 36

	defaultConfig {
		applicationId = "io.github.kerneldroid.wallpapergpu"
		minSdk = 29
		targetSdk = 36
		versionCode = 101
		versionName = "1.0.1"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}

		ndk {
			abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
		debug {
			isMinifyEnabled = false
			applicationIdSuffix = ".debug"
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	buildFeatures {
		compose = true
		buildConfig = true
	}

	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}

	testOptions {
		unitTests {
			isIncludeAndroidResources = true
		}
	}
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_17)
	}
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)
	implementation(libs.androidx.datastore.core)
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.core.splashscreen)
	implementation(libs.androidx.compose.material.icons.extended)
	implementation(libs.androidx.webkit)

	testImplementation(libs.junit)
	testImplementation("org.robolectric:robolectric:4.16.1")
	debugImplementation(libs.androidx.ui.tooling)
}
