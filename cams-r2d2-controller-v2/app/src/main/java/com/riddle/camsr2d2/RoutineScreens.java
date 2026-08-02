package com.riddle.camsr2d2;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

final class RoutineScreens {
    private RoutineScreens() {}

    static View dances(MainActivity app) {
        ScrollView scroll = new ScrollView(app);
        LinearLayout page = Ui.page(app);
        scroll.addView(page);
        page.addView(Ui.text(app, "Pick a dance", 25f, true, "#10233B"));
        page.addView(Ui.text(app,
                "Every routine uses the same tested commands as the working controller.",
                14f, false, "#60758D"), Ui.matchWrap(app, 0, 4, 0, 12));
        for (Routine routine : PresetRoutines.all()) {
            page.addView(routineCard(app, routine, false), Ui.matchWrap(app, 0, 0, 0, 10));
        }
        return scroll;
    }

    static View myMoves(MainActivity app) {
        ScrollView scroll = new ScrollView(app);
        LinearLayout page = Ui.page(app);
        scroll.addView(page);

        LinearLayout header = Ui.card(app, "#FFFFFF");
        header.setPadding(Ui.dp(app, 18), Ui.dp(app, 16), Ui.dp(app, 18), Ui.dp(app, 16));
        header.addView(Ui.text(app, "Teach R2-D2 a new routine", 22f, true, "#10233B"));
        header.addView(Ui.text(app,
                "Tap Record, use the Drive controls, then tap the red banner to save it.",
                14f, false, "#60758D"), Ui.matchWrap(app, 0, 5, 0, 12));
        Button record = Ui.button(app,
                app.isRecording() ? "Finish Recording" : "Record New Move",
                app.isRecording() ? "#D63A46" : "#1677D2", "#FFFFFF", 16f);
        record.setOnClickListener(v -> {
            if (app.isRecording()) app.finishRecording(); else app.startRecording();
        });
        header.addView(record, new LinearLayout.LayoutParams(Ui.dp(app, 220), Ui.dp(app, 50)));
        page.addView(header, Ui.matchWrap(app, 0, 0, 0, 14));

        page.addView(Ui.text(app, "Saved routines", 20f, true, "#10233B"));
        if (app.customRoutines().isEmpty()) {
            page.addView(Ui.text(app, "No saved routines yet. Your first one will appear here.",
                    15f, false, "#60758D"), Ui.matchWrap(app, 0, 10, 0, 0));
        } else {
            for (Routine routine : app.customRoutines()) {
                page.addView(routineCard(app, routine, true), Ui.matchWrap(app, 0, 10, 0, 0));
            }
        }
        return scroll;
    }

    private static View routineCard(MainActivity app, Routine routine, boolean editable) {
        LinearLayout card = Ui.card(app, "#FFFFFF");
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(Ui.dp(app, 16), Ui.dp(app, 14), Ui.dp(app, 16), Ui.dp(app, 14));

        LinearLayout info = new LinearLayout(app);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(Ui.text(app, routine.name, 18f, true, "#10233B"));
        info.addView(Ui.text(app, routine.steps.size() + " steps", 13f, false, "#60758D"));
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        Button play = Ui.button(app, "Play", "#1677D2", "#FFFFFF", 14f);
        play.setOnClickListener(v -> app.playRoutine(routine));
        card.addView(play, new LinearLayout.LayoutParams(Ui.dp(app, 90), Ui.dp(app, 44)));

        if (editable) {
            Button rename = Ui.button(app, "Rename", "#D9ECFF", "#0A4D96", 13f);
            rename.setOnClickListener(v -> app.renameRoutine(routine));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(Ui.dp(app, 92), Ui.dp(app, 44));
            rp.setMargins(Ui.dp(app, 8), 0, 0, 0);
            card.addView(rename, rp);

            Button delete = Ui.button(app, "Delete", "#FFE2E5", "#A52935", 13f);
            delete.setOnClickListener(v -> app.deleteRoutine(routine));
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(Ui.dp(app, 88), Ui.dp(app, 44));
            dp.setMargins(Ui.dp(app, 8), 0, 0, 0);
            card.addView(delete, dp);
        }
        return card;
    }
}
