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

use std::sync::Mutex;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jfloat, jint, jlong, jstring, JNI_FALSE, JNI_TRUE};

use crate::idle_controller::{IdleState, SmartIdleController};
use crate::renderer::{TouchAction, WgpuFrameBridge};
use crate::theme_archive::validate_theme_manifest_json;

pub struct NativeEngineContext {
    pub frame_bridge: WgpuFrameBridge,
    pub idle_controller: SmartIdleController,
}

impl Default for NativeEngineContext {
    fn default() -> Self {
        Self {
            frame_bridge: WgpuFrameBridge::default(),
            idle_controller: SmartIdleController::default(),
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_kerneldroid_wallpapergpu_engine_WgpuNativeBridge_nativeCreateEngine(
    _env: JNIEnv,
    _class: JClass,
    idle_timeout_ms: jlong,
) -> jlong {
    let mut ctx = NativeEngineContext::default();
    ctx.idle_controller.timeout_ms = idle_timeout_ms.max(0) as u64;
    let boxed = Box::new(Mutex::new(ctx));
    Box::into_raw(boxed) as jlong
}

#[no_mangle]
pub extern "system" fn Java_io_github_kerneldroid_wallpapergpu_engine_WgpuNativeBridge_nativeDestroyEngine(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        unsafe {
            let _ = Box::from_raw(handle as *mut Mutex<NativeEngineContext>);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_kerneldroid_wallpapergpu_engine_WgpuNativeBridge_nativeOnTouch(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    action: jint,
    x: jfloat,
    y: jfloat,
    pressure: jfloat,
    timestamp_ms: jfloat,
) {
    if handle == 0 {
        return;
    }
    let mutex = unsafe { &*(handle as *const Mutex<NativeEngineContext>) };
    if let Ok(mut ctx) = mutex.lock() {
        ctx.idle_controller.record_user_interaction();
        ctx.frame_bridge
            .update_touch(TouchAction::from(action), x, y, pressure, timestamp_ms);
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_kerneldroid_wallpapergpu_engine_WgpuNativeBridge_nativeOnOffsetsChanged(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    x_offset: jfloat,
    y_offset: jfloat,
) {
    if handle == 0 {
        return;
    }
    let mutex = unsafe { &*(handle as *const Mutex<NativeEngineContext>) };
    if let Ok(mut ctx) = mutex.lock() {
        ctx.idle_controller.record_user_interaction();
        ctx.frame_bridge.update_offset(x_offset, y_offset);
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_kerneldroid_wallpapergpu_engine_WgpuNativeBridge_nativeEvaluateSmartIdle(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    visible: jboolean,
    screen_interactive: jboolean,
    proximity_covered: jboolean,
    power_save_active: jboolean,
    delta_ms: jlong,
) -> jboolean {
    if handle == 0 {
        return JNI_FALSE;
    }
    let mutex = unsafe { &*(handle as *const Mutex<NativeEngineContext>) };
    if let Ok(mut ctx) = mutex.lock() {
        ctx.idle_controller.visible = visible == JNI_TRUE;
        ctx.idle_controller.screen_interactive = screen_interactive == JNI_TRUE;
        ctx.idle_controller.proximity_covered = proximity_covered == JNI_TRUE;
        ctx.idle_controller.power_save_active = power_save_active == JNI_TRUE;
        let state = ctx.idle_controller.tick(delta_ms.max(0) as u64);
        let frozen = matches!(state, IdleState::StaticFrozen(_));
        ctx.frame_bridge.set_static_frozen(frozen);
        return if frozen { JNI_TRUE } else { JNI_FALSE };
    }
    JNI_FALSE
}

#[no_mangle]
pub extern "system" fn Java_io_github_kerneldroid_wallpapergpu_engine_WgpuNativeBridge_nativeValidateThemeManifest(
    mut env: JNIEnv,
    _class: JClass,
    manifest_json: JString,
) -> jstring {
    let raw: String = match env.get_string(&manifest_json) {
        Ok(s) => s.into(),
        Err(_) => {
            return env
                .new_string(r#"{"valid":false,"slug":"","name":"","error":"Invalid JNI string"}"#)
                .unwrap()
                .into_raw()
        }
    };
    let result = validate_theme_manifest_json(&raw);
    let out_json = serde_json::to_string(&result).unwrap_or_else(|_| {
        r#"{"valid":false,"slug":"","name":"","error":"Serialization error"}"#.to_string()
    });
    env.new_string(out_json).unwrap().into_raw()
}
