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

/// Reason why the wallpaper transitioned from live WebGPU rendering into a static frame.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum IdleReason {
    Active,
    SurfaceHidden,
    ScreenOff,
    ProximityCovered,
    PowerSaveMode,
    InactivityTimeout,
}

impl IdleReason {
    pub fn as_str(&self) -> &'static str {
        match self {
            IdleReason::Active => "active",
            IdleReason::SurfaceHidden => "surface-hidden",
            IdleReason::ScreenOff => "screen-off",
            IdleReason::ProximityCovered => "proximity-covered",
            IdleReason::PowerSaveMode => "power-save-mode",
            IdleReason::InactivityTimeout => "inactivity-timeout",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum IdleState {
    LiveRendering,
    StaticFrozen(IdleReason),
}

/// Smart Static Wallpaper Controller.
///
/// Tracks whether anybody is currently looking at the wallpaper. When nobody is looking
/// (wallpaper hidden behind an app, screen off, proximity sensor covered in a pocket/face-down,
/// or no touch/launcher activity for `timeout_ms`), transitions the engine to `StaticFrozen`
/// so WebGPU halts frame submission (0% GPU usage) and displays a static snapshot.
#[derive(Debug, Clone)]
pub struct SmartIdleController {
    pub enabled: bool,
    pub visible: bool,
    pub screen_interactive: bool,
    pub proximity_covered: bool,
    pub power_save_active: bool,
    pub timeout_ms: u64,
    pub elapsed_idle_ms: u64,
}

impl Default for SmartIdleController {
    fn default() -> Self {
        Self {
            enabled: true,
            visible: true,
            screen_interactive: true,
            proximity_covered: false,
            power_save_active: false,
            timeout_ms: 30_000,
            elapsed_idle_ms: 0,
        }
    }
}

impl SmartIdleController {
    pub fn new(timeout_ms: u64) -> Self {
        Self {
            timeout_ms,
            ..Default::default()
        }
    }

    pub fn record_user_interaction(&mut self) {
        self.elapsed_idle_ms = 0;
    }

    pub fn tick(&mut self, delta_ms: u64) -> IdleState {
        if self.visible && self.screen_interactive && !self.proximity_covered {
            self.elapsed_idle_ms = self.elapsed_idle_ms.saturating_add(delta_ms);
        }
        self.evaluate()
    }

    pub fn evaluate(&self) -> IdleState {
        if !self.screen_interactive {
            return IdleState::StaticFrozen(IdleReason::ScreenOff);
        }
        if !self.visible {
            return IdleState::StaticFrozen(IdleReason::SurfaceHidden);
        }
        if !self.enabled {
            return IdleState::LiveRendering;
        }
        if self.proximity_covered {
            return IdleState::StaticFrozen(IdleReason::ProximityCovered);
        }
        if self.power_save_active {
            return IdleState::StaticFrozen(IdleReason::PowerSaveMode);
        }
        if self.timeout_ms > 0 && self.elapsed_idle_ms >= self.timeout_ms {
            return IdleState::StaticFrozen(IdleReason::InactivityTimeout);
        }
        IdleState::LiveRendering
    }
}
