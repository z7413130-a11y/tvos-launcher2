package com.appletv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

public class ThemeManager {

    public static final String PREFS = "tvos_prefs";
    public static final String KEY_THEME = "theme_mode"; // 0 = тёмная, 1 = светлая

    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, 0);
    }

    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, mode).apply();
    }

    public static boolean isLight(Context context) {
        return getThemeMode(context) == 1;
    }

    // Базовые цвета
    public static int background(Context c) { return isLight(c) ? 0xFFF5F5F7 : 0xFF050508; }
    public static int textPrimary(Context c) { return isLight(c) ? 0xFF1A1A1C : 0xFFFFFFFF; }
    public static int clockText(Context c) { return isLight(c) ? 0xCC000000 : 0xCCFFFFFF; }

    // Док — полупрозрачный в обеих темах
    public static GradientDrawable dockBackground(Context c) {
        GradientDrawable gd = new GradientDrawable();
        if (isLight(c)) {
            gd.setColors(new int[]{0x66FFFFFF, 0x99FFFFFF, 0x66FFFFFF});
            gd.setOrientation(GradientDrawable.Orientation.TL_BR);
            gd.setStroke(dp(c, 1), 0x33000000);
            gd.setCornerRadius(dp(c, 28));
        } else {
            gd.setColors(new int[]{0x1AFFFFFF, 0x33FFFFFF, 0x0DFFFFFF});
            gd.setOrientation(GradientDrawable.Orientation.TL_BR);
            gd.setStroke(dp(c, 1), 0x4DFFFFFF);
            gd.setCornerRadius(dp(c, 28));
        }
        return gd;
    }

    // Плашка часов
    public static GradientDrawable statusPill(Context c) {
        GradientDrawable gd = new GradientDrawable();
        if (isLight(c)) {
            gd.setColor(0x33FFFFFF);
            gd.setStroke(dp(c, 1), 0x33000000);
        } else {
            gd.setColor(0x33252528);
            gd.setStroke(dp(c, 1), 0x33FFFFFF);
        }
        gd.setCornerRadius(dp(c, 22));
        return gd;
    }

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void applyBackground(View root, Context c) {
        if (root != null) root.setBackgroundColor(background(c));
    }
}
