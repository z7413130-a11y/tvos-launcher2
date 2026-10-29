package com.appletv.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
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
    private View topStatusContainer;
    private View timePillContainer;
    private ScrollView mainScrollView;
    private View dockContainer;
    private View ambientGlowBg;
    private View gridDarkOverlay;
    private RecyclerView dockRecycler;
    private RecyclerView gridRecycler;
    
    private final List<AppItem> dockApps = new ArrayList<>();
    private final List<AppItem> gridApps = new ArrayList<>();
    private SharedPreferences prefs;
    private int currentGlowColor = 0xFF22222E;
    private float currentUiScale = 1.0f;
    
    private boolean isGridViewOpen = false; 
    public static final String VIRTUAL_SETTINGS_PKG = "com.appletv.launcher.INTERNAL_SETTINGS";

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AppItem appItem = (AppItem) o;
            return packageName.equals(appItem.packageName);
        }

        @Override
        public int hashCode() {
            return packageName.hashCode();
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
        topStatusContainer = findViewById(R.id.top_status_container);
        timePillContainer = findViewById(R.id.time_pill_container);
        mainScrollView = findViewById(R.id.main_scroll_view);
        dockContainer = findViewById(R.id.dock_container);
        ambientGlowBg = findViewById(R.id.ambient_glow_bg);
        gridDarkOverlay = findViewById(R.id.grid_dark_overlay);
        dockRecycler = findViewById(R.id.dock_recycler);
        gridRecycler = findViewById(R.id.grid_recycler);
        
        if (dockContainer != null) {
            dockContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(28));
                }
            });
            dockContainer.setClipToOutline(true);
        }

        if (heroTitle != null) {
            heroTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        }

        loadInstalledApps();
        setupGlobalFocusListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupDynamicGrid();
    }

    private void setupGlobalFocusListener() {
        final View decorView = getWindow().getDecorView();
        decorView.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
            boolean fromDock = isViewInDock(oldFocus);
            boolean toGrid = isViewInGrid(newFocus);
            boolean fromGrid = isViewInGrid(oldFocus);
            boolean toDock = isViewInDock(newFocus);

            if (fromDock && toGrid && !isGridViewOpen) {
                openGridView();
            } else if (fromGrid && toDock && isGridViewOpen) {
                closeGridView();
            }
        });
    }

    private boolean isViewInDock(View v) {
        if (v == null || dockRecycler == null) return false;
        View parent = v;
        while (parent != null) {
            if (parent == dockRecycler) return true;
            if (!(parent.getParent() instanceof View)) break;
            parent = (View) parent.getParent();
        }
        return false;
    }

    private boolean isViewInGrid(View v) {
        if (v == null || gridRecycler == null) return false;
        View parent = v;
        while (parent != null) {
            if (parent == gridRecycler) return true;
            if (!(parent.getParent() instanceof View)) break;
            parent = (View) parent.getParent();
        }
        return false;
    }

    private void openGridView() {
        isGridViewOpen = true;

        if (topStatusContainer != null) {
            topStatusContainer.animate().translationY(-dpToPx(40)).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
        }
        
        if (gridDarkOverlay != null) {
            gridDarkOverlay.animate().alpha(1f).setDuration(300).start();
        }

        if (mainScrollView != null && dockContainer != null) {
            mainScrollView.smoothScrollTo(0, dockContainer.getTop() - dpToPx(24));
        }
    }

    private void closeGridView() {
        isGridViewOpen = false;

        if (topStatusContainer != null) {
            topStatusContainer.animate().translationY(0).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
        }
        
        if (gridDarkOverlay != null) {
            gridDarkOverlay.animate().alpha(0f).setDuration(300).start();
        }

        if (mainScrollView != null) {
            mainScrollView.smoothScrollTo(0, 0);
        }
    }

    private float getUiScale() {
        return prefs.getFloat("ui_scale", 1.0f);
    }

    private void setupDynamicGrid() {
        if (gridRecycler == null) return;
        
        currentUiScale = getUiScale();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        
        int screenMarginsPx = dpToPx((int)(104 * currentUiScale)); 
        int availableWidthPx = screenWidth - screenMarginsPx;
        
        int columns = 6;
        int itemCellWidthPx = availableWidthPx / columns;
        
        int itemMarginPx = dpToPx((int)(8 * currentUiScale));
        int cardWidthPx = itemCellWidthPx - (itemMarginPx * 2);
        int cardHeightPx = (int) (cardWidthPx * (9f / 16f)); 

        gridRecycler.setLayoutManager(new GridLayoutManager(this, columns));
        gridRecycler.setAdapter(new TvosAppAdapter(gridApps, false, cardWidthPx, cardHeightPx, itemMarginPx));
        
        if (dockRecycler != null) {
            dockRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            dockRecycler.setAdapter(new TvosAppAdapter(dockApps, true, cardWidthPx, cardHeightPx, itemMarginPx));
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
                        if (Color.alpha(pixel) > 40) {
                            r += Color.red(pixel);
                            g += Color.green(pixel);
                            b += Color.blue(pixel);
                            count++;
                        }
                    }
                }
                if (count > 0) {
                    int avR = (int)(r / count);
                    int avG = (int)(g / count);
                    int avB = (int)(b / count);
                    
                    float[] hsv = new float[3];
                    Color.RGBToHSV(avR, avG, avB, hsv);
                    hsv[1] = Math.min(1.0f, hsv[1] * 1.35f);
                    hsv[2] = Math.max(0.45f, hsv[2] * 1.25f);
                    return Color.HSVToColor(hsv);
                }
            }
        } catch (Exception ignored) {}
        return 0xFF3A3A42;
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
        
        Drawable settingsIcon = getResources().getDrawable(R.drawable.ic_tvos_settings, null);
        AppItem virtualSettings = new AppItem(VIRTUAL_SETTINGS_PKG, "Настройки", settingsIcon, null, 0xFF8A8C92);
        allApps.add(virtualSettings);

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

            AppItem app = new AppItem(pkg, label, icon, banner, domColor);
            if (!allApps.contains(app)) {
                allApps.add(app);
            }
        }

        Collections.sort(allApps, (a, b) -> a.label.compareToIgnoreCase(b.label));
        
        String favs = prefs.getString("favorites", "");
        
        // ФОРСИРУЕМ добавление Настроек с помощью нового ключа settings_injected_v2
        boolean settingsInjected = prefs.getBoolean("settings_injected_v2", false);
        if (!settingsInjected) {
            if (!favs.contains(VIRTUAL_SETTINGS_PKG)) {
                favs = favs.isEmpty() ? VIRTUAL_SETTINGS_PKG : VIRTUAL_SETTINGS_PKG + "," + favs;
            }
            prefs.edit().putString("favorites", favs).putBoolean("settings_injected_v2", true).apply();
        }

        List<String> favList = new ArrayList<>();
        if (!favs.isEmpty()) {
            for (String p : favs.split(",")) {
                if (!p.trim().isEmpty() && !favList.contains(p.trim())) {
                    favList.add(p.trim());
                }
            }
        }

        dockApps.clear();
        gridApps.clear();

        for (String favPkg : favList) {
            for (AppItem app : allApps) {
                if (app.packageName.equals(favPkg) && !dockApps.contains(app)) {
                    dockApps.add(app);
                    break;
                }
            }
        }

        for (AppItem app : allApps) {
            boolean inFav = false;
            for (AppItem f : dockApps) {
                if (f.packageName.equals(app.packageName)) {
                    inFav = true;
                    break;
                }
            }
            if (!inFav && !gridApps.contains(app)) {
                gridApps.add(app);
            }
        }
    }

    private void animateAmbientGlow(int targetColor) {
        if (ambientGlowBg == null) return;
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), currentGlowColor, targetColor);
        colorAnim.setDuration(350);
        colorAnim.addUpdateListener(animator -> {
            int c = (int) animator.getAnimatedValue();
            currentGlowColor = c;
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{c, 0xFF050508});
            ambientGlowBg.setBackground(gd);
        });
        colorAnim.start();
    }

    class AppViewHolder extends RecyclerView.ViewHolder {
        LinearLayout root;
        FrameLayout card;
        ImageView bgImage;
        ImageView iconImage;
        TextView localLabel;
        View glareOverlay;
        
        ObjectAnimator shimmerAnim;
        AnimatorSet scaleAnim;

        public AppViewHolder(LinearLayout root, FrameLayout card, ImageView bgImage, ImageView iconImage, TextView localLabel, View glareOverlay) {
            super(root);
            this.root = root;
            this.card = card;
            this.bgImage = bgImage;
            this.iconImage = iconImage;
            this.localLabel = localLabel;
            this.glareOverlay = glareOverlay;
        }

        public void resetState() {
            if (scaleAnim != null) scaleAnim.cancel();
            if (shimmerAnim != null) shimmerAnim.cancel();
            card.setScaleX(1.0f);
            card.setScaleY(1.0f);
            card.setAlpha(1.0f);
            card.setTranslationZ(dpToPx(4));
            glareOverlay.setAlpha(0f);
            localLabel.setAlpha(0f);
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
            
            card.addView(iconImage, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

            View glareOverlay = new View(ctx);
            GradientDrawable glareGd = new GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                new int[]{0x00FFFFFF, 0x00FFFFFF, 0x1AFFFFFF, 0x33FFFFFF, 0x1AFFFFFF, 0x00FFFFFF, 0x00FFFFFF}
            );
            glareOverlay.setBackground(glareGd);
            glareOverlay.setLayoutParams(new FrameLayout.LayoutParams((int)(cardWidth * 2.0f), ViewGroup.LayoutParams.MATCH_PARENT));
            glareOverlay.setAlpha(0f);
            card.addView(glareOverlay);

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

            return new AppViewHolder(root, card, bgImage, iconImage, localLabel, glareOverlay);
        }

        @Override
        public void onViewRecycled(@NonNull AppViewHolder holder) {
            super.onViewRecycled(holder);
            holder.resetState();
        }

        @Override
        public void onBindViewHolder(@NonNull AppViewHolder vh, int position) {
            vh.resetState();

            if (position < 0 || position >= items.size()) return;
            AppItem item = items.get(position);
            vh.localLabel.setText(item.label);

            if (item.banner != null) {
                vh.bgImage.setImageDrawable(item.banner);
                vh.iconImage.setVisibility(View.GONE);
            } else {
                if (VIRTUAL_SETTINGS_PKG.equals(item.packageName)) {
                    vh.bgImage.setImageDrawable(null);
                    vh.bgImage.setBackgroundColor(Color.TRANSPARENT);
                    vh.iconImage.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    vh.iconImage.setScaleType(ImageView.ScaleType.FIT_XY);
                } else {
                    int baseColor = item.dominantColor;
                    int darkColor = Color.rgb((int)(Color.red(baseColor) * 0.35f), (int)(Color.green(baseColor) * 0.35f), (int)(Color.blue(baseColor) * 0.35f));
                    GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{baseColor, darkColor});
                    vh.bgImage.setImageDrawable(gradient);
                    
                    int iconSize = dpToPx((int)(56 * currentUiScale));
                    vh.iconImage.setLayoutParams(new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
                    vh.iconImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
                vh.iconImage.setImageDrawable(item.icon);
                vh.iconImage.setVisibility(View.VISIBLE);
            }

            vh.root.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (isDock && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return true;
                    }
                }
                return false;
            });

            vh.root.setOnLongClickListener(v -> {
                if (!isDock) {
                    if (dockApps.contains(item)) return true;

                    AnimatorSet outAnim = new AnimatorSet();
                    ObjectAnimator pressX = ObjectAnimator.ofFloat(vh.card, "scaleX", 1.15f, 0.9f, 0f);
                    ObjectAnimator pressY = ObjectAnimator.ofFloat(vh.card, "scaleY", 1.15f, 0.9f, 0f);
                    ObjectAnimator fade = ObjectAnimator.ofFloat(vh.card, "alpha", 1f, 0f);
                    outAnim.playTogether(pressX, pressY, fade);
                    outAnim.setDuration(250);
                    outAnim.setInterpolator(new DecelerateInterpolator());
                    
                    outAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!dockApps.contains(item)) {
                                dockApps.add(item);
                            }
                            gridApps.remove(item);
                            saveFavorites();
                            
                            if (gridRecycler != null && gridRecycler.getAdapter() != null) {
                                gridRecycler.getAdapter().notifyDataSetChanged();
                            }
                            if (dockRecycler != null && dockRecycler.getAdapter() != null) {
                                dockRecycler.getAdapter().notifyDataSetChanged();
                            }
                        }
                    });
                    outAnim.start();

                } else {
                    if (gridApps.contains(item)) return true;

                    AnimatorSet outAnim = new AnimatorSet();
                    ObjectAnimator pressX = ObjectAnimator.ofFloat(vh.card, "scaleX", 1.15f, 0.9f, 0f);
                    ObjectAnimator pressY = ObjectAnimator.ofFloat(vh.card, "scaleY", 1.15f, 0.9f, 0f);
                    ObjectAnimator fade = ObjectAnimator.ofFloat(vh.card, "alpha", 1f, 0f);
                    outAnim.playTogether(pressX, pressY, fade);
                    outAnim.setDuration(250);
                    outAnim.setInterpolator(new DecelerateInterpolator());
                    
                    outAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            dockApps.remove(item);
                            if (!gridApps.contains(item)) {
                                gridApps.add(item);
                            }
                            Collections.sort(gridApps, (a, b) -> a.label.compareToIgnoreCase(b.label));
                            saveFavorites();
                            
                            if (dockRecycler != null && dockRecycler.getAdapter() != null) {
                                dockRecycler.getAdapter().notifyDataSetChanged();
                            }
                            if (gridRecycler != null && gridRecycler.getAdapter() != null) {
                                gridRecycler.getAdapter().notifyDataSetChanged();
                            }
                        }
                    });
                    outAnim.start();
                }
                return true;
            });

            vh.root.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (vh.scaleAnim != null) vh.scaleAnim.cancel();
                    vh.scaleAnim = new AnimatorSet();
                    ObjectAnimator sx = ObjectAnimator.ofFloat(vh.card, "scaleX", 1.12f);
                    ObjectAnimator sy = ObjectAnimator.ofFloat(vh.card, "scaleY", 1.12f);
                    ObjectAnimator sz = ObjectAnimator.ofFloat(vh.card, "translationZ", dpToPx((int)(30 * currentUiScale)));
                    vh.scaleAnim.playTogether(sx, sy, sz);
                    vh.scaleAnim.setDuration(350);
                    vh.scaleAnim.setInterpolator(new OvershootInterpolator(1.1f));
                    vh.scaleAnim.start();

                    animateAmbientGlow(item.dominantColor);
                    
                    vh.glareOverlay.animate().alpha(1f).setDuration(300).start();
                    if (vh.shimmerAnim == null) {
                        vh.glareOverlay.setTranslationX(-cardWidth * 1.2f);
                        vh.shimmerAnim = ObjectAnimator.ofFloat(vh.glareOverlay, "translationX", -cardWidth * 1.2f, cardWidth * 2.0f);
                        vh.shimmerAnim.setDuration(4200);
                        vh.shimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
                    }
                    vh.shimmerAnim.start();

                    if (isDock) {
                        if (heroTitle != null) {
                            heroTitle.animate().alpha(1f).setDuration(200).start();
                            heroTitle.setText(item.label);
                        }
                        if (timePillContainer != null) {
                            timePillContainer.animate().alpha(1f).setDuration(200).start();
                        }
                    } else {
                        if (heroTitle != null) {
                            heroTitle.animate().alpha(0f).setDuration(200).start();
                        }
                        if (timePillContainer != null) {
                            timePillContainer.animate().alpha(0f).setDuration(200).start();
                        }
                        vh.localLabel.animate().alpha(1f).setDuration(200).start();
                    }
                } else {
                    if (vh.scaleAnim != null) vh.scaleAnim.cancel();
                    vh.scaleAnim = new AnimatorSet();
                    ObjectAnimator sx = ObjectAnimator.ofFloat(vh.card, "scaleX", 1.0f);
                    ObjectAnimator sy = ObjectAnimator.ofFloat(vh.card, "scaleY", 1.0f);
                    ObjectAnimator sz = ObjectAnimator.ofFloat(vh.card, "translationZ", dpToPx((int)(4 * currentUiScale)));
                    vh.scaleAnim.playTogether(sx, sy, sz);
                    vh.scaleAnim.setDuration(250);
                    vh.scaleAnim.setInterpolator(new DecelerateInterpolator(1.5f));
                    vh.scaleAnim.start();

                    if (vh.shimmerAnim != null) vh.shimmerAnim.cancel();
                    vh.glareOverlay.animate().alpha(0f).setDuration(200).start();
                    
                    if (!isDock) {
                        vh.localLabel.animate().alpha(0f).setDuration(200).start();
                    }
                }
            });

            vh.root.setOnClickListener(v -> {
                if (VIRTUAL_SETTINGS_PKG.equals(item.packageName)) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return;
                }

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

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        SettingsManager.startSleepTimer(this);
    }
}