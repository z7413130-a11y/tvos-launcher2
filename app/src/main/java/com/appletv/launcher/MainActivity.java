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
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private float currentUiScale = 1.0f;

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
            });
            dockContainer.setClipToOutline(true);
        }

        if (heroTitle != null) {
            heroTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            heroTitle.setAlpha(0f); 
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

    @Override
    protected void onResume() {
        super.onResume();
        setupDynamicGrid();
    }

    private float getUiScale() {
        return prefs.getFloat("ui_scale", 1.0f);
    }

    private void setupDynamicGrid() {
        if (gridRecycler == null) return;
        
        currentUiScale = getUiScale();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        
        int screenMarginsPx = dpToPx((int)(96 * currentUiScale));
        int availableWidthPx = screenWidth - screenMarginsPx;
        
        int columns = 6;
        int itemCellWidthPx = availableWidthPx / columns;
        
        int gridItemMarginPx = dpToPx((int)(8 * currentUiScale));
        int gridCardWidthPx = itemCellWidthPx - (gridItemMarginPx * 2);
        int gridCardHeightPx = (int) (gridCardWidthPx * (9f / 16f)); 

        float dockScaleMult = 1.18f;
        int dockCardWidthPx = (int) (gridCardWidthPx * dockScaleMult);
        int dockCardHeightPx = (int) (gridCardHeightPx * dockScaleMult);
        int dockItemMarginPx = dpToPx((int)(10 * currentUiScale));

        gridRecycler.setLayoutManager(new GridLayoutManager(this, columns));
        gridRecycler.setAdapter(new TvosAppAdapter(gridApps, false, gridCardWidthPx, gridCardHeightPx, gridItemMarginPx));
        
        if (dockRecycler != null) {
            dockRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            dockRecycler.setAdapter(new TvosAppAdapter(dockApps, true, dockCardWidthPx, dockCardHeightPx, dockItemMarginPx));
        }
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
        Map<String, ResolveInfo> uniqueApps = new HashMap<>();

        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN, null);
        leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(leanbackIntent, 0)) {
            uniqueApps.put(ri.activityInfo.packageName, ri);
        }

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

            try {
                banner = ri.activityInfo.loadBanner(pm);
                if (banner == null && ri.activityInfo.applicationInfo != null) {
                    banner = ri.activityInfo.applicationInfo.loadBanner(pm);
                }
            } catch (Exception ignored) {}

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
    }

    private void animateAmbientGlow(int targetColor) {
        if (ambientGlowBg == null) return;
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), currentGlowColor, targetColor);
        colorAnim.setDuration(400); // Сделали перетекание фона еще мягче
        colorAnim.addUpdateListener(animator -> {
            int c = (int) animator.getAnimatedValue();
            currentGlowColor = c;
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{c, 0xFF050508});
            ambientGlowBg.setBackground(gd);
        });
        colorAnim.start();
    }

    // Внедряем строгий паттерн ViewHolder для контроля комплексных анимаций без утечек и лагов
    class AppViewHolder extends RecyclerView.ViewHolder {
        LinearLayout root;
        FrameLayout card;
        ImageView bgImage;
        ImageView iconImage;
        TextView starBadge;
        TextView localLabel;
        View glareOverlay;
        
        ObjectAnimator shimmerAnim;
        AnimatorSet scaleAnim;

        public AppViewHolder(LinearLayout root, FrameLayout card, ImageView bgImage, ImageView iconImage, TextView starBadge, TextView localLabel, View glareOverlay) {
            super(root);
            this.root = root;
            this.card = card;
            this.bgImage = bgImage;
            this.iconImage = iconImage;
            this.starBadge = starBadge;
            this.localLabel = localLabel;
            this.glareOverlay = glareOverlay;
        }
    }

    private class TvosAppAdapter extends RecyclerView.Adapter<AppViewHolder> {
        private final List<AppItem> items;
        private final boolean isDock;
        
        private final int cardWidth;
        private final int cardHeight;
        private final int itemMargin;

        public TvosAppAdapter(List<AppItem> items, boolean isDock, int cardWidth, int cardHeight, int itemMargin) {
            this.items = items;
            this.isDock = isDock;
            this.cardWidth = cardWidth;
            this.cardHeight = cardHeight;
            this.itemMargin = itemMargin;
        }

        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context ctx = parent.getContext();
            
            LinearLayout root = new LinearLayout(ctx);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER);
            root.setFocusable(true);
            root.setClickable(true);
            root.setClipChildren(false); 
            root.setClipToPadding(false);

            RecyclerView.LayoutParams rootLp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rootLp.setMargins(itemMargin, itemMargin, itemMargin, itemMargin);
            root.setLayoutParams(rootLp);
            
            FrameLayout card = new FrameLayout(ctx);
            card.setFocusable(false);
            card.setClickable(false);
            card.setElevation(dpToPx((int)(4 * currentUiScale)));
            
            card.setLayoutParams(new LinearLayout.LayoutParams(cardWidth, cardHeight));
            
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx((int)(12 * currentUiScale)));
                }
            });
            card.setClipToOutline(true);

            ImageView bgImage = new ImageView(ctx);
            bgImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(bgImage, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            ImageView iconImage = new ImageView(ctx);
            iconImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int iconSize = isDock ? dpToPx((int)(66 * currentUiScale)) : dpToPx((int)(56 * currentUiScale));
            card.addView(iconImage, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));

            // ЭФФЕКТ ПЕРЕЛИВАЮЩЕГОСЯ БЛИКА (Glare Overlay)
            View glareOverlay = new View(ctx);
            GradientDrawable glareGd = new GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                new int[]{0x00FFFFFF, 0x00FFFFFF, 0x33FFFFFF, 0x77FFFFFF, 0x33FFFFFF, 0x00FFFFFF, 0x00FFFFFF}
            );
            glareOverlay.setBackground(glareGd);
            // Блик шире карточки, чтобы движение было плавным и покрывало углы
            glareOverlay.setLayoutParams(new FrameLayout.LayoutParams((int)(cardWidth * 1.8f), ViewGroup.LayoutParams.MATCH_PARENT));
            glareOverlay.setAlpha(0f);
            card.addView(glareOverlay);

            TextView starBadge = new TextView(ctx);
            starBadge.setText("⭐");
            starBadge.setTextSize(12f * currentUiScale);
            FrameLayout.LayoutParams starLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
            starLp.setMargins(0, dpToPx((int)(4 * currentUiScale)), dpToPx((int)(6 * currentUiScale)), 0);
            starBadge.setLayoutParams(starLp);
            card.addView(starBadge);

            root.addView(card);

            TextView localLabel = new TextView(ctx);
            localLabel.setTextSize(13f * currentUiScale);
            localLabel.setTextColor(Color.WHITE);
            localLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            localLabel.setSingleLine(true);
            localLabel.setGravity(Gravity.CENTER);
            localLabel.setAlpha(0f); 
            
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            labelLp.setMargins(0, dpToPx((int)(8 * currentUiScale)), 0, 0);
            localLabel.setLayoutParams(labelLp);
            
            if (isDock) {
                localLabel.setVisibility(View.GONE);
            }
            
            root.addView(localLabel);

            return new AppViewHolder(root, card, bgImage, iconImage, starBadge, localLabel, glareOverlay);
        }

        @Override
        public void onBindViewHolder(@NonNull AppViewHolder vh, int position) {
            AppItem item = items.get(position);

            vh.localLabel.setText(item.label);

            if (item.banner != null) {
                vh.bgImage.setImageDrawable(item.banner);
                vh.iconImage.setVisibility(View.GONE);
            } else {
                int baseColor = item.dominantColor;
                int darkColor = Color.rgb((int)(Color.red(baseColor) * 0.25f), (int)(Color.green(baseColor) * 0.25f), (int)(Color.blue(baseColor) * 0.25f));
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{baseColor, darkColor});
                vh.bgImage.setImageDrawable(gradient);
                vh.iconImage.setImageDrawable(item.icon);
                vh.iconImage.setVisibility(View.VISIBLE);
            }

            // Нативный цветной градиентный свет из-под карточки (Если Android 9+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                vh.card.setOutlineSpotShadowColor(item.dominantColor);
                vh.card.setOutlineAmbientShadowColor(item.dominantColor);
            }

            boolean isFav = false;
            for (AppItem a : dockApps) {
                if (a.packageName.equals(item.packageName)) {
                    isFav = true;
                    break;
                }
            }

            if (!isDock && isFav) {
                vh.starBadge.setAlpha(1f); vh.starBadge.setScaleX(1f); vh.starBadge.setScaleY(1f);
            } else {
                vh.starBadge.setAlpha(0f); vh.starBadge.setScaleX(0f); vh.starBadge.setScaleY(0f);
            }

            if (vh.root.isFocused()) {
                if (isDock && heroTitle != null) {
                    heroTitle.setAlpha(1f);
                    heroTitle.setText(item.label);
                } else if (!isDock) {
                    vh.localLabel.setAlpha(1f);
                }
            } else {
                if (!isDock) vh.localLabel.setAlpha(0f);
            }

            vh.root.setOnLongClickListener(v -> {
                if (!isDock) {
                    AppItem favItem = null;
                    for (AppItem a : dockApps) {
                        if (a.packageName.equals(item.packageName)) { favItem = a; break; }
                    }
                    if (favItem == null) {
                        dockApps.add(item);
                        saveFavorites();
                        vh.starBadge.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new OvershootInterpolator()).start();
                        if (dockRecycler != null && dockRecycler.getAdapter() != null) {
                            dockRecycler.getAdapter().notifyItemInserted(dockApps.size() - 1);
                            dockRecycler.smoothScrollToPosition(dockApps.size() - 1);
                        }
                    } else {
                        int pos = dockApps.indexOf(favItem);
                        if (pos >= 0) {
                            dockApps.remove(pos);
                            saveFavorites();
                            vh.starBadge.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(200).start();
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

            vh.root.setOnFocusChangeListener((v, hasFocus) -> {
                Context ctx = v.getContext();
                ScrollView sv = ((Activity) ctx).findViewById(R.id.main_scroll_view);
                View dockContainer = ((Activity) ctx).findViewById(R.id.dock_container);
                
                if (hasFocus) {
                    // 1. SPRING-АНИМАЦИЯ МАСШТАБА И ГЛУБОКАЯ ТЕНЬ (1.12x)
                    if (vh.scaleAnim != null) vh.scaleAnim.cancel();
                    vh.scaleAnim = new AnimatorSet();
                    ObjectAnimator sx = ObjectAnimator.ofFloat(vh.card, "scaleX", 1.12f);
                    ObjectAnimator sy = ObjectAnimator.ofFloat(vh.card, "scaleY", 1.12f);
                    ObjectAnimator sz = ObjectAnimator.ofFloat(vh.card, "translationZ", dpToPx((int)(30 * currentUiScale)));
                    vh.scaleAnim.playTogether(sx, sy, sz);
                    vh.scaleAnim.setDuration(350);
                    vh.scaleAnim.setInterpolator(new OvershootInterpolator(1.1f)); // Мягкий эластичный отскок Apple TV
                    vh.scaleAnim.start();

                    animateAmbientGlow(item.dominantColor);
                    
                    // 2. ЗАПУСК КИНЕМАТОГРАФИЧНОГО БЛИКА ПО ПОВЕРХНОСТИ
                    vh.glareOverlay.setAlpha(1f);
                    if (vh.shimmerAnim == null) {
                        vh.glareOverlay.setTranslationX(-cardWidth);
                        vh.shimmerAnim = ObjectAnimator.ofFloat(vh.glareOverlay, "translationX", -cardWidth, cardWidth * 1.5f);
                        vh.shimmerAnim.setDuration(2800); // Медленное и плавное прохождение света
                        vh.shimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
                    }
                    vh.shimmerAnim.start();

                    // Тексты и авто-скролл
                    if (isDock) {
                        if (sv != null) sv.smoothScrollTo(0, 0);
                        if (heroTitle != null) {
                            heroTitle.animate().alpha(1f).setDuration(200).start();
                            heroTitle.setText(item.label);
                        }
                    } else {
                        if (sv != null && dockContainer != null) {
                            int targetY = dockContainer.getTop() - dpToPx((int)(24 * currentUiScale));
                            if (sv.getScrollY() < targetY - dpToPx(30)) {
                                sv.smoothScrollTo(0, targetY);
                            }
                        }
                        if (heroTitle != null) {
                            heroTitle.animate().alpha(0f).setDuration(200).start();
                        }
                        vh.localLabel.animate().alpha(1f).setDuration(200).start();
                    }
                } else {
                    // 1. МЯГКИЙ ВОЗВРАТ КАРТОЧКИ В ИСХОДНОЕ СОСТОЯНИЕ
                    if (vh.scaleAnim != null) vh.scaleAnim.cancel();
                    vh.scaleAnim = new AnimatorSet();
                    ObjectAnimator sx = ObjectAnimator.ofFloat(vh.card, "scaleX", 1.0f);
                    ObjectAnimator sy = ObjectAnimator.ofFloat(vh.card, "scaleY", 1.0f);
                    ObjectAnimator sz = ObjectAnimator.ofFloat(vh.card, "translationZ", dpToPx((int)(4 * currentUiScale)));
                    vh.scaleAnim.playTogether(sx, sy, sz);
                    vh.scaleAnim.setDuration(250);
                    vh.scaleAnim.setInterpolator(new DecelerateInterpolator(1.5f)); // Плавно тормозит в конце
                    vh.scaleAnim.start();

                    // 2. ОТКЛЮЧЕНИЕ СВЕТОВОГО БЛИКА
                    if (vh.shimmerAnim != null) vh.shimmerAnim.cancel();
                    vh.glareOverlay.animate().alpha(0f).setDuration(200).start();
                    
                    if (!isDock) {
                        vh.localLabel.animate().alpha(0f).setDuration(200).start();
                    }
                }
            });

            vh.root.setOnClickListener(v -> {
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
}
