package com.appletv.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OnboardingActivity extends Activity {

    private static final String PREFS = "tvos_prefs";
    private static final String KEY_DONE = "onboarding_done";

    private TextView titleView;
    private TextView subtitleView;
    private Button actionButton;
    private LinearLayout dotsContainer;

    private int step = 0; // 0, 1, 2
    private static final int TOTAL_STEPS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Если уже проходили — сразу закрываем
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_DONE, false)) {
            finish();
            return;
        }

        titleView = findViewById(R.id.onboarding_title);
        subtitleView = findViewById(R.id.onboarding_subtitle);
        actionButton = findViewById(R.id.onboarding_button);
        dotsContainer = findViewById(R.id.onboarding_dots);

        buildDots();
        showStep(0);

        actionButton.setOnClickListener(v -> {
            if (step == 1) {
                // На шаге 1 (лаунчер по умолчанию) — вызываем системный выбор HOME
                openHomePicker();
            }
            if (step < TOTAL_STEPS - 1) {
                showStep(step + 1);
            } else {
                finishOnboarding();
            }
        });
    }

    private void buildDots() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < TOTAL_STEPS; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(8), dp(8));
            lp.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.status_pill_bg);
            dotsContainer.addView(dot);
        }
        updateDots();
    }

    private void updateDots() {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            dot.setAlpha(i == step ? 1f : 0.3f);
        }
    }

    private void showStep(int newStep) {
        step = newStep;
        updateDots();

        if (step == 0) {
            titleView.setText("Привет!");
            subtitleView.setText("Представляю вашему вниманию лаунчер\n\nApplineTV\n\nСтильный, быстрый и удобный дом для вашего телевизора.");
            actionButton.setText("Далее");
        } else if (step == 1) {
            titleView.setText("Лаунчер по умолчанию");
            subtitleView.setText("Чтобы ApplineTV запускался при нажатии кнопки «Домой», сделайте его лаунчером по умолчанию.\n\nНажмите «Далее», чтобы открыть системное меню выбора, или пропустите этот шаг.");
            actionButton.setText("Далее");
        } else {
            titleView.setText("Бета-версия");
            subtitleView.setText("⚠️ Это бета-версия ApplineTV.\n\nВозможны баги, недочёты и нестабильная работа. Мы активно дорабатываем лаунчер — спасибо, что тестируете!\n\nНажмите «Начать», чтобы перейти на главный экран.");
            actionButton.setText("Начать");
        }

        // Фокус на кнопку, чтобы пульт сразу мог нажать
        actionButton.post(actionButton::requestFocus);
    }

    private void openHomePicker() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        } catch (Exception ignored) {}
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DONE, true).apply();

        // Переходим на главный экран
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // На бета-шаге назад = завершить онбординг; на других — назад на шаг
        if (step > 0) {
            showStep(step - 1);
        } else {
            // С первого шага назад не выходим — иначе лаунчер не покажет онбординг снова
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
