package com.riddle.camsr2d2;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

final class AppPreferences {
    private static final String PREFS = "r2d2_controller_settings";
    private static final String KEY_CONTROLLER_NAME = "controller_name";
    private static final String KEY_TILT_ENABLED = "tilt_enabled";
    private static final String KEY_BUILDER_ENABLED = "builder_enabled";
    private static final String KEY_VOICE_ENABLED = "voice_enabled";
    private static final String KEY_TILT_DEADZONE = "tilt_deadzone";
    private static final String KEY_TILT_SMOOTHING = "tilt_smoothing";
    private static final String KEY_TILT_STEERING_BIAS = "tilt_steering_bias";
    private static final String KEY_TILT_RESPONSE_DELAY = "tilt_response_delay";
    private static final String KEY_TILT_INVERT_FORWARD = "tilt_invert_forward";
    private static final String KEY_TILT_INVERT_STEERING = "tilt_invert_steering";
    private static final String KEY_VOICE_MOTION_ENABLED = "voice_motion_enabled";
    private static final String KEY_VOICE_MOTION_DURATION = "voice_motion_duration";
    private static final String KEY_BUILDER_MOTION_DURATION = "builder_motion_duration";
    private static final String KEY_BUILDER_DELAY = "builder_delay";
    private static final int MAX_NAME_LENGTH = 24;

    private final SharedPreferences preferences;

    AppPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    String controllerName() {
        String saved = preferences.getString(KEY_CONTROLLER_NAME, BuildConfig.DEFAULT_CONTROLLER_NAME);
        return normalize(saved);
    }

    void setControllerName(String value) {
        preferences.edit().putString(KEY_CONTROLLER_NAME, normalize(value)).apply();
    }

    String possessiveName() {
        String name = controllerName();
        if (name.toLowerCase(Locale.US).endsWith("s")) return name + "'";
        return name + "'s";
    }

    String controllerTitle() {
        return possessiveName() + " R2-D2 Controller";
    }

    boolean tiltEnabled() { return preferences.getBoolean(KEY_TILT_ENABLED, true); }
    void setTiltEnabled(boolean value) { putBoolean(KEY_TILT_ENABLED, value); }

    boolean builderEnabled() { return preferences.getBoolean(KEY_BUILDER_ENABLED, true); }
    void setBuilderEnabled(boolean value) { putBoolean(KEY_BUILDER_ENABLED, value); }

    boolean voiceEnabled() { return preferences.getBoolean(KEY_VOICE_ENABLED, true); }
    void setVoiceEnabled(boolean value) { putBoolean(KEY_VOICE_ENABLED, value); }

    int tiltDeadzoneDegrees() { return clamp(preferences.getInt(KEY_TILT_DEADZONE, 10), 3, 25); }
    void setTiltDeadzoneDegrees(int value) { putInt(KEY_TILT_DEADZONE, clamp(value, 3, 25)); }

    int tiltSmoothingPercent() { return clamp(preferences.getInt(KEY_TILT_SMOOTHING, 60), 0, 90); }
    void setTiltSmoothingPercent(int value) { putInt(KEY_TILT_SMOOTHING, clamp(value, 0, 90)); }

    int tiltSteeringBiasPercent() { return clamp(preferences.getInt(KEY_TILT_STEERING_BIAS, 110), 50, 180); }
    void setTiltSteeringBiasPercent(int value) { putInt(KEY_TILT_STEERING_BIAS, clamp(value, 50, 180)); }

    int tiltResponseDelayMs() { return clamp(preferences.getInt(KEY_TILT_RESPONSE_DELAY, 250), 0, 800); }
    void setTiltResponseDelayMs(int value) { putInt(KEY_TILT_RESPONSE_DELAY, clamp(value, 0, 800)); }

    boolean tiltInvertForward() { return preferences.getBoolean(KEY_TILT_INVERT_FORWARD, false); }
    void setTiltInvertForward(boolean value) { putBoolean(KEY_TILT_INVERT_FORWARD, value); }

    boolean tiltInvertSteering() { return preferences.getBoolean(KEY_TILT_INVERT_STEERING, false); }
    void setTiltInvertSteering(boolean value) { putBoolean(KEY_TILT_INVERT_STEERING, value); }

    boolean voiceMotionEnabled() { return preferences.getBoolean(KEY_VOICE_MOTION_ENABLED, true); }
    void setVoiceMotionEnabled(boolean value) { putBoolean(KEY_VOICE_MOTION_ENABLED, value); }

    int voiceMotionDurationMs() { return clamp(preferences.getInt(KEY_VOICE_MOTION_DURATION, 1500), 500, 5000); }
    void setVoiceMotionDurationMs(int value) { putInt(KEY_VOICE_MOTION_DURATION, clamp(value, 500, 5000)); }

    int builderMotionDurationMs() { return clamp(preferences.getInt(KEY_BUILDER_MOTION_DURATION, 1200), 300, 5000); }
    void setBuilderMotionDurationMs(int value) { putInt(KEY_BUILDER_MOTION_DURATION, clamp(value, 300, 5000)); }

    int builderDefaultDelayMs() { return clamp(preferences.getInt(KEY_BUILDER_DELAY, 200), 0, 2000); }
    void setBuilderDefaultDelayMs(int value) { putInt(KEY_BUILDER_DELAY, clamp(value, 0, 2000)); }

    private void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    private void putInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalize(String value) {
        String fallback = BuildConfig.DEFAULT_CONTROLLER_NAME.trim();
        if (fallback.isEmpty()) fallback = "Cam";

        String result = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (result.isEmpty()) result = fallback;
        if (result.length() > MAX_NAME_LENGTH) result = result.substring(0, MAX_NAME_LENGTH).trim();
        return result;
    }
}
