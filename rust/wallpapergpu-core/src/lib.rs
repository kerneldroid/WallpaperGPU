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

pub mod bridge;
pub mod idle_controller;
pub mod renderer;
pub mod theme_archive;

pub use idle_controller::{IdleReason, IdleState, SmartIdleController};
pub use renderer::{TouchAction, WallpaperUniforms, WgpuFrameBridge};
pub use theme_archive::{ThemeManifestSummary, ThemeValidationResult, validate_theme_manifest_json};
