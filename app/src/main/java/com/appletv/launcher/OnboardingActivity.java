package com.appletv.launcher;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
    private int step = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_DONE, false)) { finish(); return; }
        titleView = findViewById(R.id.onboarding_title);
        subtitleView = findViewById(R.id.onboarding_subtitle);
        actionButton = findViewById(R.id.onboarding_button);
        dotsContainer = findViewById(R.id.onboarding_dots);
        buildDots();
        showStep(0, true);
        actionButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) actionButton.animate().scaleX(1.08f).scaleY(1.08f).setDuration(220).setInterpolator(new OvershootInterpolator(1.4f)).start();
            else actionButton.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
        });
        actionButton.setOnClickListener(v -> {
            actionButton.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).withEndAction(() -> {
                actionButton.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start();
                if (step == 1) openHomePicker();
                if (step < TOTAL_STEPS - 1) showStep(step + 1, false);
                else finishOnboarding();
            }).start();
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
            dot.setAlpha(0.3f);
            dotsContainer.addView(dot);
        }
        updateDots(false);
    }

    private void updateDots(boolean animate) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            float target = i == step ? 1f : 0.3f;
            float scale = i == step ? 1.35f : 1.0f;
            if (animate) { dot.animate().alpha(target).setDuration(320).start(); dot.animate().scaleX(scale).scaleY(scale).setDuration(320).start(); }
            else { dot.setAlpha(target); dot.setScaleX(scale); dot.setScaleY(scale); }
        }
    }

    private void showStep(int newStep, boolean firstShow) {
        step = newStep;
        Runnable applyText = () -> {
            if (step == 0) { titleView.setText(R.string.onb_welcome_title); subtitleView.setText(R.string.onb_welcome_subtitle); actionButton.setText(R.string.onb_next); }
            else if (step == 1) { titleView.setText(R.string.onb_default_title); subtitleView.setText(R.string.onb_default_subtitle); actionButton.setText(R.string.onb_next); }
            else { titleView.setText(R.string.onb_beta_title); subtitleView.setText(R.string.onb_beta_subtitle); actionButton.setText(R.string.onb_start); }
            updateDots(true);
        };
        if (firstShow) {
            applyText.run();
            titleView.setAlpha(0f); subtitleView.setAlpha(0f); actionButton.setAlpha(0f);
            AnimatorSet set = new AnimatorSet();
            ObjectAnimator t = ObjectAnimator.ofFloat(titleView, View.ALPHA, 0f, 1f); t.setDuration(500);
            ObjectAnimator s = ObjectAnimator.ofFloat(subtitleView, View.ALPHA, 0f, 1f); s.setDuration(600); s.setStartDelay(120);
            ObjectAnimator b = ObjectAnimator.ofFloat(actionButton, View.ALPHA, 0f, 1f); b.setDuration(500); b.setStartDelay(260);
            set.playTogether(t, s, b); set.setInterpolator(new DecelerateInterpolator()); set.start();
            actionButton.post(actionButton::requestFocus);
            return;
        }
        AnimatorSet fadeOut = new AnimatorSet();
        fadeOut.playTogether(
            ObjectAnimator.ofFloat(titleView, View.ALPHA, 1f, 0f),
            ObjectAnimator.ofFloat(subtitleView, View.ALPHA, 1f, 0f),
            ObjectAnimator.ofFloat(actionButton, View.ALPHA, 1f, 0f)
        );
        fadeOut.setDuration(180); fadeOut.setInterpolator(new DecelerateInterpolator());
        fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                applyText.run();
                float o = dp(24);
                titleView.setTranslationY(o); subtitleView.setTranslationY(o); actionButton.setTranslationY(o);
                AnimatorSet in = new AnimatorSet();
                ObjectAnimator inT = ObjectAnimator.ofFloat(titleView, View.ALPHA, 0f, 1f); inT.setDuration(400);
                ObjectAnimator tyT = ObjectAnimator.ofFloat(titleView, View.TRANSLATION_Y, o, 0f); tyT.setDuration(400);
                ObjectAnimator inS = ObjectAnimator.ofFloat(subtitleView, View.ALPHA, 0f, 1f); inS.setDuration(450); inS.setStartDelay(60);
                ObjectAnimator tyS = ObjectAnimator.ofFloat(subtitleView, View.TRANSLATION_Y, o, 0f); tyS.setDuration(450); tyS.setStartDelay(60);
                ObjectAnimator inB = ObjectAnimator.ofFloat(actionButton, View.ALPHA, 0f, 1f); inB.setDuration(420); inB.setStartDelay(140);
                ObjectAnimator tyB = ObjectAnimator.ofFloat(actionButton, View.TRANSLATION_Y, o, 0f); tyB.setDuration(420); tyB.setStartDelay(140);
                in.playTogether(inT, tyT, inS, tyS, inB, tyB);
                in.setInterpolator(new DecelerateInterpolator(1.4f));
                in.start();
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
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (step > 0) showStep(step - 1, false);
        else finishOnboarding();
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
