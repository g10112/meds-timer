package com.example.alexsmedstimer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = Color.rgb(9, 11, 16);
    static final int CARD = Color.rgb(20, 24, 33);
    static final int CARD_BORDER = Color.rgb(54, 64, 82);
    static final int TEXT = Color.rgb(245, 247, 251);
    static final int MUTED = Color.rgb(174, 183, 198);
    static final int ACCENT = Color.rgb(139, 233, 253);
    static final int SUCCESS = Color.rgb(111, 237, 181);
    static final int DANGER = Color.rgb(255, 126, 145);

    private Ui() {}

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context context, String value, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    static Button button(Context context, String label, int background, int foreground) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextColor(foreground);
        button.setTextSize(14f);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans", Typeface.BOLD));
        button.setMinHeight(dp(context, 54));
        button.setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10));
        button.setBackground(rounded(background, CARD_BORDER, dp(context, 14), dp(context, 1)));
        return button;
    }

    static LinearLayout card(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 18);
        layout.setPadding(padding, padding, padding, padding);
        layout.setBackground(rounded(CARD, CARD_BORDER, dp(context, 18), dp(context, 1)));
        return layout;
    }

    static GradientDrawable rounded(int fill, int stroke, int radiusPx, int strokePx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            drawable.setStroke(strokePx, stroke);
        }
        return drawable;
    }

    static LinearLayout.LayoutParams fullWidth(Context context, int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(context, topMarginDp);
        return params;
    }

    static void visible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
