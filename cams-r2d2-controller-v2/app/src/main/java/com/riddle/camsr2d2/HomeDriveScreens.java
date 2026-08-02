package com.riddle.camsr2d2;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

final class HomeDriveScreens {
    private HomeDriveScreens() {}

    static View home(MainActivity app) {
        ScrollView scroll = new ScrollView(app);
        LinearLayout page = Ui.page(app);
        scroll.addView(page);

        LinearLayout hero = Ui.card(app, "#F7FAFD");
        hero.setPadding(Ui.dp(app, 24), Ui.dp(app, 22), Ui.dp(app, 24), Ui.dp(app, 22));
        hero.addView(Ui.text(app, "Ready for an adventure?", 28f, true, "#10233B"));
        hero.addView(Ui.text(app,
                "Connect R2-D2, drive him around, play a dance, or teach him a brand-new routine.",
                16f, false, "#60758D"));
        Button connect = Ui.button(app,
                app.isR2Ready() ? "Disconnect R2-D2" : "Connect R2-D2",
                app.isR2Ready() ? "#D63A46" : "#1677D2", "#FFFFFF", 17f);
        connect.setOnClickListener(v -> {
            if (app.isR2Ready()) app.disconnectR2D2();
            else app.requestConnection();
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(Ui.dp(app, 220), Ui.dp(app, 52));
        cp.setMargins(0, Ui.dp(app, 18), 0, 0);
        hero.addView(connect, cp);
        page.addView(hero, Ui.matchWrap(app, 0, 0, 0, 14));

        GridLayout grid = new GridLayout(app);
        grid.setColumnCount(3);
        grid.addView(quickCard(app, "Drive R2-D2", "Big hold-to-drive controls.", "DRIVE",
                () -> app.showScreen(MainActivity.DRIVE)), Ui.gridCell(app));
        grid.addView(quickCard(app, "Dance Party", "Six ready-made routines.", "DANCE",
                () -> app.showScreen(MainActivity.DANCES)), Ui.gridCell(app));
        grid.addView(quickCard(app, "Make a Move", "Record and replay your own routine.", "CREATE",
                app::startRecording), Ui.gridCell(app));
        if (app.preferences().tiltEnabled()) {
            grid.addView(quickCard(app, "Tilt Drive", "Steer by gently tilting the tablet.", "TILT",
                    () -> app.showScreen(MainActivity.TILT)), Ui.gridCell(app));
        }
        if (app.preferences().builderEnabled()) {
            grid.addView(quickCard(app, "Routine Builder", "Build moves with colorful action blocks.", "BUILD",
                    () -> app.showScreen(MainActivity.BUILDER)), Ui.gridCell(app));
        }
        if (app.preferences().voiceEnabled()) {
            grid.addView(quickCard(app, "Voice Commands", "Tell R2-D2 what to do.", "VOICE",
                    () -> app.showScreen(MainActivity.VOICE)), Ui.gridCell(app));
        }
        page.addView(grid);
        return scroll;
    }

    private static View quickCard(MainActivity app, String title, String body, String badge, Runnable action) {
        LinearLayout card = Ui.card(app, "#FFFFFF");
        card.setPadding(Ui.dp(app, 18), Ui.dp(app, 18), Ui.dp(app, 18), Ui.dp(app, 18));
        TextView badgeView = Ui.text(app, badge, 11f, true, "#0A4D96");
        badgeView.setBackground(Ui.round(app, "#D9ECFF", 12));
        badgeView.setPadding(Ui.dp(app, 10), Ui.dp(app, 5), Ui.dp(app, 10), Ui.dp(app, 5));
        card.addView(badgeView, new LinearLayout.LayoutParams(-2, -2));
        card.addView(Ui.text(app, title, 20f, true, "#10233B"), Ui.matchWrap(app, 0, 12, 0, 0));
        card.addView(Ui.text(app, body, 14f, false, "#60758D"), Ui.matchWrap(app, 0, 6, 0, 14));
        Button open = Ui.button(app, "Open", "#1677D2", "#FFFFFF", 14f);
        open.setOnClickListener(v -> action.run());
        card.addView(open, new LinearLayout.LayoutParams(-1, Ui.dp(app, 44)));
        return card;
    }

    static View drive(MainActivity app) {
        LinearLayout page = new LinearLayout(app);
        page.setOrientation(LinearLayout.HORIZONTAL);
        page.setPadding(Ui.dp(app, 4), Ui.dp(app, 4), Ui.dp(app, 4), Ui.dp(app, 4));

        LinearLayout drive = Ui.card(app, "#FFFFFF");
        drive.setPadding(Ui.dp(app, 18), Ui.dp(app, 14), Ui.dp(app, 18), Ui.dp(app, 14));
        page.addView(drive, new LinearLayout.LayoutParams(0, -1, 1.2f));
        drive.addView(Ui.text(app, "Hold to drive — release to stop", 18f, true, "#10233B"));

        GridLayout dpad = new GridLayout(app);
        dpad.setColumnCount(3);
        dpad.setRowCount(3);
        dpad.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        Ui.addGridSpacer(app, dpad);
        dpad.addView(motionButton(app, "▲\nForward", ActionType.FORWARD), Ui.gridCell(app));
        Ui.addGridSpacer(app, dpad);
        dpad.addView(motionButton(app, "◀\nLeft", ActionType.TURN_LEFT), Ui.gridCell(app));
        Button stop = Ui.bigButton(app, "■\nSTOP", "#D63A46");
        stop.setOnClickListener(v -> app.performAction(ActionType.STOP, true));
        dpad.addView(stop, Ui.gridCell(app));
        dpad.addView(motionButton(app, "▶\nRight", ActionType.TURN_RIGHT), Ui.gridCell(app));
        Ui.addGridSpacer(app, dpad);
        dpad.addView(motionButton(app, "▼\nReverse", ActionType.REVERSE), Ui.gridCell(app));
        Ui.addGridSpacer(app, dpad);
        drive.addView(dpad, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button emergency = Ui.button(app, "EMERGENCY STOP", "#D63A46", "#FFFFFF", 18f);
        emergency.setOnClickListener(v -> app.emergencyStop());
        drive.addView(emergency, new LinearLayout.LayoutParams(-1, Ui.dp(app, 56)));

        LinearLayout extras = new LinearLayout(app);
        extras.setOrientation(LinearLayout.VERTICAL);
        extras.setPadding(Ui.dp(app, 14), 0, 0, 0);
        page.addView(extras, new LinearLayout.LayoutParams(0, -1, 0.8f));
        extras.addView(controlSection(app, "Head", Arrays.asList(
                controlButton(app, "Left", ActionType.HEAD_LEFT),
                controlButton(app, "Center", ActionType.HEAD_CENTER),
                controlButton(app, "Right", ActionType.HEAD_RIGHT))));
        extras.addView(controlSection(app, "Lights", Arrays.asList(
                controlButton(app, "Red", ActionType.LIGHT_RED),
                controlButton(app, "Blue", ActionType.LIGHT_BLUE),
                controlButton(app, "Off", ActionType.LIGHTS_OFF))), Ui.matchWrap(app, 0, 12, 0, 0));
        extras.addView(controlSection(app, "Sounds", Arrays.asList(
                controlButton(app, "Wake", ActionType.WAKE),
                controlButton(app, "Whistle", ActionType.WHISTLE),
                controlButton(app, "Victory", ActionType.ACHIEVEMENT))), Ui.matchWrap(app, 0, 12, 0, 0));
        return page;
    }

    private static Button motionButton(MainActivity app, String label, ActionType action) {
        Button button = Ui.bigButton(app, label, "#1677D2");
        button.setOnTouchListener((view, event) -> {
            int type = event.getActionMasked();
            if (type == MotionEvent.ACTION_DOWN) {
                app.startManualMotion(action);
                view.setPressed(true);
                return true;
            }
            if (type == MotionEvent.ACTION_UP || type == MotionEvent.ACTION_CANCEL) {
                app.stopManualMotion();
                view.setPressed(false);
                view.performClick();
                return true;
            }
            return false;
        });
        return button;
    }

    private static View controlSection(MainActivity app, String title, List<Button> buttons) {
        LinearLayout section = Ui.card(app, "#FFFFFF");
        section.setPadding(Ui.dp(app, 14), Ui.dp(app, 12), Ui.dp(app, 14), Ui.dp(app, 14));
        section.addView(Ui.text(app, title, 17f, true, "#10233B"));
        LinearLayout row = new LinearLayout(app);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(app, 8), 0, 0);
        for (Button button : buttons) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Ui.dp(app, 52), 1f);
            params.setMargins(Ui.dp(app, 3), 0, Ui.dp(app, 3), 0);
            row.addView(button, params);
        }
        section.addView(row);
        return section;
    }

    private static Button controlButton(MainActivity app, String label, ActionType action) {
        Button button = Ui.button(app, label, "#D9ECFF", "#0A4D96", 14f);
        button.setOnClickListener(v -> app.performAction(action, true));
        return button;
    }
}
