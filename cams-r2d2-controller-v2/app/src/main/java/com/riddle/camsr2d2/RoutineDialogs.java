package com.riddle.camsr2d2;

import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

final class RoutineDialogs {
    private RoutineDialogs() {}

    static void finishRecording(
            MainActivity app,
            RoutineRecorder recorder,
            List<Routine> routines,
            RoutineStore store,
            MotionController motion,
            TextView banner
    ) {
        if (!recorder.isRecording()) return;
        motion.stopManual();
        List<RoutineStep> steps = recorder.finish();
        banner.setVisibility(View.GONE);
        if (steps.isEmpty()) {
            app.toastMessage("Nothing was recorded.");
            return;
        }
        EditText input = new EditText(app);
        input.setHint(app.controllerPossessive() + " Awesome Dance");
        input.setSingleLine(true);
        new AlertDialog.Builder(app)
                .setTitle("Name this routine")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) title = app.controllerPossessive() + " Move " + (routines.size() + 1);
                    routines.add(0, Routine.custom(title, steps));
                    store.save(routines);
                    AppLog.add("Saved custom routine: " + title + ".");
                    app.showScreen(MainActivity.MY_MOVES);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static void rename(MainActivity app, Routine routine, List<Routine> routines, RoutineStore store) {
        EditText input = new EditText(app);
        input.setText(routine.name);
        input.setSingleLine(true);
        new AlertDialog.Builder(app)
                .setTitle("Rename routine")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (!title.isEmpty()) {
                        routine.name = title;
                        store.save(routines);
                        app.showScreen(MainActivity.MY_MOVES);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static void delete(MainActivity app, Routine routine, List<Routine> routines, RoutineStore store) {
        new AlertDialog.Builder(app)
                .setTitle("Delete " + routine.name + "?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    routines.remove(routine);
                    store.save(routines);
                    app.showScreen(MainActivity.MY_MOVES);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static void clearAll(MainActivity app, List<Routine> routines, RoutineStore store) {
        new AlertDialog.Builder(app)
                .setTitle("Clear all saved routines?")
                .setMessage("Preset dances will stay. Only custom routines will be removed.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    routines.clear();
                    store.clear();
                    AppLog.add("All custom routines were cleared.");
                    app.showScreen(MainActivity.PARENT);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
