package com.appletv.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class SettingsActivity extends Activity {

    private ImageView dynamicLeftIcon;
    private LinearLayout listContainer;
    private TextView titleView;
    private SharedPreferences prefs;

    private enum MenuState { MAIN, GENERAL, APPEARANCE, HOME, SYSTEM, ABOUT, SCALE_OPTIONS, THEME_OPTIONS, SLEEP_OPTIONS, HIDE_APPS }
    private MenuState currentState = MenuState.MAIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);
        
        dynamicLeftIcon = findViewById(R.id.dynamic_left_icon);
        listContainer = findViewById(R.id.settings_list_container);
        titleView = findViewById(R.id.settings_title);

        loadMainMenu();
    }

    private void loadMainMenu() {
        currentState = MenuState.MAIN;
        titleView.setText("Настройки");
        listContainer.removeAllViews();

        addItem("Основные", "", R.drawable.ic_panel_info, this::loadGeneralMenu, true);
        addItem("Оформление", "", R.drawable.ic_panel_appearance, this::loadAppearanceMenu, true);
        addItem("Главный экран", "", R.drawable.ic_panel_home, this::loadHomeMenu, true);
        addItem("Система", "", R.drawable.ic_panel_system, this::loadSystemMenu, true);
        addItem("О лаунчере", "", R.drawable.ic_panel_info, this::loadAboutMenu, true);
        
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_appearance);
        requestFirstFocus();
    }

    // --- 1. ОСНОВНЫЕ ---
    private void loadGeneralMenu() {
        currentState = MenuState.GENERAL;
        titleView.setText("Основные");
        listContainer.removeAllViews();
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_info);

        int timeFormat = prefs.getInt("time_format", 0);
        String[] timeOptions = {"24 часа", "12 часов"};
        addItem("Формат времени", timeOptions[timeFormat], R.drawable.ic_panel_info, () -> {
            int nextFormat = (timeFormat + 1) % timeOptions.length;
            prefs.edit().putInt("time_format", nextFormat).apply();
            loadGeneralMenu();
        }, true);

        int sleepTime = prefs.getInt("sleep_timeout", 0);
        String[] sleepLabels = {"Отключено", "5 минут", "15 минут", "30 минут"};
        addItem("Заставка (Спящий режим)", sleepLabels[sleepTime], R.drawable.ic_panel_info, this::loadSleepOptionsMenu, true);

        addItem("Лаунчер по умолчанию", "", R.drawable.ic_panel_info, () -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_HOME_SETTINGS));
            } catch (Exception e) {
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                } catch (Exception ex) {
                    showToast("Не удалось открыть системные настройки");
                }
            }
        }, true);

        requestFirstFocus();
    }

    private void loadSleepOptionsMenu() {
        currentState = MenuState.SLEEP_OPTIONS;
        titleView.setText("Спящий режим");
        listContainer.removeAllViews();
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_info);

        int currentSleep = prefs.getInt("sleep_timeout", 0);
        String[] sleepLabels = {"Отключено", "5 минут", "15 минут", "30 минут"};

        for (int i = 0; i < sleepLabels.length; i++) {
            final int idx = i;
            addItem(sleepLabels[i], currentSleep == idx ? "✓" : "", R.drawable.ic_panel_info, () -> {
                prefs.edit().putInt("sleep_timeout", idx).apply();
                loadSleepOptionsMenu();
                showToast("Таймер заставки изменен");
            }, false);
        }
        requestFirstFocus();
    }

    // --- 2. ОФОРМЛЕНИЕ ---
    private void loadAppearanceMenu() {
        currentState = MenuState.APPEARANCE;
        titleView.setText("Оформление");
        listContainer.removeAllViews();
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_appearance);

        addItem("Масштаб интерфейса", "", R.drawable.ic_panel_appearance, this::loadScaleOptionsMenu, true);

        boolean glow = prefs.getBoolean("ambient_glow", true);
        addItem("Окружающее свечение", glow ? "Вкл" : "Выкл", R.drawable.ic_panel_appearance, () -> {
            prefs.edit().putBoolean("ambient_glow", !glow).apply();
            loadAppearanceMenu();
        }, true);

        int theme = prefs.getInt("theme", 0);
        String[] themeLabels = {"Темная", "Светлая", "Авто"};
        addItem("Тема", themeLabels[theme], R.drawable.ic_panel_appearance, this::loadThemeOptionsMenu, true);

        requestFirstFocus();
    }

    private void loadScaleOptionsMenu() {
        currentState = MenuState.SCALE_OPTIONS;
        titleView.setText("Масштаб интерфейса");
        listContainer.removeAllViews();
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_appearance);

        float currentScale = prefs.getFloat("ui_scale", 1.0f);

        addItem("Мелкий (0.8x)", currentScale == 0.8f ? "✓" : "", R.drawable.ic_panel_appearance, () -> {
            prefs.edit().putFloat("ui_scale", 0.8f).apply();
            loadScaleOptionsMenu();
            showToast("Масштаб изменен");
        }, false);

        addItem("Стандартный (1.0x)", currentScale == 1.0f ? "✓" : "", R.drawable.ic_panel_appearance, () -> {
            prefs.edit().putFloat("ui_scale", 1.0f).apply();
            loadScaleOptionsMenu();
            showToast("Масштаб изменен");
        }, false);

        addItem("Крупный (1.2x)", currentScale == 1.2f ? "✓" : "", R.drawable.ic_panel_appearance, () -> {
            prefs.edit().putFloat("ui_scale", 1.2f).apply();
            loadScaleOptionsMenu();
            showToast("Масштаб изменен");
        }, false);

        requestFirstFocus();
    }

    private void loadThemeOptionsMenu() {
        currentState = MenuState.THEME_OPTIONS;
        titleView.setText("Тема лаунчера");
        listContainer.removeAllViews();
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_appearance);

        int currentTheme = prefs.getInt("theme", 0);
        String[] themeLabels = {"Темная", "Светлая", "Авто (по времени)"};

        for (int i = 0; i < themeLabels.length; i++) {
            final int idx = i;
            addItem(themeLabels[i], currentTheme == idx ? "✓" : "", R.drawable.ic_panel_appearance, () -> {
                prefs.edit().putInt("theme", idx).apply();
                loadThemeOptionsMenu();
                showToast("Тема изменена");
            }, false);
        }
        requestFirstFocus();
    }

    // --- 3. ГЛАВНЫЙ ЭКРАН ---
    private void loadHomeMenu() {
        currentState = MenuState.HOME;
        titleView.setText("Главный экран");
        listContainer.removeAllViews();
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_home);

        addItem("Скрытие приложений", "", R.drawable.ic_panel_home, this::loadHideAppsMenu, true);

        addItem("Сброс избранного (Dock)", "", R.drawable.ic_panel_home, () -> {
            prefs.edit().putString("favorites", "").apply();
            showToast("Избранное в Dock очищено!");
        }, true);

        requestFirstFocus();
    }

    private void loadHideAppsMenu() {
        currentState = MenuState.HIDE_APPS;
        titleView.setText("Скрытие приложений");
        listContainer.removeAllViews();
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_home);

        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
        String hiddenStr = prefs.getString("hidden_apps", "");

        for (ResolveInfo ri : apps) {
            if (ri.activityInfo.packageName.equals(getPackageName())) continue;
            String pkg = ri.activityInfo.packageName;
            String label = ri.loadLabel(pm).toString();
            boolean isHidden = hiddenStr.contains(pkg);

            addItem(label, isHidden ? "Скрыто" : "Видно", R.drawable.ic_panel_home, () -> {
                String current = prefs.getString("hidden_apps", "");
                if (isHidden) {
                    prefs.edit().putString("hidden_apps", current.replace(pkg + ",", "")).apply();
                } else {
                    prefs.edit().putString("hidden_apps", current + pkg + ",").apply();
                }
                loadHideAppsMenu();
            }, false);
        }
        requestFirstFocus();
    }

    // --- 4. СИСТЕМА (вернули исходный вид с переходом в параметры системы) ---
    private void loadSystemMenu() {
        currentState = MenuState.SYSTEM;
        titleView.setText("Система");
        listContainer.removeAllViews();
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_system);

        addItem("Настройки Android TV", "", R.drawable.ic_panel_system, () -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            } catch (Exception e) {}
        }, true);
        addItem("Язык", "", R.drawable.ic_panel_system, () -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS));
            } catch (Exception e) {}
        }, true);

        requestFirstFocus();
    }

    // --- 5. О ЛАУНЧЕРЕ (вернули исходную информационную карточку) ---
    private void loadAboutMenu() {
        currentState = MenuState.ABOUT;
        titleView.setText("О лаунчере");
        listContainer.removeAllViews();
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_info);

        addItem("Версия лаунчера: 1.0", "", R.drawable.ic_panel_info, null, false);
        addItem("Сборка: Apple TV Edition", "", R.drawable.ic_panel_info, null, false);
        addItem("Разработчик: iEFART", "", R.drawable.ic_panel_info, null, false);

        requestFirstFocus();
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---

    private void requestFirstFocus() {
        listContainer.post(() -> {
            if (listContainer.getChildCount() > 0) listContainer.getChildAt(0).requestFocus();
        });
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void addItem(String text, String value, int iconResId, Runnable onClick, boolean hasArrow) {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48));
        rowParams.setMargins(0, 0, 0, dpToPx(6)); 
        row.setLayoutParams(rowParams);
        
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        if (onClick == null) {
            row.setFocusable(false);
            row.setClickable(false);
        } else {
            row.setFocusable(true);
            row.setClickable(true);
            row.setOnClickListener(v -> onClick.run());
        }
        
        row.setPadding(dpToPx(16), 0, dpToPx(16), 0);
        row.setBackgroundResource(R.drawable.bg_settings_row_normal);

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16f);
        label.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        row.addView(label, textParams);

        if (value != null && !value.isEmpty()) {
            TextView valView = new TextView(this);
            valView.setText(value);
            valView.setTextColor(Color.parseColor("#999999"));
            valView.setTextSize(14f);
            valView.setPadding(0, 0, dpToPx(12), 0);
            valView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            row.addView(valView);
        }

        TextView arrow = new TextView(this);
        arrow.setText(">");
        arrow.setTextColor(Color.parseColor("#777777"));
        arrow.setTextSize(16f);
        arrow.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        
        if (!hasArrow) arrow.setVisibility(View.GONE);
        row.addView(arrow);

        row.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                row.setBackgroundResource(R.drawable.bg_settings_row_focused);
                label.setTextColor(Color.BLACK);
                arrow.setTextColor(Color.BLACK);
                row.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start();
                
                if (currentState == MenuState.MAIN) {
                    dynamicLeftIcon.animate().alpha(0f).setDuration(100).withEndAction(() -> {
                        dynamicLeftIcon.setImageResource(iconResId);
                        dynamicLeftIcon.animate().alpha(1f).setDuration(100).start();
                    }).start();
                }
            } else {
                row.setBackgroundResource(R.drawable.bg_settings_row_normal);
                label.setTextColor(Color.WHITE);
                arrow.setTextColor(Color.parseColor("#777777"));
                row.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            }
        });

        listContainer.addView(row);
    }

    @Override
    public void onBackPressed() {
        if (currentState == MenuState.SCALE_OPTIONS || currentState == MenuState.THEME_OPTIONS || currentState == MenuState.SLEEP_OPTIONS) {
            loadAppearanceMenu();
        } else if (currentState == MenuState.HIDE_APPS) {
            loadHomeMenu();
        } else if (currentState != MenuState.MAIN) {
            loadMainMenu();
        } else {
            super.onBackPressed();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
