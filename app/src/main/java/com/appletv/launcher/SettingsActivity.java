package com.appletv.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class SettingsActivity extends Activity {

    private ImageView dynamicLeftIcon;
    private LinearLayout listContainer;
    private TextView titleView;
    private SharedPreferences prefs;

    private enum MenuState { MAIN, GENERAL, APPEARANCE, HOME_SCREEN, SYSTEM, ABOUT, SLEEP_OPTIONS }
    private MenuState currentState = MenuState.MAIN;
    private int currentLeftIconId = -1;

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

    private void updateLeftIcon(int resId) {
        if (currentLeftIconId == resId || dynamicLeftIcon == null) return;
        currentLeftIconId = resId;
        dynamicLeftIcon.animate().alpha(0f).setDuration(120).withEndAction(() -> {
            dynamicLeftIcon.setImageResource(resId);
            dynamicLeftIcon.animate().alpha(1f).setDuration(120).start();
        }).start();
    }

    // ==================== ГЛАВНОЕ МЕНЮ ====================
    private void loadMainMenu() {
        currentState = MenuState.MAIN;
        titleView.setText("Настройки");
        listContainer.removeAllViews();
        currentLeftIconId = -1;

        addItem("Основные", "", R.drawable.ic_panel_info, this::loadGeneralMenu, true);
        addItem("Оформление", "", R.drawable.ic_panel_appearance, this::loadAppearanceMenu, true);
        addItem("Главный экран", "", R.drawable.ic_panel_home, this::loadHomeScreenMenu, true);
        addItem("Система", "", R.drawable.ic_panel_system, this::loadSystemMenu, true);
        addItem("О лаунчере", "", R.drawable.ic_panel_info, this::loadAboutMenu, true);

        requestFirstFocus();
    }

    // ==================== ОСНОВНЫЕ ====================
    private void loadGeneralMenu() {
        currentState = MenuState.GENERAL;
        titleView.setText("Основные");
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_info);

        int timeFormat = prefs.getInt("time_format", 0);
        String[] timeOptions = {"24 часа", "12 часов"};
        addItem("Формат времени", timeOptions[timeFormat], R.drawable.ic_panel_info, () -> {
            int next = (timeFormat + 1) % timeOptions.length;
            prefs.edit().putInt("time_format", next).apply();
            SettingsManager.applyTimeFormat(this);
            loadGeneralMenu();
            showToast("Формат времени изменен");
        }, true);

        int sleepTime = prefs.getInt("sleep_timeout", 0);
        String[] sleepLabels = {"Отключено", "5 минут", "15 минут", "30 минут"};
        addItem("Заставка (Спящий режим)", sleepLabels[sleepTime], R.drawable.ic_panel_info, this::loadSleepOptionsMenu, true);

        addItem("Лаунчер по умолчанию", "", R.drawable.ic_panel_info, () -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_HOME_SETTINGS)); }
            catch (Exception e) {
                try { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); }
                catch (Exception ex) { showToast("Не удалось открыть настройки"); }
            }
        }, true);

        requestFirstFocus();
    }

    private void loadSleepOptionsMenu() {
        currentState = MenuState.SLEEP_OPTIONS;
        titleView.setText("Спящий режим");
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_info);

        int currentSleep = prefs.getInt("sleep_timeout", 0);
        String[] sleepLabels = {"Отключено", "5 минут", "15 минут", "30 минут"};

        for (int i = 0; i < sleepLabels.length; i++) {
            final int idx = i;
            addItem(sleepLabels[i], currentSleep == idx ? "✓" : "", R.drawable.ic_panel_info, () -> {
                prefs.edit().putInt("sleep_timeout", idx).apply();
                SettingsManager.startSleepTimer(this);
                loadSleepOptionsMenu();
                showToast("Таймер заставки изменен");
            }, false);
        }
        requestFirstFocus();
    }

    // ==================== ОФОРМЛЕНИЕ ====================
    private void loadAppearanceMenu() {
        currentState = MenuState.APPEARANCE;
        titleView.setText("Оформление");
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_appearance);

        int bgMode = prefs.getInt("bg_mode", 0);
        String[] bgLabels = {"Подсветка (ambient)", "Кинематографичные баннеры"};
        addItem("Фон главного экрана", bgLabels[bgMode], R.drawable.ic_panel_appearance, () -> {
            int next = (bgMode + 1) % bgLabels.length;
            prefs.edit().putInt("bg_mode", next).apply();
            SettingsManager.applyBackgroundMode(this);
            loadAppearanceMenu();
            showToast("Фон изменен: " + bgLabels[next]);
        }, true);

        // Масштаб интерфейса: 0.85 / 1.0 / 1.15
        float scale = prefs.getFloat("ui_scale", 1.0f);
        String scaleLabel;
        if (scale < 0.95f) scaleLabel = "Мелкий (85%)";
        else if (scale > 1.05f) scaleLabel = "Крупный (115%)";
        else scaleLabel = "Стандартный (100%)";

        addItem("Масштаб интерфейса", scaleLabel, R.drawable.ic_panel_appearance, () -> {
            float next;
            if (scale < 0.95f) next = 1.0f;
            else if (scale < 1.05f) next = 1.15f;
            else next = 0.85f;
            prefs.edit().putFloat("ui_scale", next).apply();
            loadAppearanceMenu();
            showToast("Масштаб: " + Math.round(next * 100) + "% (применится на главном экране)");
        }, true);

        requestFirstFocus();
    }

    // ==================== ГЛАВНЫЙ ЭКРАН ====================
    private void loadHomeScreenMenu() {
        currentState = MenuState.HOME_SCREEN;
        titleView.setText("Главный экран");
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_home);

        boolean showClock = prefs.getBoolean("show_clock", true);
        addItem("Показывать часы", showClock ? "Вкл" : "Выкл", R.drawable.ic_panel_home, () -> {
            boolean next = !prefs.getBoolean("show_clock", true);
            prefs.edit().putBoolean("show_clock", next).apply();
            SettingsManager.applyClockVisibility(this);
            loadHomeScreenMenu();
            showToast(next ? "Часы включены" : "Часы скрыты");
        }, true);

        addItem("Сбросить избранное (док)", "", R.drawable.ic_panel_home, () -> {
            prefs.edit().putString("favorites", MainActivity.VIRTUAL_SETTINGS_PKG)
                    .putBoolean("settings_injected_v2", true).apply();
            showToast("Избранное сброшено. Перезапустите лаунчер.");
        }, true);

        requestFirstFocus();
    }

    // ==================== СИСТЕМА ====================
    private void loadSystemMenu() {
        currentState = MenuState.SYSTEM;
        titleView.setText("Система");
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_system);

        addItem("Настройки Android TV", "", R.drawable.ic_panel_system, () -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); }
            catch (Exception e) { showToast("Не удалось открыть"); }
        }, true);

        addItem("Язык", "", R.drawable.ic_panel_system, () -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS)); }
            catch (Exception e) { showToast("Не удалось открыть"); }
        }, true);

        addItem("Сброс всех настроек", "", R.drawable.ic_panel_system, () -> {
            prefs.edit().clear().apply();
            showToast("Настройки сброшены. Перезапустите лаунчер.");
            loadMainMenu();
        }, true);

        requestFirstFocus();
    }

    // ==================== О ЛАУНЧЕРЕ ====================
    private void loadAboutMenu() {
        currentState = MenuState.ABOUT;
        titleView.setText("О лаунчере");
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_info);

        addItem("Версия лаунчера: 1.0", "", R.drawable.ic_panel_info, null, false);
        addItem("Сборка: Apple TV Edition", "", R.drawable.ic_panel_info, null, false);
        addItem("Разработчик: iEFART", "", R.drawable.ic_panel_info, null, false);
        addItem("Проверить обновления", "", R.drawable.ic_panel_info, () -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(android.net.Uri.parse("https://github.com/z7413130-a11y/tvos-launcher2"));
                startActivity(i);
            } catch (Exception e) { showToast("Нет браузера на устройстве"); }
        }, true);

        requestFirstFocus();
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ ====================
    private void requestFirstFocus() {
        listContainer.post(() -> {
            if (listContainer.getChildCount() > 0) listContainer.getChildAt(0).requestFocus();
        });
    }

    private void showToast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    private void addItem(String text, String value, int iconResId, Runnable onClick, boolean hasArrow) {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48));
        rowParams.setMargins(0, 0, 0, dpToPx(6));
        row.setLayoutParams(rowParams);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        if (onClick == null) { row.setFocusable(false); row.setClickable(false); }
        else { row.setFocusable(true); row.setClickable(true); row.setOnClickListener(v -> onClick.run()); }

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
            row.addView(valView);
        }

        TextView arrow = new TextView(this);
        arrow.setText(">");
        arrow.setTextColor(Color.parseColor("#777777"));
        arrow.setTextSize(16f);
        if (!hasArrow) arrow.setVisibility(View.GONE);
        row.addView(arrow);

        row.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                row.setBackgroundResource(R.drawable.bg_settings_row_focused);
                label.setTextColor(Color.BLACK);
                arrow.setTextColor(Color.BLACK);
                row.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start();
                if (currentState == MenuState.MAIN) updateLeftIcon(iconResId);
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
        if (currentState == MenuState.MAIN) super.onBackPressed();
        else loadMainMenu();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        SettingsManager.startSleepTimer(this);
    }

    private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density + 0.5f); }
}
