package com.riddle.camsr2d2;

import android.content.Intent;
import android.graphics.Typeface;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class ParentScreen {
    private ParentScreen() {}

    static View build(MainActivity app) {
        ScrollView scroll = new ScrollView(app);
        LinearLayout page = Ui.page(app);
        scroll.addView(page);

        page.addView(Ui.text(app, "Parent Mode", 25f, true, "#10233B"));
        page.addView(Ui.text(app,
                "Technical information, connection tools, diagnostics, and reset options.",
                14f, false, "#60758D"), Ui.matchWrap(app, 0, 4, 0, 12));

        LinearLayout status = Ui.card(app, "#FFFFFF");
        status.setPadding(Ui.dp(app, 16), Ui.dp(app, 14), Ui.dp(app, 16), Ui.dp(app, 14));
        status.addView(Ui.text(app, app.technicalSummary(), 13f, false, "#10233B"));
        page.addView(status, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout tools = new LinearLayout(app);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.addView(tool(app, "Reconnect", app::reconnect), Ui.weightedButton(app));
        tools.addView(tool(app, "Emergency Stop", app::emergencyStop), Ui.weightedButton(app));
        tools.addView(tool(app, "Copy Diagnostics", app::copyDiagnostics), Ui.weightedButton(app));
        tools.addView(tool(app, "Clear Logs", () -> {
            AppLog.clear();
            app.showScreen(MainActivity.PARENT);
        }), Ui.weightedButton(app));
        page.addView(tools, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout resets = new LinearLayout(app);
        resets.setOrientation(LinearLayout.HORIZONTAL);
        resets.addView(tool(app, "Clear Saved Routines", app::confirmClearRoutines), Ui.weightedButton(app));
        resets.addView(tool(app, "Bluetooth Settings",
                () -> app.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))), Ui.weightedButton(app));
        page.addView(resets, Ui.matchWrap(app, 0, 0, 0, 12));

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

    private static Button tool(MainActivity app, String label, Runnable action) {
        Button button = Ui.button(app, label, "#D9ECFF", "#0A4D96", 13f);
        button.setOnClickListener(v -> action.run());
        return button;
    }
}
