package com.appletv.launcher;

import android.app.Activity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dynamicLeftIcon = findViewById(R.id.dynamic_left_icon);
        LinearLayout listContainer = findViewById(R.id.settings_list_container);

        String[] sections = {"Оформление", "Главный экран", "Система", "О лаунчере"};
        int[] icons = {
            R.drawable.ic_panel_appearance,
            R.drawable.ic_panel_home,
            R.drawable.ic_panel_system,
            R.drawable.ic_panel_info
        };

        for (int i = 0; i < sections.length; i++) {
            listContainer.addView(createSettingsItem(sections[i], icons[i]));
        }
    }

    private View createSettingsItem(String text, int iconResId) {
        LinearLayout row = new LinearLayout(this);
        // Задаем отступы снизу для каждой полоски (чтобы они не слипались)
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(65));
        rowParams.setMargins(0, 0, 0, dpToPx(8)); 
        row.setLayoutParams(rowParams);
        
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setPadding(dpToPx(24), 0, dpToPx(24), 0);
        
        // По умолчанию ставим темно-серый фон
        row.setBackgroundResource(R.drawable.bg_settings_row_normal);

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Color.WHITE);
        label.setTextSize(20f);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        row.addView(label, textParams);

        TextView arrow = new TextView(this);
        arrow.setText(">");
        arrow.setTextColor(Color.parseColor("#888888"));
        arrow.setTextSize(22f);
        arrow.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        row.addView(arrow);

        row.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                row.setBackgroundResource(R.drawable.bg_settings_row_focused);
                label.setTextColor(Color.BLACK);
                arrow.setTextColor(Color.BLACK);
                row.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start();
                
                // Динамически меняем иконку в левом квадрате с плавной анимацией
                dynamicLeftIcon.animate().alpha(0f).setDuration(100).withEndAction(() -> {
                    dynamicLeftIcon.setImageResource(iconResId);
                    dynamicLeftIcon.animate().alpha(1f).setDuration(100).start();
                }).start();

            } else {
                row.setBackgroundResource(R.drawable.bg_settings_row_normal);
                label.setTextColor(Color.WHITE);
                arrow.setTextColor(Color.parseColor("#888888"));
                row.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            }
        });

        row.setOnClickListener(v -> {
            Toast.makeText(this, "Открываем: " + text, Toast.LENGTH_SHORT).show();
        });

        return row;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
