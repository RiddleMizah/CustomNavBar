package com.riddle.customnavbar;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class NavAccessibilityService extends AccessibilityService {
    private WindowManager windowManager;
    private View navBar;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        showNavBar();
    }

    private void showNavBar() {
        if (navBar != null) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundResource(R.drawable.nav_bg);
        bar.setPadding(dp(6), dp(4), dp(6), dp(4));

        Button back = makeButton("◀");
        Button home = makeButton("●");
        Button recents = makeButton("■");
        back.setOnClickListener(v -> performGlobalAction(GLOBAL_ACTION_BACK));
        home.setOnClickListener(v -> performGlobalAction(GLOBAL_ACTION_HOME));
        recents.setOnClickListener(v -> {
            boolean worked = performGlobalAction(GLOBAL_ACTION_RECENTS);
            if (!worked) Toast.makeText(this, "Recents is missing from this firmware", Toast.LENGTH_SHORT).show();
        });

        bar.addView(back);
        bar.addView(home);
        bar.addView(recents);

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        p.y = dp(12);
        navBar = bar;
        windowManager.addView(navBar, p);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(22);
        b.setTextColor(Color.WHITE);
        b.setMinWidth(dp(74));
        b.setMinimumHeight(dp(54));
        b.setBackgroundColor(Color.TRANSPARENT);
        return b;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }

    @Override
    public void onDestroy() {
        if (navBar != null && windowManager != null) {
            windowManager.removeView(navBar);
            navBar = null;
        }
        super.onDestroy();
    }
}
