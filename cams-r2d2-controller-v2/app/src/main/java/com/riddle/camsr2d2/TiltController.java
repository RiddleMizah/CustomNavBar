package com.riddle.camsr2d2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.view.Surface;
import android.view.WindowManager;

final class TiltController implements SensorEventListener {
    interface Listener {
        void onSensorState(boolean available, String sensorName);
        void onReading(float forwardDegrees, float steeringDegrees, ActionType action);
    }

    private final Context context;
    private final AppPreferences preferences;
    private final Listener listener;
    private final SensorManager sensorManager;
    private final Sensor accelerometer;

    private boolean running;
    private boolean hasFiltered;
    private float filteredForward;
    private float filteredSteering;
    private ActionType currentAction = ActionType.STOP;
    private ActionType pendingAction = ActionType.STOP;
    private long pendingSince;

    TiltController(Context context, AppPreferences preferences, Listener listener) {
        this.context = context;
        this.preferences = preferences;
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    boolean start() {
        if (sensorManager == null || accelerometer == null) {
            listener.onSensorState(false, "Accelerometer unavailable");
            return false;
        }
        running = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        listener.onSensorState(running, accelerometer.getName());
        pendingSince = SystemClock.elapsedRealtime();
        return running;
    }

    void stop() {
        running = false;
        if (sensorManager != null) sensorManager.unregisterListener(this);
        currentAction = ActionType.STOP;
        pendingAction = ActionType.STOP;
        hasFiltered = false;
    }

    boolean isRunning() {
        return running;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!running || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float screenX;
        float screenY;

        int rotation = Surface.ROTATION_0;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null && wm.getDefaultDisplay() != null) {
            rotation = wm.getDefaultDisplay().getRotation();
        }

        if (rotation == Surface.ROTATION_90) {
            screenX = -y;
            screenY = x;
        } else if (rotation == Surface.ROTATION_180) {
            screenX = -x;
            screenY = -y;
        } else if (rotation == Surface.ROTATION_270) {
            screenX = y;
            screenY = -x;
        } else {
            screenX = x;
            screenY = y;
        }

        float denominator = Math.max(0.5f, Math.abs(z));
        float rawSteering = (float) Math.toDegrees(Math.atan2(screenX, denominator));
        float rawForward = (float) Math.toDegrees(Math.atan2(screenY, denominator));

        float keep = preferences.tiltSmoothingPercent() / 100f;
        if (!hasFiltered) {
            filteredForward = rawForward;
            filteredSteering = rawSteering;
            hasFiltered = true;
        } else {
            filteredForward = keep * filteredForward + (1f - keep) * rawForward;
            filteredSteering = keep * filteredSteering + (1f - keep) * rawSteering;
        }

        float forward = preferences.tiltInvertForward() ? -filteredForward : filteredForward;
        float steering = preferences.tiltInvertSteering() ? -filteredSteering : filteredSteering;
        ActionType candidate = chooseAction(forward, steering);
        long now = SystemClock.elapsedRealtime();

        if (candidate == ActionType.STOP) {
            pendingAction = ActionType.STOP;
            pendingSince = now;
            currentAction = ActionType.STOP;
        } else if (candidate != currentAction) {
            if (candidate != pendingAction) {
                pendingAction = candidate;
                pendingSince = now;
            }
            currentAction = ActionType.STOP;
            if (now - pendingSince >= preferences.tiltResponseDelayMs()) {
                currentAction = candidate;
            }
        } else {
            pendingAction = candidate;
            pendingSince = now;
        }

        listener.onReading(forward, steering, currentAction);
    }

    private ActionType chooseAction(float forward, float steering) {
        int deadzone = preferences.tiltDeadzoneDegrees();
        float forwardMagnitude = Math.abs(forward);
        float steeringMagnitude = Math.abs(steering) * preferences.tiltSteeringBiasPercent() / 100f;

        if (forwardMagnitude <= deadzone && steeringMagnitude <= deadzone) return ActionType.STOP;
        if (steeringMagnitude > forwardMagnitude) {
            return steering > 0f ? ActionType.TURN_RIGHT : ActionType.TURN_LEFT;
        }
        return forward > 0f ? ActionType.REVERSE : ActionType.FORWARD;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action required. The live values make calibration issues visible to Parent Mode.
    }
}
