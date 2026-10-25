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

use bytemuck::{Pod, Zeroable};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(u32)]
pub enum TouchAction {
    Down = 0,
    Up = 1,
    Move = 2,
    Cancel = 3,
}

impl From<i32> for TouchAction {
    fn from(value: i32) -> Self {
        match value {
            0 => TouchAction::Down,
            1 => TouchAction::Up,
            2 => TouchAction::Move,
            _ => TouchAction::Cancel,
        }
    }
}

/// 64-byte aligned WebGPU Uniform Buffer layout shared between the Rust native bridge
/// and WGSL shaders in WallpaperGPU templates.
#[repr(C)]
#[derive(Debug, Clone, Copy, Pod, Zeroable)]
pub struct WallpaperUniforms {
    pub resolution: [f32; 2],
    pub time_seconds: f32,
    pub delta_seconds: f32,
    pub pointer_xy: [f32; 2],
    pub pointer_velocity: [f32; 2],
    pub launcher_offset: [f32; 2],
    pub tilt_gamma_beta: [f32; 2],
    pub pointer_down: f32,
    pub pointer_pressure: f32,
    pub battery_level: f32,
    pub static_frozen: f32,
}

impl Default for WallpaperUniforms {
    fn default() -> Self {
        Self {
            resolution: [1080.0, 2400.0],
            time_seconds: 0.0,
            delta_seconds: 0.016,
            pointer_xy: [-1.0e5, -1.0e5],
            pointer_velocity: [0.0, 0.0],
            launcher_offset: [0.5, 0.5],
            tilt_gamma_beta: [0.0, 0.0],
            pointer_down: 0.0,
            pointer_pressure: 0.0,
            battery_level: 0.80,
            static_frozen: 0.0,
        }
    }
}

pub struct WgpuFrameBridge {
    pub uniforms: WallpaperUniforms,
    last_pointer_time_ms: f32,
}

impl Default for WgpuFrameBridge {
    fn default() -> Self {
        Self {
            uniforms: WallpaperUniforms::default(),
            last_pointer_time_ms: 0.0,
        }
    }
}

impl WgpuFrameBridge {
    pub fn set_viewport(&mut self, width: u32, height: u32) {
        self.uniforms.resolution = [width.max(1) as f32, height.max(1) as f32];
    }

    pub fn update_touch(
        &mut self,
        action: TouchAction,
        x: f32,
        y: f32,
        pressure: f32,
        timestamp_ms: f32,
    ) {
        let dt = ((timestamp_ms - self.last_pointer_time_ms) * 0.001).max(0.008);
        if self.uniforms.pointer_down > 0.5 {
            self.uniforms.pointer_velocity = [
                (x - self.uniforms.pointer_xy[0]) / dt,
                (y - self.uniforms.pointer_xy[1]) / dt,
            ];
        } else {
            self.uniforms.pointer_velocity = [0.0, 0.0];
        }
        self.last_pointer_time_ms = timestamp_ms;
        self.uniforms.pointer_xy = [x, y];
        self.uniforms.pointer_pressure = pressure;
        self.uniforms.pointer_down = match action {
            TouchAction::Down | TouchAction::Move => 1.0,
            TouchAction::Up | TouchAction::Cancel => 0.0,
        };
    }

    pub fn update_offset(&mut self, offset_x: f32, offset_y: f32) {
        self.uniforms.launcher_offset = [offset_x.clamp(0.0, 1.0), offset_y.clamp(0.0, 1.0)];
    }

    pub fn update_tilt(&mut self, gamma: f32, beta: f32) {
        self.uniforms.tilt_gamma_beta = [gamma.clamp(-90.0, 90.0), beta.clamp(-180.0, 180.0)];
    }

    pub fn update_battery(&mut self, level_percent: f32) {
        self.uniforms.battery_level = (level_percent * 0.01).clamp(0.0, 1.0);
    }

    pub fn set_static_frozen(&mut self, frozen: bool) {
        self.uniforms.static_frozen = if frozen { 1.0 } else { 0.0 };
    }
}
