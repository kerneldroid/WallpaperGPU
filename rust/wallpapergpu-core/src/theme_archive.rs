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

use serde::{Deserialize, Serialize};

/// Standardized WallpaperGPU `.theme` manifest summary parsed and validated by the Rust core.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ThemeManifestSummary {
    pub format: String,
    pub schema_version: String,
    pub slug: String,
    pub name: String,
    pub category: String,
    pub description: String,
    pub entrypoint: String,
    pub shader: Option<String>,
    #[serde(default)]
    pub accent: Vec<String>,
    #[serde(default)]
    pub capabilities: ThemeCapabilities,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ThemeCapabilities {
    #[serde(default = "default_true")]
    pub touch: bool,
    #[serde(default = "default_true")]
    pub multi_touch: bool,
    #[serde(default = "default_true")]
    pub parallax_offset: bool,
    #[serde(default = "default_true")]
    pub device_tilt: bool,
    #[serde(default)]
    pub battery_level: bool,
    #[serde(default = "default_true")]
    pub smart_static_idle: bool,
}

fn default_true() -> bool {
    true
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ThemeValidationResult {
    pub valid: bool,
    pub slug: String,
    pub name: String,
    pub error: Option<String>,
}

/// Validates a `theme.json` manifest string against the WallpaperGPU v1.0 specification.
pub fn validate_theme_manifest_json(json_str: &str) -> ThemeValidationResult {
    match serde_json::from_str::<ThemeManifestSummary>(json_str) {
        Ok(manifest) => {
            if manifest.format != "WallpaperGPU.Theme" {
                return ThemeValidationResult {
                    valid: false,
                    slug: manifest.slug,
                    name: manifest.name,
                    error: Some(format!(
                        "Unsupported theme format '{}'; expected 'WallpaperGPU.Theme'",
                        manifest.format
                    )),
                };
            }
            if manifest.slug.trim().is_empty() || manifest.entrypoint.trim().is_empty() {
                return ThemeValidationResult {
                    valid: false,
                    slug: manifest.slug,
                    name: manifest.name,
                    error: Some("Missing required 'slug' or 'entrypoint' in theme.json".to_string()),
                };
            }
            ThemeValidationResult {
                valid: true,
                slug: manifest.slug,
                name: manifest.name,
                error: None,
            }
        }
        Err(err) => ThemeValidationResult {
            valid: false,
            slug: String::new(),
            name: String::new(),
            error: Some(format!("Invalid theme.json schema: {err}")),
        },
    }
}
