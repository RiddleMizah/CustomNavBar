package com.riddle.camsr2d2;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

final class TiltDriveScreen {
    private TiltDriveScreen() {}

    static View build(MainActivity app) {
        ScrollView scroll = new ScrollView(app);
        LinearLayout page = Ui.page(app);
        scroll.addView(page);

        page.addView(Ui.text(app, "Tilt Drive", 25f, true, "#10233B"));
        page.addView(Ui.text(app,
                "Hold the tablet level, arm Tilt Drive, then gently tilt it. Returning to the deadzone stops R2-D2.",
                14f, false, "#60758D"), Ui.matchWrap(app, 0, 4, 0, 12));

        LinearLayout safety = Ui.card(app, "#FFFFFF");
        safety.setPadding(Ui.dp(app, 18), Ui.dp(app, 16), Ui.dp(app, 18), Ui.dp(app, 16));
        safety.addView(Ui.text(app, "Safety control", 19f, true, "#10233B"));
        safety.addView(Ui.text(app,
                "Tilt Drive always opens disarmed and stops automatically when you leave this screen.",
                13f, false, "#60758D"), Ui.matchWrap(app, 0, 4, 0, 10));

        Switch armed = new Switch(app);
        armed.setText("Arm Tilt Drive");
        armed.setTextSize(17f);
        armed.setTextColor(Ui.color("#10233B"));
        safety.addView(armed, new LinearLayout.LayoutParams(-1, Ui.dp(app, 54)));
        page.addView(safety, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout live = Ui.card(app, "#0A2445");
        live.setGravity(Gravity.CENTER_HORIZONTAL);
        live.setPadding(Ui.dp(app, 18), Ui.dp(app, 18), Ui.dp(app, 18), Ui.dp(app, 18));
        TextView action = Ui.text(app, "DISARMED", 30f, true, "#FFC83D");
        action.setGravity(Gravity.CENTER);
        live.addView(action, new LinearLayout.LayoutParams(-1, -2));
        TextView angles = Ui.text(app, "Forward 0.0°   •   Steering 0.0°", 17f, false, "#EAF5FF");
        angles.setGravity(Gravity.CENTER);
        live.addView(angles, Ui.matchWrap(app, 0, 10, 0, 0));
        TextView sensor = Ui.text(app, "Sensor waiting", 12f, false, "#BFD6EE");
        sensor.setGravity(Gravity.CENTER);
        live.addView(sensor, Ui.matchWrap(app, 0, 8, 0, 0));
        page.addView(live, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout tuning = Ui.card(app, "#FFFFFF");
        tuning.setPadding(Ui.dp(app, 16), Ui.dp(app, 14), Ui.dp(app, 16), Ui.dp(app, 14));
        tuning.addView(Ui.text(app, "Current Parent Mode tuning", 17f, true, "#10233B"));
        tuning.addView(Ui.text(app,
                "Deadzone: " + app.preferences().tiltDeadzoneDegrees() + "°\n" +
                        "Smoothing: " + app.preferences().tiltSmoothingPercent() + "%\n" +
                        "Steering bias: " + app.preferences().tiltSteeringBiasPercent() + "%\n" +
                        "Direction delay: " + app.preferences().tiltResponseDelayMs() + " ms",
                14f, false, "#60758D"), Ui.matchWrap(app, 0, 8, 0, 0));
        page.addView(tuning, Ui.matchWrap(app, 0, 0, 0, 12));

        Button emergency = Ui.button(app, "EMERGENCY STOP", "#D63A46", "#FFFFFF", 19f);
        emergency.setOnClickListener(v -> {
            armed.setChecked(false);
            app.disarmTilt();
            action.setText("STOPPED");
            action.setTextColor(Ui.color("#FFC83D"));
            app.emergencyStop();
        });
        page.addView(emergency, new LinearLayout.LayoutParams(-1, Ui.dp(app, 62)));

        TiltController.Listener listener = new TiltController.Listener() {
            @Override
            public void onSensorState(boolean available, String sensorName) {
                sensor.setText(available ? "Sensor: " + sensorName : sensorName);
                if (!available) {
                    armed.setChecked(false);
                    action.setText("SENSOR UNAVAILABLE");
                    action.setTextColor(Ui.color("#FF8A95"));
                }
            }

            @Override
            public void onReading(float forwardDegrees, float steeringDegrees, ActionType command) {
                angles.setText(String.format(Locale.US,
                        "Forward %.1f°   •   Steering %.1f°", forwardDegrees, steeringDegrees));
                action.setText(command == ActionType.STOP ? "DEADZONE — STOP" : command.label.toUpperCase(Locale.US));
                action.setTextColor(Ui.color(command == ActionType.STOP ? "#FFC83D" : "#7EE0A9"));
            }
        };

        armed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!app.armTilt(listener)) {
                    buttonView.setChecked(false);
                } else {
                    action.setText("LEVEL TABLET — READY");
                    action.setTextColor(Ui.color("#FFC83D"));
                }
            } else {
                app.disarmTilt();
                action.setText("DISARMED");
                action.setTextColor(Ui.color("#FFC83D"));
            }
        });

        return scroll;
    }
}
