package com.appletv.launcher;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private TextView heroTitle;
    private View ambientGlowBg;
    private RecyclerView dockRecycler;
    private RecyclerView gridRecycler;
    private FrameLayout btnSettings;
    
    private final List<AppItem> dockApps = new ArrayList<>();
    private final List<AppItem> gridApps = new ArrayList<>();
    private SharedPreferences prefs;
    private int currentGlowColor = 0xFF22222E;

    // ГЛОБАЛЬНЫЙ СПРАВОЧНИК кастомных баннеров для fallback-подмены
    private final Map<String, Integer> fallbackBanners = new HashMap<>();

    public static class AppItem {
        public String packageName;
        public String label;
        public Drawable icon;
        public Drawable banner;
        public int dominantColor;

        public AppItem(String packageName, String label, Drawable icon, Drawable banner, int dominantColor) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.banner = banner;
            this.dominantColor = dominantColor;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);

        // Инициализация справочника (ключ - часть имени пакета, значение - ресурс баннера)
        fallbackBanners.put("youtube", R.drawable.banner_youtube);
        fallbackBanners.put("kinopoisk", R.drawable.banner_kinopoisk);
        fallbackBanners.put("vk.video", R.drawable.banner_vk);
        fallbackBanners.put("vk.tv", R.drawable.banner_vk);

        heroTitle = findViewById(R.id.hero_title);
        ambientGlowBg = findViewById(R.id.ambient_glow_bg);
        dockRecycler = findViewById(R.id.dock_recycler);
        gridRecycler = findViewById(R.id.grid_recycler);
        btnSettings = findViewById(R.id.btn_launcher_settings);
        
        FrameLayout dockContainer = findViewById(R.id.dock_container);
        if (dockContainer != null) {
            dockContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(36));
                }
            });
            dockContainer.setClipToOutline(true);
        }

        if (heroTitle != null) {
            heroTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        }

        if (dockRecycler != null) {
            dockRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            dockRecycler.setAdapter(new TvosAppAdapter(dockApps, true));
        }

        if (gridRecycler != null) {
            gridRecycler.setLayoutManager(new GridLayoutManager(this, 4));
            gridRecycler.setAdapter(new TvosAppAdapter(gridApps, false));
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                } catch (Exception ignored) {}
            });
        }

        loadInstalledApps();
    }

    private void saveFavorites() {
        StringBuilder sb = new StringBuilder();
        for (AppItem item : dockApps) {
            sb.append(item.packageName).append(",");
        }
        prefs.edit().putString("favorites", sb.toString()).apply();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int extractDominantColor(Drawable drawable) {
        try {
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                bitmap = Bitmap.createBitmap(Math.max(1, drawable.getIntrinsicWidth()), Math.max(1, drawable.getIntrinsicHeight()), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
            if (bitmap != null) {
                Bitmap small = Bitmap.createScaledBitmap(bitmap, 8, 8, false);
                long r = 0, g = 0, b = 0;
                int count = 0;
                for (int x = 1; x < 7; x++) {
                    for (int y = 1; y < 7; y++) {
                        int pixel = small.getPixel(x, y);
                        if (Color.alpha(pixel) > 50) {
                            r += Color.red(pixel);
                            g += Color.green(pixel);
                            b += Color.blue(pixel);
                            count++;
                        }
                    }
                }
                if (count > 0) return Color.rgb((int)(r / count), (int)(g / count), (int)(b / count));
            }
        } catch (Exception ignored) {}
        return 0xFF2C2C2E;
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        
        // Умный сборщик: сначала собираем ТВ-версии, потом дополняем мобильными
        Map<String, ResolveInfo> uniqueApps = new HashMap<>();

        // 1. Ищем ТВ-приложения (LEANBACK_LAUNCHER - там лежат официальные баннеры 16:9)
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN, null);
        leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(leanbackIntent, 0)) {
            uniqueApps.put(ri.activityInfo.packageName, ri);
        }

        // 2. Ищем мобильные приложения (LAUNCHER) и добавляем только те, которых нет в ТВ-списке
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(launcherIntent, 0)) {
            if (!uniqueApps.containsKey(ri.activityInfo.packageName)) {
                uniqueApps.put(ri.activityInfo.packageName, ri);
            }
        }

        List<AppItem> allApps = new ArrayList<>();
        
        for (ResolveInfo ri : uniqueApps.values()) {
            if (ri.activityInfo.packageName.equals(getPackageName())) continue;

            String pkg = ri.activityInfo.packageName;
            String label = ri.loadLabel(pm).toString();
            Drawable icon = ri.loadIcon(pm);
            int domColor = extractDominantColor(icon);
            
            Drawable banner = null;

            // ПРИОРИТЕТ 1: Вытягиваем официальный ТВ-баннер из манифеста приложения
            try {
                banner = ri.activityInfo.loadBanner(pm);
                if (banner == null && ri.activityInfo.applicationInfo != null) {
                    banner = ri.activityInfo.applicationInfo.loadBanner(pm);
                }
            } catch (Exception ignored) {}

            // ПРИОРИТЕТ 2: Если официального баннера нет, ищем в нашем справочнике
            if (banner == null) {
                for (Map.Entry<String, Integer> entry : fallbackBanners.entrySet()) {
                    if (pkg.contains(entry.getKey())) {
                        banner = getDrawable(entry.getValue());
                        break;
                    }
                }
            }

            allApps.add(new AppItem(pkg, label, icon, banner, domColor));
        }

        Collections.sort(allApps, (a, b) -> a.label.compareToIgnoreCase(b.label));
        gridApps.clear();
        gridApps.addAll(allApps);

        dockApps.clear();
        String favs = prefs.getString("favorites", "");
        if (!favs.isEmpty()) {
            String[] favArray = favs.split(",");
            for (String pkg : favArray) {
                if (pkg.trim().isEmpty()) continue;
                for (AppItem app : allApps) {
                    if (app.packageName.equals(pkg)) {
                        dockApps.add(app);
                        break;
                    }
                }
            }
        }

        if (dockRecycler != null && dockRecycler.getAdapter() != null) dockRecycler.getAdapter().notifyDataSetChanged();
        if (gridRecycler != null && gridRecycler.getAdapter() != null) gridRecycler.getAdapter().notifyDataSetChanged();
    }

    private void animateAmbientGlow(int targetColor) {
        if (ambientGlowBg == null) return;
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), currentGlowColor, targetColor);
        colorAnim.setDuration(280);
        colorAnim.addUpdateListener(animator -> {
            int c = (int) animator.getAnimatedValue();
            currentGlowColor = c;
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{c, 0xFF050508});
            ambientGlowBg.setBackground(gd);
        });
        colorAnim.start();
    }

    private class TvosAppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<AppItem> items;
        private final boolean isDock;

        public TvosAppAdapter(List<AppItem> items, boolean isDock) {
            this.items = items;
            this.isDock = isDock;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout card = new FrameLayout(parent.getContext());
            
            int width = dpToPx(144);
            int height = dpToPx(81);
            
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(width, height);
            int marginX = isDock ? dpToPx(10) : dpToPx(38);
            int marginY = isDock ? dpToPx(10) : dpToPx(30);
            
            lp.setMargins(marginX, marginY, marginX, marginY);
            card.setLayoutParams(lp);
            card.setFocusable(true);
            card.setClickable(true);
            
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(12));
                }
            });
            card.setClipToOutline(true);

            ImageView bgImage = new ImageView(parent.getContext());
            bgImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            bgImage.setTag("bg_image");
            card.addView(bgImage, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            ImageView iconImage = new ImageView(parent.getContext());
            iconImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconImage.setTag("icon_image");
            // Сделали иконку чуть крупнее (56dp) для премиального вида на фоне 16:9
            card.addView(iconImage, new FrameLayout.LayoutParams(dpToPx(56), dpToPx(56), Gravity.CENTER));

            TextView starBadge = new TextView(parent.getContext());
            starBadge.setText("⭐");
            starBadge.setTextSize(12f);
            FrameLayout.LayoutParams starLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
            starLp.setMargins(0, dpToPx(4), dpToPx(6), 0);
            starBadge.setLayoutParams(starLp);
            starBadge.setTag("fav_star");
            card.addView(starBadge);

            return new RecyclerView.ViewHolder(card) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AppItem item = items.get(position);
            FrameLayout card = (FrameLayout) holder.itemView;
            ImageView bgImage = (ImageView) card.findViewWithTag("bg_image");
            ImageView iconImage = (ImageView) card.findViewWithTag("icon_image");
            TextView starBadge = (TextView) card.findViewWithTag("fav_star");

            if (item.banner != null) {
                // Если есть ТВ-баннер (Официальный или Кастомный)
                bgImage.setImageDrawable(item.banner);
                iconImage.setVisibility(View.GONE);
            } else {
                // ПРИОРИТЕТ 3: Автогенератор 16:9 (для мобильных приложений)
                int baseColor = item.dominantColor;
                // Затемняем доминирующий цвет для градиента
                int darkColor = Color.rgb((int)(Color.red(baseColor) * 0.25f), (int)(Color.green(baseColor) * 0.25f), (int)(Color.blue(baseColor) * 0.25f));
                
                // Стильный диагональный градиент
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{baseColor, darkColor});
                bgImage.setImageDrawable(gradient);
                
                // Иконка строго по центру
                iconImage.setImageDrawable(item.icon);
                iconImage.setVisibility(View.VISIBLE);
            }

            boolean isFav = false;
            for (AppItem a : dockApps) {
                if (a.packageName.equals(item.packageName)) {
                    isFav = true;
                    break;
                }
            }

            if (!isDock && isFav) {
                starBadge.setAlpha(1f); starBadge.setScaleX(1f); starBadge.setScaleY(1f);
            } else {
                starBadge.setAlpha(0f); starBadge.setScaleX(0f); starBadge.setScaleY(0f);
            }

            card.setOnLongClickListener(v -> {
                if (!isDock) {
                    AppItem favItem = null;
                    for (AppItem a : dockApps) {
                        if (a.packageName.equals(item.packageName)) { favItem = a; break; }
                    }
                    if (favItem == null) {
                        dockApps.add(item);
                        saveFavorites();
                        starBadge.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new OvershootInterpolator()).start();
                        if (dockRecycler != null && dockRecycler.getAdapter() != null) {
                            dockRecycler.getAdapter().notifyItemInserted(dockApps.size() - 1);
                            dockRecycler.smoothScrollToPosition(dockApps.size() - 1);
                        }
                    } else {
                        int pos = dockApps.indexOf(favItem);
                        if (pos >= 0) {
                            dockApps.remove(pos);
                            saveFavorites();
                            starBadge.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(200).start();
                            if (dockRecycler != null && dockRecycler.getAdapter() != null) dockRecycler.getAdapter().notifyItemRemoved(pos);
                        }
                    }
                } else {
                    int pos = dockApps.indexOf(item);
                    if (pos >= 0) {
                        dockApps.remove(pos);
                        saveFavorites();
                        notifyItemRemoved(pos);
                        if (gridRecycler != null && gridRecycler.getAdapter() != null) gridRecycler.getAdapter().notifyDataSetChanged();
                    }
                }
                return true;
            });

            card.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    animateScale(card, 1.10f, 1.10f, dpToPx(30));
                    animateAmbientGlow(item.dominantColor);
                    if (heroTitle != null) heroTitle.setText(item.label);
                } else {
                    animateScale(card, 1.0f, 1.0f, 0f);
                }
            });

            // Обработчик нажатия тоже улучшен: ищем Leanback Intent, если его нет - берем мобильный
            card.setOnClickListener(v -> {
                PackageManager pm = getPackageManager();
                Intent launch = pm.getLeanbackLaunchIntentForPackage(item.packageName);
                if (launch == null) launch = pm.getLaunchIntentForPackage(item.packageName);
                if (launch != null) startActivity(launch);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private void animateScale(View view, float sx, float sy, float elev) {
        ObjectAnimator ax = ObjectAnimator.ofFloat(view, "scaleX", sx);
        ObjectAnimator ay = ObjectAnimator.ofFloat(view, "scaleY", sy);
        ObjectAnimator az = ObjectAnimator.ofFloat(view, "translationZ", elev);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(ax, ay, az);
        set.setDuration(180);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }
}
