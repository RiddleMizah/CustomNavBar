package com.riddle.camsr2d2;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

final class RoutineBuilderScreen {
    private RoutineBuilderScreen() {}

    static View build(MainActivity app) {
        ScrollView scroll = new ScrollView(app);
        LinearLayout page = Ui.page(app);
        scroll.addView(page);

        page.addView(Ui.text(app, "Visual Routine Builder", 25f, true, "#10233B"));
        page.addView(Ui.text(app,
                "Tap colorful action blocks to build a routine. Reorder, tune, preview, then save it to My Moves.",
                14f, false, "#60758D"), Ui.matchWrap(app, 0, 4, 0, 12));

        LinearLayout palette = Ui.card(app, "#FFFFFF");
        palette.setPadding(Ui.dp(app, 16), Ui.dp(app, 14), Ui.dp(app, 16), Ui.dp(app, 16));
        palette.addView(Ui.text(app, "1. Add action blocks", 19f, true, "#10233B"));

        GridLayout actionGrid = new GridLayout(app);
        actionGrid.setColumnCount(4);
        actionGrid.setPadding(0, Ui.dp(app, 8), 0, 0);
        for (ActionType action : ActionType.values()) {
            Button button = Ui.button(app, shortLabel(action), blockColor(action), "#FFFFFF", 13f);
            button.setOnClickListener(v -> app.addBuilderStep(action));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = Ui.dp(app, 54);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(Ui.dp(app, 4), Ui.dp(app, 4), Ui.dp(app, 4), Ui.dp(app, 4));
            actionGrid.addView(button, params);
        }
        palette.addView(actionGrid, new LinearLayout.LayoutParams(-1, -2));
        page.addView(palette, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout sequence = Ui.card(app, "#F7FAFD");
        sequence.setPadding(Ui.dp(app, 16), Ui.dp(app, 14), Ui.dp(app, 16), Ui.dp(app, 16));
        sequence.addView(Ui.text(app, "2. Arrange the sequence", 19f, true, "#10233B"));

        List<RoutineStep> steps = app.builderSteps();
        if (steps.isEmpty()) {
            sequence.addView(Ui.text(app,
                    "Your routine is empty. Tap an action block above to begin.",
                    15f, false, "#60758D"), Ui.matchWrap(app, 0, 12, 0, 0));
        } else {
            for (int i = 0; i < steps.size(); i++) {
                sequence.addView(stepBlock(app, steps.get(i), i), Ui.matchWrap(app, 0, 8, 0, 0));
            }
        }
        page.addView(sequence, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout actions = new LinearLayout(app);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        Button preview = Ui.button(app, "▶ Preview", "#1677D2", "#FFFFFF", 15f);
        preview.setEnabled(!steps.isEmpty());
        preview.setAlpha(steps.isEmpty() ? 0.45f : 1f);
        preview.setOnClickListener(v -> app.previewBuilderRoutine());
        actions.addView(preview, Ui.weightedButton(app));

        Button save = Ui.button(app, "Save to My Moves", "#1D9A62", "#FFFFFF", 15f);
        save.setEnabled(!steps.isEmpty());
        save.setAlpha(steps.isEmpty() ? 0.45f : 1f);
        save.setOnClickListener(v -> app.saveBuilderRoutine());
        actions.addView(save, Ui.weightedButton(app));

        Button clear = Ui.button(app, "Clear", "#FFE2E5", "#A52935", 15f);
        clear.setEnabled(!steps.isEmpty());
        clear.setAlpha(steps.isEmpty() ? 0.45f : 1f);
        clear.setOnClickListener(v -> app.confirmClearBuilder());
        actions.addView(clear, Ui.weightedButton(app));

        page.addView(actions, new LinearLayout.LayoutParams(-1, Ui.dp(app, 56)));
        return scroll;
    }

    private static View stepBlock(MainActivity app, RoutineStep step, int index) {
        LinearLayout block = new LinearLayout(app);
        block.setOrientation(LinearLayout.HORIZONTAL);
        block.setGravity(Gravity.CENTER_VERTICAL);
        block.setPadding(Ui.dp(app, 12), Ui.dp(app, 10), Ui.dp(app, 10), Ui.dp(app, 10));
        block.setBackground(Ui.round(app, blockColor(step.action), 14));

        TextView number = Ui.text(app, String.valueOf(index + 1), 18f, true, "#FFFFFF");
        number.setGravity(Gravity.CENTER);
        block.addView(number, new LinearLayout.LayoutParams(Ui.dp(app, 38), Ui.dp(app, 38)));

        LinearLayout info = new LinearLayout(app);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(Ui.text(app, step.action.label, 16f, true, "#FFFFFF"));
        String timing = "Delay " + step.delayBeforeMs + " ms";
        if (step.action.motion) timing += "  •  Move " + step.durationMs + " ms";
        info.addView(Ui.text(app, timing, 12f, false, "#F1F7FF"));
        block.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        block.addView(smallButton(app, "↑", () -> app.moveBuilderStep(index, -1)), smallParams(app));
        block.addView(smallButton(app, "↓", () -> app.moveBuilderStep(index, 1)), smallParams(app));
        block.addView(smallButton(app, "Tune", () -> showTimingDialog(app, index, step)), tuneParams(app));
        block.addView(smallButton(app, "×", () -> app.removeBuilderStep(index)), smallParams(app));
        return block;
    }

    private static Button smallButton(MainActivity app, String text, Runnable action) {
        Button button = Ui.button(app, text, "#FFFFFF", "#0A4D96", 13f);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private static LinearLayout.LayoutParams smallParams(MainActivity app) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.dp(app, 44), Ui.dp(app, 40));
        params.setMargins(Ui.dp(app, 4), 0, 0, 0);
        return params;
    }

    private static LinearLayout.LayoutParams tuneParams(MainActivity app) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.dp(app, 68), Ui.dp(app, 40));
        params.setMargins(Ui.dp(app, 4), 0, 0, 0);
        return params;
    }

    private static void showTimingDialog(MainActivity app, int index, RoutineStep step) {
        LinearLayout panel = new LinearLayout(app);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(app, 24), Ui.dp(app, 8), Ui.dp(app, 24), 0);

        TextView delayLabel = Ui.text(app, "Delay before action: " + step.delayBeforeMs + " ms", 15f, true, "#10233B");
        panel.addView(delayLabel);
        SeekBar delay = new SeekBar(app);
        delay.setMax(2000);
        delay.setProgress((int) Math.min(2000, step.delayBeforeMs));
        delay.setOnSeekBarChangeListener(simpleListener(value -> delayLabel.setText("Delay before action: " + value + " ms")));
        panel.addView(delay);

        SeekBar duration = null;
        if (step.action.motion) {
            TextView durationLabel = Ui.text(app, "Movement duration: " + step.durationMs + " ms", 15f, true, "#10233B");
            panel.addView(durationLabel, Ui.matchWrap(app, 0, 10, 0, 0));
            duration = new SeekBar(app);
            duration.setMax(4700);
            duration.setProgress((int) Math.max(0, Math.min(4700, step.durationMs - 300)));
            SeekBar displayedDuration = duration;
            duration.setOnSeekBarChangeListener(simpleListener(value ->
                    durationLabel.setText("Movement duration: " + (value + 300) + " ms")));
            panel.addView(displayedDuration);
        } else {
            panel.addView(Ui.text(app,
                    "This action has a short fixed playback time. Only its delay is adjustable.",
                    13f, false, "#60758D"), Ui.matchWrap(app, 0, 10, 0, 0));
        }

        SeekBar finalDuration = duration;
        new AlertDialog.Builder(app)
                .setTitle("Tune " + step.action.label)
                .setView(panel)
                .setPositiveButton("Apply", (dialog, which) -> {
                    long durationMs = step.action.motion && finalDuration != null
                            ? finalDuration.getProgress() + 300L : 0L;
                    app.updateBuilderStep(index, delay.getProgress(), durationMs);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private interface ProgressConsumer { void accept(int value); }

    private static SeekBar.OnSeekBarChangeListener simpleListener(ProgressConsumer consumer) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                consumer.accept(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    private static String shortLabel(ActionType action) {
        switch (action) {
            case FORWARD: return "▲ Forward";
            case REVERSE: return "▼ Reverse";
            case TURN_LEFT: return "◀ Turn Left";
            case TURN_RIGHT: return "▶ Turn Right";
            case STOP: return "■ Stop";
            case HEAD_LEFT: return "Head Left";
            case HEAD_CENTER: return "Head Center";
            case HEAD_RIGHT: return "Head Right";
            case LIGHT_RED: return "Red Light";
            case LIGHT_BLUE: return "Blue Light";
            case LIGHTS_OFF: return "Lights Off";
            case WAKE: return "Wake Sound";
            case WHISTLE: return "Whistle";
            case ACHIEVEMENT: return "Celebrate";
            default: return action.label;
        }
    }

    private static String blockColor(ActionType action) {
        if (action == ActionType.STOP) return "#D63A46";
        if (action.motion) return "#1677D2";
        switch (action) {
            case HEAD_LEFT:
            case HEAD_CENTER:
            case HEAD_RIGHT: return "#7653B5";
            case LIGHT_RED:
            case LIGHT_BLUE:
            case LIGHTS_OFF: return "#B77900";
            default: return "#1D9A62";
        }
    }
}
