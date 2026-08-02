package com.riddle.camsr2d2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    private Ui() {}

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static int color(String hex) {
        return Color.parseColor(hex);
    }

    static GradientDrawable round(Context context, String fill, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color(fill));
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    static TextView text(Context context, String value, float size, boolean bold, String color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color(color));
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setLineSpacing(0f, 1.08f);
        return view;
    }

    static Button button(Context context, String value, String background, String foreground, float size) {
        Button button = new Button(context);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(size);
        button.setTextColor(color(foreground));
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(context, 8), 0, dp(context, 8), 0);
        button.setBackground(round(context, background, 14));
        return button;
    }

    static Button bigButton(Context context, String value, String background) {
        Button button = button(context, value, background, "#FFFFFF", 19f);
        button.setMinHeight(dp(context, 80));
        return button;
    }

    static TextView banner(Context context, String value, String background) {
        TextView view = text(context, value, 14f, true, "#FFFFFF");
        view.setGravity(Gravity.CENTER);
        view.setBackground(round(context, background, 12));
        return view;
    }

    static LinearLayout card(Context context, String background) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(context, background, 18));
        card.setElevation(dp(context, 2));
        return card;
    }

    static LinearLayout page(Context context) {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 20));
        return page;
    }

    static LinearLayout.LayoutParams matchWrap(Context context, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(context, left), dp(context, top), dp(context, right), dp(context, bottom));
        return params;
    }

    static LinearLayout.LayoutParams weightedButton(Context context) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(context, 48), 1f);
        params.setMargins(dp(context, 4), 0, dp(context, 4), 0);
        return params;
    }

    static GridLayout.LayoutParams gridCell(Context context) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6));
        return params;
    }

    static void addGridSpacer(Context context, GridLayout grid) {
        grid.addView(new View(context), gridCell(context));
    }
}
