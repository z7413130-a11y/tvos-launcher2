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
        int clockId = activity.getResources().getIdentifier("clock_tv", "id", activity.getPackageName());
        if (clockId == 0) clockId = activity.getResources().getIdentifier("main_text_clock", "id", activity.getPackageName());

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

    /** Скрыть/показать блок часов (time_pill_container в красивом layout). */
    public static void applyClockVisibility(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);
        boolean show = prefs.getBoolean("show_clock", true);
        int id = activity.getResources().getIdentifier("time_pill_container", "id", activity.getPackageName());
        if (id == 0) id = activity.getResources().getIdentifier("clock_tv", "id", activity.getPackageName());
        if (id != 0) {
            View v = activity.findViewById(id);
            if (v != null) v.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /** Применить режим фона: 0 = подсветка (баннеры скрыты), 1 = баннеры. */
    public static void applyBackgroundMode(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);
        int bgMode = prefs.getInt("bg_mode", 0);
        int id = activity.getResources().getIdentifier("banner_container", "id", activity.getPackageName());
        if (id != 0) {
            View banner = activity.findViewById(id);
            if (banner != null) banner.setVisibility(bgMode == 1 ? View.VISIBLE : View.GONE);
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

        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        activity.getWindow().setAttributes(params);

        if (timeoutMs > 0) {
            sleepRunnable = () -> {
                try {
                    android.content.Intent i = new android.content.Intent(activity, ScreensaverActivity.class);
                    i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(i);
                } catch (Exception ignored) {}
            };
            sleepHandler.postDelayed(sleepRunnable, timeoutMs);
        }
    }
}
