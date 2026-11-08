package com.appletv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    // Цвета
    public static int background(Context c) { return isLight(c) ? 0xFFF5F5F7 : 0xFF050508; }
    public static int textPrimary(Context c) { return isLight(c) ? 0xFF1A1A1C : 0xFFFFFFFF; }
    public static int textSecondary(Context c) { return isLight(c) ? 0xFF666666 : 0xFFCCCCCC; }
    public static int cardBg(Context c) { return isLight(c) ? 0xFFFFFFFF : 0xFF1A1A1C; }
    public static int dockBg(Context c) { return isLight(c) ? 0xCCFFFFFF : 0x1AFFFFFF; }
    public static int rowBg(Context c) { return isLight(c) ? 0x14000000 : 0x14FFFFFF; }
    public static int rowFocusedBg(Context c) { return isLight(c) ? 0xFF000000 : 0xFFFFFFFF; }
    public static int rowFocusedText(Context c) { return isLight(c) ? 0xFFFFFFFF : 0xFF000000; }

    /** Применить фон к корневому View. */
    public static void applyBackground(View root, Context c) {
        if (root != null) root.setBackgroundColor(background(c));
    }

    /** Рекурсивно применить цвета текста (для простых layout'ов). */
    public static void applyTextColors(View root, Context c) {
        if (root instanceof TextView) {
            ((TextView) root).setTextColor(textPrimary(c));
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) applyTextColors(g.getChildAt(i), c);
        }
    }
}
