package com.riddle.camsr2d2;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

final class AppPreferences {
    private static final String PREFS = "r2d2_controller_settings";
    private static final String KEY_CONTROLLER_NAME = "controller_name";
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

    private String normalize(String value) {
        String fallback = BuildConfig.DEFAULT_CONTROLLER_NAME.trim();
        if (fallback.isEmpty()) fallback = "Cam";

        String result = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (result.isEmpty()) result = fallback;
        if (result.length() > MAX_NAME_LENGTH) result = result.substring(0, MAX_NAME_LENGTH).trim();
        return result;
    }
}
