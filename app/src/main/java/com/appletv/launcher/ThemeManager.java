package com.appletv.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import java.util.Calendar;

public class ThemeManager {
    
    public static void applySettings(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);
        
        // --- 1. ЛОГИКА AMBIENT GLOW ---
        boolean glow = prefs.getBoolean("ambient_glow", true);
        // Ищем вьюшку свечения (по ID из твоих ресурсов)
        int overlayId = activity.getResources().getIdentifier("hero_gradient_overlay", "id", activity.getPackageName());
        if (overlayId != 0) {
            View glowView = activity.findViewById(overlayId);
            if (glowView != null) {
                glowView.setVisibility(glow ? View.VISIBLE : View.GONE);
            }
        }

        // --- 2. ЛОГИКА ТЕМ (Авто / Светлая / Темная) ---
        String theme = prefs.getString("theme_mode", "dark");
        boolean isLight = false;
        
        if (theme.equals("light")) {
            isLight = true;
        } else if (theme.equals("auto")) {
            // Если Авто: с 07:00 до 19:00 — светлая, ночью — темная
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            isLight = (hour >= 7 && hour < 19);
        }
        
        // Мгновенно перекрашиваем фон корневого окна, чтобы изменения были без перезапуска
        View root = activity.getWindow().getDecorView().getRootView();
        if (isLight) {
            root.setBackgroundColor(android.graphics.Color.parseColor("#F2F2F7"));
        } else {
            root.setBackgroundColor(android.graphics.Color.parseColor("#000000"));
        }
    }
}
