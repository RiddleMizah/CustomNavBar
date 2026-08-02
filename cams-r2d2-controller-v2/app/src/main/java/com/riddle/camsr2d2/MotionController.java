package com.riddle.camsr2d2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

final class MotionController implements RoutinePlayer.Executor {
    private static final long CAM_SETTLE_MS = 400L;

    private final Context context;
    private final R2D2Client client;
    private final RoutineRecorder recorder;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActionType activeMotion;
    private Runnable pendingMotor;
    private Runnable activeRoutineTimer;
    private boolean activeMotionRecorded;

    MotionController(Context context, R2D2Client client, RoutineRecorder recorder) {
        this.context = context;
        this.client = client;
        this.recorder = recorder;
    }

    void startManual(ActionType action) {
        startContinuous(action, true);
    }

    void startContinuous(ActionType action, boolean record) {
        if (!client.isReady()) {
            toast("Connect R2-D2 first.");
            return;
        }
        if (action == null || !action.motion) return;
        cancelPendingMotor();
        activeMotion = action;
        activeMotionRecorded = record;
        if (record) recorder.startMotion(action);
        sendMotionStart(action, null);
        AppLog.add((record ? "Manual motion: " : "Assisted motion: ") + action.label + ".");
    }

    void stopManual() {
        cancelPendingMotor();
        if (activeMotion != null && activeMotionRecorded) recorder.stopMotion();
        activeMotion = null;
        activeMotionRecorded = false;
        sendStop();
    }

    void performAction(ActionType action, boolean record) {
        if (!client.isReady() && action != ActionType.STOP) {
            toast("Connect R2-D2 first.");
            return;
        }
        if (record) recorder.recordAction(action);
        switch (action) {
            case STOP: emergencyStop(); break;
            case HEAD_LEFT: client.send(Protocol.head(0)); break;
            case HEAD_CENTER: client.send(Protocol.head(1)); break;
            case HEAD_RIGHT: client.send(Protocol.head(2)); break;
            case LIGHT_RED: client.send(Protocol.led(255, 0)); break;
            case LIGHT_BLUE: client.send(Protocol.led(0, 255)); break;
            case LIGHTS_OFF: client.send(Protocol.led(0, 0)); break;
            case WAKE: client.send(Protocol.audio(Protocol.AUDIO_WAKE)); break;
            case WHISTLE: client.send(Protocol.audio(Protocol.AUDIO_WHISTLE)); break;
            case ACHIEVEMENT: client.send(Protocol.audio(Protocol.AUDIO_ACHIEVEMENT)); break;
            default: break;
        }
        AppLog.add("Action: " + action.label + ".");
    }

    private void sendMotionStart(ActionType action, Runnable motorStarted) {
        byte cam;
        byte direction;
        switch (action) {
            case FORWARD:
                cam = Protocol.CAM_DRIVE; direction = Protocol.MOTOR_FORWARD; break;
            case REVERSE:
                cam = Protocol.CAM_DRIVE; direction = Protocol.MOTOR_BACKWARD; break;
            case TURN_LEFT:
                cam = Protocol.CAM_PIVOT_LEFT; direction = Protocol.MOTOR_FORWARD; break;
            case TURN_RIGHT:
                cam = Protocol.CAM_PIVOT_RIGHT; direction = Protocol.MOTOR_FORWARD; break;
            default:
                return;
        }
        client.send(Protocol.cam(cam));
        pendingMotor = () -> {
            pendingMotor = null;
            client.send(Protocol.motor(direction));
            if (motorStarted != null) motorStarted.run();
        };
        handler.postDelayed(pendingMotor, CAM_SETTLE_MS);
    }

    private void cancelPendingMotor() {
        if (pendingMotor != null) {
            handler.removeCallbacks(pendingMotor);
            pendingMotor = null;
        }
    }

    private void sendStop() {
        if (client.isReady()) {
            client.send(Protocol.motor(Protocol.MOTOR_STOP));
            client.send(Protocol.stopAll());
        }
    }

    @Override
    public void execute(RoutineStep step, Runnable complete) {
        if (step.action.motion) {
            sendMotionStart(step.action, () -> {
                activeRoutineTimer = () -> {
                    sendStop();
                    activeRoutineTimer = null;
                    complete.run();
                };
                handler.postDelayed(activeRoutineTimer, Math.max(150L, step.durationMs));
            });
        } else {
            performAction(step.action, false);
            activeRoutineTimer = () -> {
                activeRoutineTimer = null;
                complete.run();
            };
            handler.postDelayed(activeRoutineTimer, 260L);
        }
    }

    @Override
    public void emergencyStop() {
        cancelPendingMotor();
        if (activeRoutineTimer != null) {
            handler.removeCallbacks(activeRoutineTimer);
            activeRoutineTimer = null;
        }
        activeMotion = null;
        activeMotionRecorded = false;
        sendStop();
        AppLog.add("Emergency stop sent.");
    }

    private void toast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
