package com.appletv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

    // Базовые цвета
    public static int background(Context c) { return isLight(c) ? 0xFFF5F5F7 : 0xFF050508; }
    public static int textPrimary(Context c) { return isLight(c) ? 0xFF1A1A1C : 0xFFFFFFFF; }
    public static int textSecondary(Context c) { return isLight(c) ? 0xFF666666 : 0xFFCCCCCC; }
    public static int cardBg(Context c) { return isLight(c) ? 0xFFFFFFFF : 0xFF1A1A1C; }

    // Док (стекло)
    public static GradientDrawable dockBackground(Context c) {
        GradientDrawable gd = new GradientDrawable();
        if (isLight(c)) {
            gd.setColors(new int[]{0xCCFFFFFF, 0xE6FFFFFF, 0xCCFFFFFF});
            gd.setOrientation(GradientDrawable.Orientation.TL_BR);
            gd.setStroke(dp(c, 1), 0x4D000000);
            gd.setCornerRadius(dp(c, 28));
        } else {
            gd.setColors(new int[]{0x1AFFFFFF, 0x33FFFFFF, 0x0DFFFFFF});
            gd.setOrientation(GradientDrawable.Orientation.TL_BR);
            gd.setStroke(dp(c, 1), 0x4DFFFFFF);
            gd.setCornerRadius(dp(c, 28));
        }
        return gd;
    }

    // Градиент верхней панели (hero_gradient_overlay)
    public static GradientDrawable heroGradient(Context c) {
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                isLight(c) ? new int[]{0x00FFFFFF, 0x88FFFFFF, 0xFFFFFFFF}
                           : new int[]{0xFF000000, 0x88000000, 0x00000000});
        return gd;
    }

    // Верхний fade (top_fade_gradient)
    public static GradientDrawable topFade(Context c) {
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                isLight(c) ? new int[]{0x00F5F5F7, 0xB3F5F5F7, 0xFFF5F5F7}
                           : new int[]{0x00050508, 0xB3050508, 0xFF050508});
        return gd;
    }

    // Плашка часов (status_pill_bg)
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

    // Левая панель настроек
    public static GradientDrawable settingsLeftPanel(Context c) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isLight(c) ? 0xFFFFFFFF : 0xFF212124);
        gd.setCornerRadius(dp(c, 42));
        return gd;
    }

    // Строка настроек (обычная)
    public static GradientDrawable rowNormal(Context c) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isLight(c) ? 0x14000000 : 0x14FFFFFF);
        gd.setCornerRadius(dp(c, 6));
        return gd;
    }

    // Строка настроек (фокус)
    public static GradientDrawable rowFocused(Context c) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isLight(c) ? 0xFF000000 : 0xFFFFFFFF);
        gd.setCornerRadius(dp(c, 8));
        return gd;
    }

    public static int rowFocusedText(Context c) {
        return isLight(c) ? 0xFFFFFFFF : 0xFF000000;
    }

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void applyBackground(View root, Context c) {
        if (root != null) root.setBackgroundColor(background(c));
    }
}
