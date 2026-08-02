package com.riddle.camsr2d2;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

final class RoutineStore {
    private static final String PREFS = "cams_r2d2_routines";
    private static final String KEY_ROUTINES = "custom_routines";

    private final SharedPreferences preferences;

    RoutineStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    List<Routine> load() {
        List<Routine> routines = new ArrayList<>();
        String raw = preferences.getString(KEY_ROUTINES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                routines.add(Routine.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception error) {
            AppLog.add("Routine database could not be read: " + error.getMessage());
        }
        return routines;
    }

    void save(List<Routine> routines) {
        JSONArray array = new JSONArray();
        try {
            for (Routine routine : routines) array.put(routine.toJson());
            preferences.edit().putString(KEY_ROUTINES, array.toString()).apply();
        } catch (Exception error) {
            AppLog.add("Routine database could not be saved: " + error.getMessage());
        }
    }

    void clear() {
        preferences.edit().remove(KEY_ROUTINES).apply();
    }
}
