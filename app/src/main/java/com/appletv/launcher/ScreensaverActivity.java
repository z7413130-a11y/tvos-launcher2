package com.appletv.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ScreensaverActivity extends Activity {

    private LinearLayout clockContainer;
    private TextView dateView;
    private final Random random = new Random();
    private boolean isRunning = true;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screensaver);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        clockContainer = findViewById(R.id.screensaver_clock_container);
        dateView = findViewById(R.id.screensaver_date);

        updateDate();
        startFloating();
    }

    private void updateDate() {
        String pattern = "en".equals(LocaleHelper.getLanguage(this))
                ? "EEEE, d MMMM" : "EEEE, d MMMM";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, new Locale(LocaleHelper.getLanguage(this)));
        dateView.setText(sdf.format(new Date()));
    }

    /** Плавно двигает блок часов по экрану туда-обратно. */
    private void startFloating() {
        if (!isRunning) return;

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;

        int marginX = (int) (screenW * 0.15f);
        int marginY = (int) (screenH * 0.15f);

        float startX = clockContainer.getTranslationX();
        float startY = clockContainer.getTranslationY();

        float targetX = startX + (random.nextFloat() * 2 - 1) * marginX;
        float targetY = startY + (random.nextFloat() * 2 - 1) * marginY;

        long duration = 12000 + random.nextInt(6000);

        ObjectAnimator animX = ObjectAnimator.ofFloat(clockContainer, View.TRANSLATION_X, startX, targetX);
        ObjectAnimator animY = ObjectAnimator.ofFloat(clockContainer, View.TRANSLATION_Y, startY, targetY);
        animX.setDuration(duration);
        animY.setDuration(duration);
        animX.setInterpolator(new LinearInterpolator());
        animY.setInterpolator(new LinearInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animX, animY);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isRunning) startFloating();
            }
        });
        set.start();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }
}
