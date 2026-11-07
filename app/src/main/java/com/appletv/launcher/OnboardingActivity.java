package com.appletv.launcher;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OnboardingActivity extends Activity {

    private static final String PREFS = "tvos_prefs";
    private static final String KEY_DONE = "onboarding_done";
    private static final int TOTAL_STEPS = 3;

    private TextView titleView;
    private TextView subtitleView;
    private Button actionButton;
    private LinearLayout dotsContainer;
    private View contentContainer;

    private int step = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_DONE, false)) {
            finish();
            return;
        }

        titleView = findViewById(R.id.onboarding_title);
        subtitleView = findViewById(R.id.onboarding_subtitle);
        actionButton = findViewById(R.id.onboarding_button);
        dotsContainer = findViewById(R.id.onboarding_dots);
        contentContainer = findViewById(R.id.onboarding_content);

        buildDots();
        showStep(0, true);

        // Плавный фокус на кнопке (Apple TV style)
        actionButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actionButton.animate()
                        .scaleX(1.08f).scaleY(1.08f)
                        .setDuration(220)
                        .setInterpolator(new OvershootInterpolator(1.4f))
                        .start();
            } else {
                actionButton.animate()
                        .scaleX(1.0f).scaleY(1.0f)
                        .setDuration(180)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        });

        actionButton.setOnClickListener(v -> {
            // Лёгкий «нажим» перед переходом
            actionButton.animate()
                    .scaleX(0.94f).scaleY(0.94f)
                    .setDuration(90)
                    .withEndAction(() -> {
                        actionButton.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start();
                        handleNext();
                    }).start();
        });
    }

    private void handleNext() {
        if (step == 1) {
            openHomePicker();
        }
        if (step < TOTAL_STEPS - 1) {
            showStep(step + 1, false);
        } else {
            finishOnboarding();
        }
    }

    private void buildDots() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < TOTAL_STEPS; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(8), dp(8));
            lp.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.status_pill_bg);
            dot.setAlpha(0.3f);
            dotsContainer.addView(dot);
        }
        updateDots(true);
    }

    private void updateDots(boolean animate) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            float target = i == step ? 1f : 0.3f;
            if (animate) {
                dot.animate().alpha(target).setDuration(320).start();
                // Активная точка чуть крупнее
                float scale = i == step ? 1.35f : 1.0f;
                dot.animate().scaleX(scale).scaleY(scale).setDuration(320).start();
            } else {
                dot.setAlpha(target);
                dot.setScaleX(i == step ? 1.35f : 1.0f);
                dot.setScaleY(i == step ? 1.35f : 1.0f);
            }
        }
    }

    /** Плавная смена шага: fade-out -> смена текста -> fade-in + slide-up */
    private void showStep(int newStep, boolean firstShow) {
        step = newStep;

        Runnable applyText = () -> {
            if (step == 0) {
                titleView.setText("Привет!");
                subtitleView.setText("Представляю вашему вниманию лаунчер\n\nApplineTV\n\nСтильный, быстрый и удобный лаунчер для вашего телевизора.");
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
            updateDots(true);
        };

        if (firstShow) {
            // Первый показ: сразу текст + плавное появление всего блока
            applyText.run();
            titleView.setAlpha(0f);
            subtitleView.setAlpha(0f);
            actionButton.setAlpha(0f);
            contentContainer.setAlpha(1f);

            AnimatorSet set = new AnimatorSet();
            ObjectAnimator t = ObjectAnimator.ofFloat(titleView, View.ALPHA, 0f, 1f);
            t.setDuration(500);
            ObjectAnimator s = ObjectAnimator.ofFloat(subtitleView, View.ALPHA, 0f, 1f);
            s.setDuration(600);
            s.setStartDelay(120);
            ObjectAnimator b = ObjectAnimator.ofFloat(actionButton, View.ALPHA, 0f, 1f);
            b.setDuration(500);
            b.setStartDelay(260);

            set.playTogether(t, s, b);
            set.setInterpolator(new DecelerateInterpolator());
            set.start();

            actionButton.post(actionButton::requestFocus);
            return;
        }

        // Смена шага: fade-out старого контента
        AnimatorSet fadeOut = new AnimatorSet();
        ObjectAnimator outTitle = ObjectAnimator.ofFloat(titleView, View.ALPHA, 1f, 0f);
        ObjectAnimator outSub = ObjectAnimator.ofFloat(subtitleView, View.ALPHA, 1f, 0f);
        ObjectAnimator outBtn = ObjectAnimator.ofFloat(actionButton, View.ALPHA, 1f, 0f);
        fadeOut.playTogether(outTitle, outSub, outBtn);
        fadeOut.setDuration(180);
        fadeOut.setInterpolator(new DecelerateInterpolator());

        fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                applyText.run();

                // Подготовка: лёгкий сдвиг вниз + прозрачность
                float offset = dp(24);
                titleView.setTranslationY(offset);
                subtitleView.setTranslationY(offset);
                actionButton.setTranslationY(offset);

                // Появление: fade-in + slide-up (Apple TV style)
                AnimatorSet fadeIn = new AnimatorSet();
                ObjectAnimator inTitle = ObjectAnimator.ofFloat(titleView, View.ALPHA, 0f, 1f);
                ObjectAnimator tyTitle = ObjectAnimator.ofFloat(titleView, View.TRANSLATION_Y, offset, 0f);
                ObjectAnimator inSub = ObjectAnimator.ofFloat(subtitleView, View.ALPHA, 0f, 1f);
                ObjectAnimator tySub = ObjectAnimator.ofFloat(subtitleView, View.TRANSLATION_Y, offset, 0f);
                ObjectAnimator inBtn = ObjectAnimator.ofFloat(actionButton, View.ALPHA, 0f, 1f);
                ObjectAnimator tyBtn = ObjectAnimator.ofFloat(actionButton, View.TRANSLATION_Y, offset, 0f);

                inTitle.setDuration(400);
                tyTitle.setDuration(400);
                inSub.setDuration(450);
                tySub.setDuration(450);
                inSub.setStartDelay(60);
                tySub.setStartDelay(60);
                inBtn.setDuration(420);
                tyBtn.setDuration(420);
                inBtn.setStartDelay(140);
                tyBtn.setStartDelay(140);

                fadeIn.playTogether(inTitle, tyTitle, inSub, tySub, inBtn, tyBtn);
                fadeIn.setInterpolator(new DecelerateInterpolator(1.4f));
                fadeIn.start();

                actionButton.post(actionButton::requestFocus);
            }
        });
        fadeOut.start();
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

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (step > 0) {
            showStep(step - 1, false);
        }
        // На шаге 0 назад не выходим
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
