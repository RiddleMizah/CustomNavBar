package com.riddle.camsr2d2;

import org.json.JSONException;
import org.json.JSONObject;

final class RoutineStep {
    final ActionType action;
    final long delayBeforeMs;
    final long durationMs;

    RoutineStep(ActionType action, long delayBeforeMs, long durationMs) {
        this.action = action;
        this.delayBeforeMs = Math.max(0L, delayBeforeMs);
        this.durationMs = Math.max(0L, durationMs);
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("action", action.name())
                .put("delayBeforeMs", delayBeforeMs)
                .put("durationMs", durationMs);
    }

    static RoutineStep fromJson(JSONObject object) throws JSONException {
        return new RoutineStep(
                ActionType.valueOf(object.getString("action")),
                object.optLong("delayBeforeMs", 0L),
                object.optLong("durationMs", 0L)
        );
    }
}
