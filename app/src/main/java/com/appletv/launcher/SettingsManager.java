package com.appletv.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextClock;

public class SettingsManager {
    private static Handler sleepHandler = new Handler();
    private static Runnable sleepRunnable;

    public static void applyTimeFormat(Activity activity) {
        // Динамически ищем часы на экране
        int clockId = activity.getResources().getIdentifier("clock_tv", "id", activity.getPackageName());
        if (clockId == 0) clockId = activity.getResources().getIdentifier("textClock", "id", activity.getPackageName());
        if (clockId == 0) clockId = activity.getResources().getIdentifier("clock", "id", activity.getPackageName());
        
        if (clockId != 0) {
            View view = activity.findViewById(clockId);
            if (view instanceof TextClock) {
                TextClock clock = (TextClock) view;
                SharedPreferences prefs = activity.getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);
                int timeFormat = prefs.getInt("time_format", 0);
                if (timeFormat == 1) {
                    clock.setFormat12Hour("h:mm a");
                    clock.setFormat24Hour(null);
                } else {
                    clock.setFormat24Hour("HH:mm");
                    clock.setFormat12Hour(null);
                }
            }
        }
    }

    public static void startSleepTimer(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);
        int sleepTimeIdx = prefs.getInt("sleep_timeout", 0);
        
        long timeoutMs = 0;
        if (sleepTimeIdx == 1) timeoutMs = 5 * 60 * 1000;
        else if (sleepTimeIdx == 2) timeoutMs = 15 * 60 * 1000;
        else if (sleepTimeIdx == 3) timeoutMs = 30 * 60 * 1000;

        if (sleepRunnable != null) {
            sleepHandler.removeCallbacks(sleepRunnable);
        }

        // При любой активности возвращаем нормальную яркость
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        activity.getWindow().setAttributes(params);

        if (timeoutMs > 0) {
            sleepRunnable = () -> {
                // Затемняем экран при бездействии
                WindowManager.LayoutParams dimParams = activity.getWindow().getAttributes();
                dimParams.screenBrightness = 0.0f; 
                activity.getWindow().setAttributes(dimParams);
            };
            sleepHandler.postDelayed(sleepRunnable, timeoutMs);
        }
    }
}
