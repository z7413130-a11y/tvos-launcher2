package com.appletv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamicAppLoader {

    public static void loadApps(Activity activity, LinearLayout container) {
        if (container == null) return;
        container.removeAllViews();

        PackageManager pm = activity.getPackageManager();
        Map<String, ResolveInfo> uniqueApps = new HashMap<>();

        // ТВ-приложения
        Intent leanback = new Intent(Intent.ACTION_MAIN, null);
        leanback.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(leanback, 0)) {
            uniqueApps.put(ri.activityInfo.packageName, ri);
        }

        // Обычные приложения
        Intent launcher = new Intent(Intent.ACTION_MAIN, null);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(launcher, 0)) {
            if (!uniqueApps.containsKey(ri.activityInfo.packageName)) {
                uniqueApps.put(ri.activityInfo.packageName, ri);
            }
        }

        List<ResolveInfo> apps = new ArrayList<>(uniqueApps.values());
        Collections.sort(apps, (a, b) ->
                a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));

        int itemWidth = dp(activity, 120);

        for (ResolveInfo ri : apps) {
            if (ri.activityInfo.packageName.equals(activity.getPackageName())) continue;

            String label = ri.loadLabel(pm).toString();
            Drawable icon = ri.loadIcon(pm);

            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setFocusable(true);
            item.setClickable(true);
            item.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    itemWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(activity, 8), 0, dp(activity, 8), 0);
            item.setLayoutParams(lp);

            ImageView iconView = new ImageView(activity);
            int iconSize = dp(activity, 56);
            iconView.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
            iconView.setImageDrawable(icon);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            item.addView(iconView);

            TextView labelView = new TextView(activity);
            labelView.setText(label);
            labelView.setTextColor(Color.WHITE);
            labelView.setTextSize(13f);
            labelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            labelView.setSingleLine(true);
            labelView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelLp.topMargin = dp(activity, 6);
            labelView.setLayoutParams(labelLp);
            item.addView(labelView);

            // Анимация фокуса
            item.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    item.animate().scaleX(1.12f).scaleY(1.12f).setDuration(200).start();
                    iconView.setAlpha(1f);
                    labelView.setAlpha(1f);
                } else {
                    item.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    iconView.setAlpha(0.85f);
                    labelView.setAlpha(0.7f);
                }
            });

            // Запуск приложения
            item.setOnClickListener(v -> {
                Intent launch = pm.getLeanbackLaunchIntentForPackage(ri.activityInfo.packageName);
                if (launch == null) launch = pm.getLaunchIntentForPackage(ri.activityInfo.packageName);
                if (launch != null) {
                    activity.startActivity(launch);
                } else {
                    android.widget.Toast.makeText(activity,
                            "Не удалось запустить: " + label,
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            });

            container.addView(item);
        }
    }

    private static int dp(Activity activity, int dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
