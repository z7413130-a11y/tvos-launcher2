package com.appletv.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import java.util.Calendar;

public class ThemeManager {

    // Применяем белый/черный фон и красим Док-бар
    public static void applyThemeToMainActivity(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);
        String theme = prefs.getString("theme_mode", "dark");
        
        boolean isLight = false;
        if (theme.equals("light")) {
            isLight = true;
        } else if (theme.equals("auto")) {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            isLight = (hour >= 7 && hour < 19);
        }

        View root = activity.getWindow().getDecorView().getRootView();
        int dockId = activity.getResources().getIdentifier("dock_container", "id", activity.getPackageName());
        View dock = activity.findViewById(dockId);

        if (isLight) {
            // Белый интерфейс
            root.setBackgroundColor(Color.parseColor("#F2F2F7"));
            if (dock != null && dock.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) dock.getBackground()).setColor(Color.parseColor("#E6FFFFFF"));
            }
        } else {
            // Черный интерфейс
            root.setBackgroundColor(Color.parseColor("#000000"));
            if (dock != null && dock.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) dock.getBackground()).setColor(Color.parseColor("#4D1C1C1E"));
            }
        }
    }

    // Проверяем, включена ли динамическая подсветка
    public static boolean isAmbientGlowEnabled(Context context) {
        return context.getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE)
                      .getBoolean("ambient_glow", true);
    }
}
