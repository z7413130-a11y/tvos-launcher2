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

    private enum MenuState { MAIN, GENERAL, SYSTEM, ABOUT, SLEEP_OPTIONS }
    private MenuState currentState = MenuState.MAIN;
    private int currentLeftIconId = -1; // Кэш текущей иконки, чтобы не мерцало

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
        if (currentLeftIconId == resId) return;
        currentLeftIconId = resId;
        
        dynamicLeftIcon.animate().alpha(0f).setDuration(120).withEndAction(() -> {
            dynamicLeftIcon.setImageResource(resId);
            dynamicLeftIcon.animate().alpha(1f).setDuration(120).start();
        }).start();
    }

    private void loadMainMenu() {
        currentState = MenuState.MAIN;
        titleView.setText("Настройки");
        listContainer.removeAllViews();
        currentLeftIconId = -1; // Сброс при возврате в главное меню

        addItem("Основные", "", R.drawable.ic_panel_info, this::loadGeneralMenu, true);
        addItem("Оформление", "", R.drawable.ic_panel_appearance, () -> showToast("В разработке"), true);
        addItem("Главный экран", "", R.drawable.ic_panel_home, () -> showToast("В разработке"), true);
        addItem("Система", "", R.drawable.ic_panel_system, this::loadSystemMenu, true);
        addItem("О лаунчере", "", R.drawable.ic_panel_info, this::loadAboutMenu, true);
        
        requestFirstFocus();
    }

    // --- ОСНОВНЫЕ ---
    private void loadGeneralMenu() {
        currentState = MenuState.GENERAL;
        titleView.setText("Основные");
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_info);

        int timeFormat = prefs.getInt("time_format", 0);
        String[] timeOptions = {"24 часа", "12 часов"};
        addItem("Формат времени", timeOptions[timeFormat], R.drawable.ic_panel_info, () -> {
            int nextFormat = (timeFormat + 1) % timeOptions.length;
            prefs.edit().putInt("time_format", nextFormat).apply();
            loadGeneralMenu();
            showToast("Формат времени изменен");
        }, true);

        int sleepTime = prefs.getInt("sleep_timeout", 0);
        String[] sleepLabels = {"Отключено", "5 минут", "15 минут", "30 минут"};
        addItem("Заставка (Спящий режим)", sleepLabels[sleepTime], R.drawable.ic_panel_info, this::loadSleepOptionsMenu, true);

        addItem("Лаунчер по умолчанию", "", R.drawable.ic_panel_info, () -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_HOME_SETTINGS)); } 
            catch (Exception e) { try { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); } catch (Exception ex) {} }
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
                loadSleepOptionsMenu();
                showToast("Таймер заставки изменен");
            }, false);
        }
        requestFirstFocus();
    }

    // --- СИСТЕМА ---
    private void loadSystemMenu() {
        currentState = MenuState.SYSTEM;
        titleView.setText("Система");
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_system);

        addItem("Настройки Android TV", "", R.drawable.ic_panel_system, () -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); } catch (Exception e) {}
        }, true);
        
        addItem("Язык", "", R.drawable.ic_panel_system, () -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS)); } 
            catch (Exception e) { showToast("Открытие настроек языка..."); }
        }, true);
        
        addItem("Сброс", "", R.drawable.ic_panel_system, () -> showToast("В разработке"), true);
        
        requestFirstFocus();
    }

    // --- О ЛАУНЧЕРЕ ---
    private void loadAboutMenu() {
        currentState = MenuState.ABOUT;
        titleView.setText("О лаунчере");
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_info);

        addItem("Версия лаунчера: 1.0", "", R.drawable.ic_panel_info, null, false);
        addItem("Сборка: Apple TV Edition", "", R.drawable.ic_panel_info, null, false);
        addItem("Разработчик: iEFART", "", R.drawable.ic_panel_info, null, false);
        addItem("Проверить обновления", "", R.drawable.ic_panel_info, () -> showToast("В разработке"), true);

        requestFirstFocus();
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---
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
                
                // ВОТ ОН, ИСПРАВЛЕННЫЙ МЕХАНИЗМ СМЕНЫ КАРТИНКИ
                if (currentState == MenuState.MAIN) {
                    updateLeftIcon(iconResId);
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
        if (currentState == MenuState.SLEEP_OPTIONS || currentState == MenuState.GENERAL || currentState == MenuState.SYSTEM || currentState == MenuState.ABOUT) loadMainMenu();
        else if (currentState != MenuState.MAIN) loadMainMenu();
        else super.onBackPressed();
    }

    private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density + 0.5f); }
}
