package com.appletv.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class DynamicAppLoader {

    public static void loadApps(Context context, LinearLayout container) {
        container.removeAllViews();
        PackageManager pm = context.getPackageManager();

        // Ищем приложения для Android TV (Leanback)
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);

        // Если телевизор/эмулятор без Leanback, ищем обычные
        if (apps.isEmpty()) {
            intent.removeCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            apps = pm.queryIntentActivities(intent, 0);
        }

        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(context.getPackageName())) continue; // Пропускаем сам лаунчер

            View card = buildAppCard(context, pm, info, pkg);
            container.addView(card);
        }
    }

    private static View buildAppCard(Context context, PackageManager pm, ResolveInfo info, String pkg) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setFocusable(true);
        card.setClickable(true);

        int margin = dpToPx(context, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(context, 140), ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(margin, margin, margin, margin);
        card.setLayoutParams(params);

        ImageView icon = new ImageView(context);
        Drawable appIcon = info.loadIcon(pm);
        icon.setImageDrawable(appIcon);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(context, 120), dpToPx(context, 120)));
        icon.setBackgroundResource(R.drawable.status_pill_bg);
        icon.setPadding(dpToPx(context, 10), dpToPx(context, 10), dpToPx(context, 10), dpToPx(context, 10));

        TextView label = new TextView(context);
        label.setText(info.loadLabel(pm));
        label.setTextColor(Color.WHITE);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setAlpha(0.6f);
        label.setPadding(0, dpToPx(context, 12), 0, 0);

        card.addView(icon);
        card.addView(label);

        // Обработчик запуска
        card.setOnClickListener(v -> {
            Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
            if (launchIntent != null) context.startActivity(launchIntent);
        });

        // Обработчик фокуса (масштаб + анимация)
        card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                card.animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).start();
                label.setAlpha(1.0f);
                // Позже сюда мы подключим BannerManager
            } else {
                card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                label.setAlpha(0.6f);
            }
        });

        return card;
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
