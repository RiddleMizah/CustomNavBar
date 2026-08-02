package com.riddle.camsr2d2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class Routine {
    final String id;
    String name;
    final boolean preset;
    final long createdAt;
    final List<RoutineStep> steps;

    Routine(String id, String name, boolean preset, long createdAt, List<RoutineStep> steps) {
        this.id = id;
        this.name = name;
        this.preset = preset;
        this.createdAt = createdAt;
        this.steps = new ArrayList<>(steps);
    }

    static Routine custom(String name, List<RoutineStep> steps) {
        return new Routine(UUID.randomUUID().toString(), name, false, System.currentTimeMillis(), steps);
    }

    static Routine preset(String id, String name, List<RoutineStep> steps) {
        return new Routine(id, name, true, 0L, steps);
    }

    List<RoutineStep> readOnlySteps() {
        return Collections.unmodifiableList(steps);
    }

    JSONObject toJson() throws JSONException {
        JSONArray array = new JSONArray();
        for (RoutineStep step : steps) array.put(step.toJson());
        return new JSONObject()
                .put("id", id)
                .put("name", name)
                .put("preset", preset)
                .put("createdAt", createdAt)
                .put("steps", array);
    }

    static Routine fromJson(JSONObject object) throws JSONException {
        JSONArray array = object.getJSONArray("steps");
        List<RoutineStep> steps = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            steps.add(RoutineStep.fromJson(array.getJSONObject(i)));
        }
        return new Routine(
                object.getString("id"),
                object.getString("name"),
                object.optBoolean("preset", false),
                object.optLong("createdAt", 0L),
                steps
        );
    }
}
