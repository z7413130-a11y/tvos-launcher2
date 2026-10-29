package com.appletv.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        LinearLayout listContainer = findViewById(R.id.settings_list_container);

        String[] sections = {"Оформление", "Главный экран", "Система", "О лаунчере"};

        for (String section : sections) {
            listContainer.addView(createSettingsItem(section));
            // Линия-разделитель
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.parseColor("#333333"));
            listContainer.addView(divider);
        }
    }

    private View createSettingsItem(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(60)));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setPadding(dpToPx(20), 0, dpToPx(20), 0);

        // Скругленный фон для фокуса
        GradientDrawable focusBg = new GradientDrawable();
        focusBg.setCornerRadius(dpToPx(10));
        focusBg.setColor(Color.WHITE);
        
        row.setBackgroundColor(Color.TRANSPARENT);

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Color.WHITE);
        label.setTextSize(18f);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        row.addView(label, params);

        TextView arrow = new TextView(this);
        arrow.setText(">");
        arrow.setTextColor(Color.parseColor("#666666"));
        arrow.setTextSize(20f);
        row.addView(arrow);

        row.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                row.setBackground(focusBg);
                label.setTextColor(Color.BLACK);
                arrow.setTextColor(Color.BLACK);
                row.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start();
            } else {
                row.setBackgroundColor(Color.TRANSPARENT);
                label.setTextColor(Color.WHITE);
                arrow.setTextColor(Color.parseColor("#666666"));
                row.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            }
        });

        row.setOnClickListener(v -> {
            Toast.makeText(this, "Раздел: " + text, Toast.LENGTH_SHORT).show();
        });

        return row;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
