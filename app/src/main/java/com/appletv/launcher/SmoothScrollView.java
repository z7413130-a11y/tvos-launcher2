package com.appletv.launcher;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ScrollView;

public class SmoothScrollView extends ScrollView {

    public SmoothScrollView(Context context) {
        super(context);
    }

    public SmoothScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmoothScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // Перехватываем смену фокуса и заменяем резкий прыжок на плавную анимацию
    @Override
    public void requestChildFocus(View child, View focused) {
        if (focused != null) {
            Rect rect = new Rect();
            focused.getDrawingRect(rect);
            offsetDescendantRectToMyCoords(focused, rect);
            int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
            if (delta != 0) {
                ObjectAnimator anim = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), getScrollY() + delta);
                anim.setDuration(300);
                anim.setInterpolator(new DecelerateInterpolator(2.0f)); // 2.0f даст резвый старт и очень мягкое замедление
                anim.start();
            }
        }
    }

    // Блокируем любые системные попытки моментально прокрутить экран
    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        int delta = computeScrollDeltaToGetChildRectOnScreen(rectangle);
        if (delta != 0) {
            ObjectAnimator anim = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), getScrollY() + delta);
            anim.setDuration(300);
            anim.setInterpolator(new DecelerateInterpolator(2.0f));
            anim.start();
        }
        return true; 
    }
}
