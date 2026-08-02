package com.riddle.camsr2d2;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

final class RoutineRecorder {
    private final List<RoutineStep> steps = new ArrayList<>();
    private boolean recording;
    private long lastEventAt;
    private ActionType pendingMotion;
    private long pendingMotionStartedAt;
    private long pendingMotionDelay;

    void start() {
        steps.clear();
        recording = true;
        lastEventAt = SystemClock.elapsedRealtime();
        pendingMotion = null;
    }

    boolean isRecording() {
        return recording;
    }

    int stepCount() {
        return steps.size() + (pendingMotion == null ? 0 : 1);
    }

    void recordAction(ActionType action) {
        if (!recording || action.motion) return;
        long now = SystemClock.elapsedRealtime();
        steps.add(new RoutineStep(action, now - lastEventAt, 0L));
        lastEventAt = now;
    }

    void startMotion(ActionType action) {
        if (!recording || !action.motion) return;
        if (pendingMotion != null) stopMotion();
        long now = SystemClock.elapsedRealtime();
        pendingMotion = action;
        pendingMotionStartedAt = now;
        pendingMotionDelay = now - lastEventAt;
    }

    void stopMotion() {
        if (!recording || pendingMotion == null) return;
        long now = SystemClock.elapsedRealtime();
        long duration = Math.max(150L, now - pendingMotionStartedAt);
        steps.add(new RoutineStep(pendingMotion, pendingMotionDelay, duration));
        pendingMotion = null;
        lastEventAt = now;
    }

    List<RoutineStep> finish() {
        if (pendingMotion != null) stopMotion();
        recording = false;
        return new ArrayList<>(steps);
    }

    void cancel() {
        recording = false;
        pendingMotion = null;
        steps.clear();
    }
}
