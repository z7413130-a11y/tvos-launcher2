package com.appletv.launcher;

import android.app.Activity;
import android.content.Intent;
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

    // Состояния меню (где мы сейчас находимся)
    private enum MenuState { MAIN, SYSTEM, ABOUT }
    private MenuState currentState = MenuState.MAIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dynamicLeftIcon = findViewById(R.id.dynamic_left_icon);
        listContainer = findViewById(R.id.settings_list_container);
        titleView = findViewById(R.id.settings_title);

        // При запуске загружаем главное меню
        loadMainMenu();
    }

    private void loadMainMenu() {
        currentState = MenuState.MAIN;
        titleView.setText("Настройки");
        listContainer.removeAllViews(); // Очищаем старый список

        // Добавляем пункты с их иконками и действиями
        addItem("Основные", R.drawable.ic_panel_info, () -> showToast("В разработке"), true);
        addItem("Оформление", R.drawable.ic_panel_appearance, () -> showToast("В разработке"), true);
        addItem("Главный экран", R.drawable.ic_panel_home, () -> showToast("В разработке"), true);
        addItem("Система", R.drawable.ic_panel_system, this::loadSystemMenu, true);
        addItem("О лаунчере", R.drawable.ic_panel_info, this::loadAboutMenu, true);
        
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_appearance);
        
        // Автоматически ставим фокус на первый пункт
        listContainer.post(() -> {
            if (listContainer.getChildCount() > 0) listContainer.getChildAt(0).requestFocus();
        });
    }

    private void loadSystemMenu() {
        currentState = MenuState.SYSTEM;
        titleView.setText("Система");
        listContainer.removeAllViews();

        addItem("Настройки Android TV", R.drawable.ic_panel_system, () -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            } catch (Exception e) {}
        }, true);
        addItem("Язык", R.drawable.ic_panel_system, () -> showToast("В разработке"), true);
        addItem("Сброс", R.drawable.ic_panel_system, () -> showToast("В разработке"), true);

        // В подменю левая иконка зафиксирована на шестеренке
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_system);

        listContainer.post(() -> {
            if (listContainer.getChildCount() > 0) listContainer.getChildAt(0).requestFocus();
        });
    }

    private void loadAboutMenu() {
        currentState = MenuState.ABOUT;
        titleView.setText("О лаунчере");
        listContainer.removeAllViews();

        // Пункты без стрелочек (hasArrow = false), так как это просто информация
        addItem("Версия лаунчера: 1.0", R.drawable.ic_panel_info, null, false);
        addItem("Сборка: Apple TV Edition", R.drawable.ic_panel_info, null, false);
        addItem("Разработчик: iEFART", R.drawable.ic_panel_info, null, false);
        addItem("Проверить обновления", R.drawable.ic_panel_info, () -> showToast("У вас последняя версия"), true);

        // Левая иконка зафиксирована на "i"
        dynamicLeftIcon.setImageResource(R.drawable.ic_panel_info);

        listContainer.post(() -> {
            if (listContainer.getChildCount() > 0) listContainer.getChildAt(0).requestFocus();
        });
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // Универсальный создатель строк
    private void addItem(String text, int iconResId, Runnable onClick, boolean hasArrow) {
        listContainer.addView(createSettingsItem(text, iconResId, onClick, hasArrow));
    }

    private View createSettingsItem(String text, int iconResId, Runnable onClick, boolean hasArrow) {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        rowParams.setMargins(0, 0, 0, dpToPx(6)); 
        row.setLayoutParams(rowParams);
        
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        // Если у пункта нет действия (просто текст версии), отключаем его кликабельность
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

        TextView arrow = new TextView(this);
        arrow.setText(">");
        arrow.setTextColor(Color.parseColor("#777777"));
        arrow.setTextSize(16f);
        arrow.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        
        // Прячем стрелочку, если это информационный пункт
        if (!hasArrow) {
            arrow.setVisibility(View.GONE);
        }
        row.addView(arrow);

        row.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                row.setBackgroundResource(R.drawable.bg_settings_row_focused);
                label.setTextColor(Color.BLACK);
                arrow.setTextColor(Color.BLACK);
                row.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start();
                
                // Левая иконка динамически меняется ТОЛЬКО в главном меню
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

        return row;
    }

    // Умная кнопка НАЗАД
    @Override
    public void onBackPressed() {
        // Если мы находимся в каком-то подразделе, возвращаемся в главное меню настроек
        if (currentState != MenuState.MAIN) {
            loadMainMenu();
        } else {
            // Если мы уже в главном меню, закрываем Настройки
            super.onBackPressed();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
