package com.riddle.customnavbar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = dp(24);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Custom Nav Bar");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Open Accessibility Settings and enable Custom navigation bar. No Display over other apps permission is required.");
        info.setTextSize(18);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, dp(20), 0, dp(20));

        Button settings = new Button(this);
        settings.setText("Open Accessibility Settings");
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        root.addView(info, new LinearLayout.LayoutParams(-1, -2));
        root.addView(settings, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
