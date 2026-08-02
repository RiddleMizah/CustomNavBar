package com.riddle.camsr2d2;

import android.content.Intent;
import android.graphics.Typeface;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

final class ParentScreen {
    private ParentScreen() {}

    static View build(MainActivity app) {
        AppPreferences prefs = app.preferences();
        ScrollView scroll = new ScrollView(app);
        LinearLayout page = Ui.page(app);
        scroll.addView(page);

        page.addView(Ui.text(app, "Parent Mode", 25f, true, "#10233B"));
        page.addView(Ui.text(app,
                "Personalization, feature visibility, safety tuning, connection tools, diagnostics, and reset options.",
                14f, false, "#60758D"), Ui.matchWrap(app, 0, 4, 0, 12));

        LinearLayout status = Ui.card(app, "#FFFFFF");
        status.setPadding(Ui.dp(app, 16), Ui.dp(app, 14), Ui.dp(app, 16), Ui.dp(app, 14));
        status.addView(Ui.text(app, app.technicalSummary(), 13f, false, "#10233B"));
        page.addView(status, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout connectionTools = new LinearLayout(app);
        connectionTools.setOrientation(LinearLayout.HORIZONTAL);
        connectionTools.addView(tool(app, "Change Name", app::editControllerName), Ui.weightedButton(app));
        connectionTools.addView(tool(app, "Reconnect", app::reconnect), Ui.weightedButton(app));
        connectionTools.addView(dangerTool(app, "Disconnect", app::disconnectR2D2), Ui.weightedButton(app));
        connectionTools.addView(dangerTool(app, "Emergency Stop", app::emergencyStop), Ui.weightedButton(app));
        page.addView(connectionTools, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout visibility = settingsCard(app, "Feature visibility",
                "Hide experimental controls from Cam without removing them from the app.");
        visibility.addView(settingsSwitch(app, "Show Tilt Drive", prefs.tiltEnabled(), prefs::setTiltEnabled));
        visibility.addView(settingsSwitch(app, "Show Visual Routine Builder", prefs.builderEnabled(), prefs::setBuilderEnabled));
        visibility.addView(settingsSwitch(app, "Show Voice Commands", prefs.voiceEnabled(), prefs::setVoiceEnabled));
        Button refresh = Ui.button(app, "Apply visibility changes", "#1677D2", "#FFFFFF", 14f);
        refresh.setOnClickListener(v -> app.refreshInterface());
        visibility.addView(refresh, Ui.matchWrap(app, 0, 10, 0, 0));
        page.addView(visibility, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout tilt = settingsCard(app, "Tilt Drive tuning",
                "Deadzone prevents drift. Smoothing reduces jitter. Direction delay prevents fast accidental direction changes.");
        tilt.addView(slider(app, "Deadzone", 3, 25, prefs.tiltDeadzoneDegrees(), "°",
                prefs::setTiltDeadzoneDegrees));
        tilt.addView(slider(app, "Smoothing", 0, 90, prefs.tiltSmoothingPercent(), "%",
                prefs::setTiltSmoothingPercent));
        tilt.addView(slider(app, "Steering bias", 50, 180, prefs.tiltSteeringBiasPercent(), "%",
                prefs::setTiltSteeringBiasPercent));
        tilt.addView(slider(app, "Direction-change delay", 0, 800, prefs.tiltResponseDelayMs(), " ms",
                prefs::setTiltResponseDelayMs));
        tilt.addView(settingsSwitch(app, "Invert forward and reverse", prefs.tiltInvertForward(), prefs::setTiltInvertForward));
        tilt.addView(settingsSwitch(app, "Invert left and right", prefs.tiltInvertSteering(), prefs::setTiltInvertSteering));
        page.addView(tilt, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout voice = settingsCard(app, "Voice command safety",
                "Voice movement runs only for the selected duration, then R2-D2 stops automatically.");
        voice.addView(settingsSwitch(app, "Allow voice movement commands", prefs.voiceMotionEnabled(), prefs::setVoiceMotionEnabled));
        voice.addView(slider(app, "Movement duration", 500, 5000, prefs.voiceMotionDurationMs(), " ms",
                prefs::setVoiceMotionDurationMs));
        page.addView(voice, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout builder = settingsCard(app, "Routine Builder defaults",
                "These values are used when Cam adds a new block. Every block can still be tuned individually.");
        builder.addView(slider(app, "Default movement duration", 300, 5000,
                prefs.builderMotionDurationMs(), " ms", prefs::setBuilderMotionDurationMs));
        builder.addView(slider(app, "Default delay before each block", 0, 2000,
                prefs.builderDefaultDelayMs(), " ms", prefs::setBuilderDefaultDelayMs));
        page.addView(builder, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout diagnosticTools = new LinearLayout(app);
        diagnosticTools.setOrientation(LinearLayout.HORIZONTAL);
        diagnosticTools.addView(tool(app, "Copy Diagnostics", app::copyDiagnostics), Ui.weightedButton(app));
        diagnosticTools.addView(tool(app, "Clear Logs", () -> {
            AppLog.clear();
            app.showScreen(MainActivity.PARENT);
        }), Ui.weightedButton(app));
        diagnosticTools.addView(tool(app, "Bluetooth Settings",
                () -> app.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))), Ui.weightedButton(app));
        diagnosticTools.addView(tool(app, "Clear Saved Routines", app::confirmClearRoutines), Ui.weightedButton(app));
        page.addView(diagnosticTools, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout logs = Ui.card(app, "#0A2445");
        logs.setPadding(Ui.dp(app, 14), Ui.dp(app, 12), Ui.dp(app, 14), Ui.dp(app, 12));
        logs.addView(Ui.text(app, "Technical log", 16f, true, "#FFC83D"));
        String content = AppLog.text().isEmpty() ? "No log entries yet." : AppLog.text();
        TextView logText = Ui.text(app, content, 12f, false, "#EAF5FF");
        logText.setTypeface(Typeface.MONOSPACE);
        logs.addView(logText, Ui.matchWrap(app, 0, 8, 0, 0));
        page.addView(logs);
        return scroll;
    }

    private static LinearLayout settingsCard(MainActivity app, String title, String body) {
        LinearLayout card = Ui.card(app, "#FFFFFF");
        card.setPadding(Ui.dp(app, 16), Ui.dp(app, 14), Ui.dp(app, 16), Ui.dp(app, 14));
        card.addView(Ui.text(app, title, 19f, true, "#10233B"));
        card.addView(Ui.text(app, body, 13f, false, "#60758D"), Ui.matchWrap(app, 0, 4, 0, 8));
        return card;
    }

    private interface BooleanSetter { void set(boolean value); }
    private interface IntSetter { void set(int value); }

    private static Switch settingsSwitch(MainActivity app, String label, boolean checked, BooleanSetter setter) {
        Switch toggle = new Switch(app);
        toggle.setText(label);
        toggle.setTextSize(15f);
        toggle.setTextColor(Ui.color("#10233B"));
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> setter.set(isChecked));
        toggle.setPadding(0, Ui.dp(app, 3), 0, Ui.dp(app, 3));
        return toggle;
    }

    private static View slider(
            MainActivity app,
            String label,
            int min,
            int max,
            int current,
            String suffix,
            IntSetter setter
    ) {
        LinearLayout row = new LinearLayout(app);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, Ui.dp(app, 6), 0, Ui.dp(app, 4));
        TextView value = Ui.text(app, label + ": " + current + suffix, 14f, true, "#10233B");
        row.addView(value);
        SeekBar seek = new SeekBar(app);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, current - min)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int resolved = min + progress;
                value.setText(label + ": " + resolved + suffix);
                if (fromUser) setter.set(resolved);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        row.addView(seek);
        return row;
    }

    private static Button tool(MainActivity app, String label, Runnable action) {
        Button button = Ui.button(app, label, "#D9ECFF", "#0A4D96", 13f);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private static Button dangerTool(MainActivity app, String label, Runnable action) {
        Button button = Ui.button(app, label, "#FFE2E5", "#A52935", 13f);
        button.setOnClickListener(v -> action.run());
        return button;
    }
}
