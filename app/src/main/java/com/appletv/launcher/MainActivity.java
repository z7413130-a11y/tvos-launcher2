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
import android.os.Handler;
import android.os.Looper;
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

    private boolean isGridViewOpen = false;
    private int lastDockPosition = 0;
    private boolean isEditMode = false;
    private AppItem grabbedItem = null;
    private final Handler editHandler = new Handler(Looper.getMainLooper());
    private Runnable editTimeoutRunnable = null;

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
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SoundManager.init(this);

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

        if (mainScrollView != null) mainScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingsManager.applyTimeFormat(this);
        SettingsManager.applyBackgroundMode(this);
        SettingsManager.applyClockVisibility(this);
        SettingsManager.startSleepTimer(this);
        setupDynamicGrid();
    }

    private void setupDynamicGrid() {
        if (gridRecycler == null) return;

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenMarginsPx = dpToPx(104);
        int availableWidthPx = screenWidth - screenMarginsPx;
        final int columns = 6;
        int itemCellWidthPx = availableWidthPx / columns;
        int itemMarginPx = dpToPx(8);
        int cardWidthPx = itemCellWidthPx - (itemMarginPx * 2);
        int cardHeightPx = (int) (cardWidthPx * (9f / 16f));

        gridRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);
        gridRecycler.setLayoutManager(new GridLayoutManager(this, columns));
        gridRecycler.setAdapter(new TvosAppAdapter(gridApps, false, cardWidthPx, cardHeightPx, itemMarginPx));

        if (dockRecycler != null) {
            dockRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);
            dockRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            dockRecycler.setAdapter(new TvosAppAdapter(dockApps, true, cardWidthPx, cardHeightPx, itemMarginPx));

            dockRecycler.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN && !isEditMode) {
                    openGridView();
                    scrollToGrid();
                    if (gridRecycler != null && !gridApps.isEmpty()) {
                        gridRecycler.post(() -> {
                            gridRecycler.scrollToPosition(0);
                            RecyclerView.ViewHolder vh = gridRecycler.findViewHolderForAdapterPosition(0);
                            if (vh != null) vh.itemView.requestFocus();
                            else if (gridRecycler.getChildCount() > 0) gridRecycler.getChildAt(0).requestFocus();
                        });
                    }
                    return true;
                }
                return false;
            });

            gridRecycler.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP && !isEditMode) {
                    View focused = gridRecycler.getFocusedChild();
                    if (focused != null) {
                        int pos = gridRecycler.getChildAdapterPosition(focused);
                        if (pos >= 0 && pos < columns) {
                            closeGridView();
                            if (dockRecycler != null && !dockApps.isEmpty()) {
                                final int target = Math.min(lastDockPosition, Math.max(0, dockApps.size() - 1));
                                dockRecycler.post(() -> {
                                    dockRecycler.scrollToPosition(target);
                                    RecyclerView.ViewHolder vh = dockRecycler.findViewHolderForAdapterPosition(target);
                                    if (vh != null) vh.itemView.requestFocus();
                                    else if (dockRecycler.getChildCount() > 0) dockRecycler.getChildAt(0).requestFocus();
                                });
                            }
                            return true;
                        }
                    }
                }
                return false;
            });
        }
    }

    private void scrollToGrid() {
        if (mainScrollView == null || gridRecycler == null) return;
        mainScrollView.post(() -> {
            int target = gridRecycler.getTop() - dpToPx(16);
            if (target < 0) target = 0;
            smoothScrollWithBounce(mainScrollView, target, 450);
        });
    }

    private void smoothScrollWithBounce(final ScrollView scrollView, final int targetY, final int duration) {
        final int startY = scrollView.getScrollY();
        final int delta = targetY - startY;
        final OvershootInterpolator interpolator = new OvershootInterpolator(1.05f);
        final long startTime = android.os.SystemClock.uptimeMillis();
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = android.os.SystemClock.uptimeMillis() - startTime;
                float t = Math.min(1f, elapsed / (float) duration);
                float eased = interpolator.getInterpolation(t);
                scrollView.scrollTo(0, startY + Math.round(delta * eased));
                if (t < 1f) handler.postDelayed(this, 16);
            }
        });
    }

    private void openGridView() {
        if (isGridViewOpen) return;
        isGridViewOpen = true;
        if (gridDarkOverlay != null) gridDarkOverlay.animate().alpha(1f).setDuration(300).start();
    }

    private void closeGridView() {
        if (!isGridViewOpen) return;
        isGridViewOpen = false;
        if (gridDarkOverlay != null) gridDarkOverlay.animate().alpha(0f).setDuration(300).start();
        if (mainScrollView != null) smoothScrollWithBounce(mainScrollView, 0, 400);
    }

    // ==================== РЕЖИМ РЕДАКТИРОВАНИЯ ====================
    private void enterEditMode() {
        if (isEditMode) return;
        isEditMode = true;
        grabbedItem = null;
        if (heroTitle != null) {
            heroTitle.setAlpha(1f);
            heroTitle.setText("Редактирование");
        }
        if (gridRecycler != null && gridRecycler.getAdapter() != null) gridRecycler.getAdapter().notifyDataSetChanged();
        if (dockRecycler != null && dockRecycler.getAdapter() != null) dockRecycler.getAdapter().notifyDataSetChanged();
        resetEditTimeout();
    }

    private void exitEditMode() {
        if (!isEditMode) return;
        isEditMode = false;
        grabbedItem = null;
        saveLayout();
        if (editTimeoutRunnable != null) {
            editHandler.removeCallbacks(editTimeoutRunnable);
            editTimeoutRunnable = null;
        }
        if (heroTitle != null) heroTitle.setText("Apple TV");
        if (gridRecycler != null && gridRecycler.getAdapter() != null) gridRecycler.getAdapter().notifyDataSetChanged();
        if (dockRecycler != null && dockRecycler.getAdapter() != null) dockRecycler.getAdapter().notifyDataSetChanged();
    }

    private void resetEditTimeout() {
        if (editTimeoutRunnable != null) editHandler.removeCallbacks(editTimeoutRunnable);
        editTimeoutRunnable = () -> exitEditMode();
        editHandler.postDelayed(editTimeoutRunnable, 30000);
    }

    private void saveLayout() {
        StringBuilder dockSb = new StringBuilder();
        for (AppItem item : dockApps) dockSb.append(item.packageName).append(",");
        prefs.edit().putString("favorites", dockSb.toString()).apply();

        StringBuilder gridSb = new StringBuilder();
        for (AppItem item : gridApps) gridSb.append(item.packageName).append(",");
        prefs.edit().putString("grid_order", gridSb.toString()).apply();
    }

    private void moveInDock(AppItem item, int dir) {
        int idx = dockApps.indexOf(item);
        if (idx < 0) return;
        int newIdx = idx + dir;
        if (newIdx < 0 || newIdx >= dockApps.size()) return;
        Collections.swap(dockApps, idx, newIdx);
        if (dockRecycler != null && dockRecycler.getAdapter() != null) dockRecycler.getAdapter().notifyDataSetChanged();
        focusDockItem(newIdx);
        resetEditTimeout();
    }

    private void moveInGrid(AppItem item, int dir) {
        int idx = gridApps.indexOf(item);
        if (idx < 0) return;
        int newIdx = idx + dir;
        if (newIdx < 0 || newIdx >= gridApps.size()) return;
        Collections.swap(gridApps, idx, newIdx);
        if (gridRecycler != null && gridRecycler.getAdapter() != null) gridRecycler.getAdapter().notifyDataSetChanged();
        focusGridItem(newIdx);
        resetEditTimeout();
    }

    private void focusDockItem(int pos) {
        if (dockRecycler == null) return;
        final int target = pos;
        dockRecycler.post(() -> {
            RecyclerView.ViewHolder vh = dockRecycler.findViewHolderForAdapterPosition(target);
            if (vh != null) vh.itemView.requestFocus();
            else if (dockRecycler.getChildCount() > 0) dockRecycler.getChildAt(0).requestFocus();
        });
    }

    private void focusGridItem(int pos) {
        if (gridRecycler == null) return;
        final int target = pos;
        gridRecycler.post(() -> {
            gridRecycler.scrollToPosition(target);
            RecyclerView.ViewHolder vh = gridRecycler.findViewHolderForAdapterPosition(target);
            if (vh != null) vh.itemView.requestFocus();
            else if (gridRecycler.getChildCount() > 0) gridRecycler.getChildAt(0).requestFocus();
        });
    }

    private void addToDock(AppItem item) {
        if (dockApps.contains(item)) return;
        dockApps.add(item);
        gridApps.remove(item);
        saveLayout();
        if (gridRecycler != null && gridRecycler.getAdapter() != null) gridRecycler.getAdapter().notifyDataSetChanged();
        if (dockRecycler != null && dockRecycler.getAdapter() != null) dockRecycler.getAdapter().notifyDataSetChanged();
        focusDockItem(dockApps.size() - 1);
        resetEditTimeout();
    }

    private void removeFromDock(AppItem item) {
        if (!dockApps.contains(item)) return;
        dockApps.remove(item);
        gridApps.add(0, item);
        saveLayout();
        if (dockRecycler != null && dockRecycler.getAdapter() != null) dockRecycler.getAdapter().notifyDataSetChanged();
        if (gridRecycler != null && gridRecycler.getAdapter() != null) gridRecycler.getAdapter().notifyDataSetChanged();
        focusGridItem(0);
        resetEditTimeout();
    }

    private void saveFavorites() {
        StringBuilder sb = new StringBuilder();
        for (AppItem item : dockApps) sb.append(item.packageName).append(",");
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
            if (!allApps.contains(app)) allApps.add(app);
        }

        Collections.sort(allApps, (a, b) -> a.label.compareToIgnoreCase(b.label));

        String favs = prefs.getString("favorites", "");

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
                if (!p.trim().isEmpty() && !favList.contains(p.trim())) favList.add(p.trim());
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
                if (f.packageName.equals(app.packageName)) { inFav = true; break; }
            }
            if (!inFav && !gridApps.contains(app)) gridApps.add(app);
        }

        String gridOrder = prefs.getString("grid_order", "");
        if (!gridOrder.isEmpty()) {
            final List<String> order = new ArrayList<>();
            for (String p : gridOrder.split(",")) {
                if (!p.trim().isEmpty()) order.add(p.trim());
            }
            Collections.sort(gridApps, (a, b) -> {
                int ia = order.indexOf(a.packageName);
                int ib = order.indexOf(b.packageName);
                if (ia == -1 && ib == -1) return a.label.compareToIgnoreCase(b.label);
                if (ia == -1) return 1;
                if (ib == -1) return -1;
                return Integer.compare(ia, ib);
            });
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
        TextView dotsMenu;
        TextView actionButton;

        ObjectAnimator shimmerAnim;
        AnimatorSet scaleAnim;
        ObjectAnimator jiggleAnim;

        public AppViewHolder(LinearLayout root, FrameLayout card, ImageView bgImage, ImageView iconImage, TextView localLabel, View glareOverlay, TextView dotsMenu, TextView actionButton) {
            super(root);
            this.root = root;
            this.card = card;
            this.bgImage = bgImage;
            this.iconImage = iconImage;
            this.localLabel = localLabel;
            this.glareOverlay = glareOverlay;
            this.dotsMenu = dotsMenu;
            this.actionButton = actionButton;
        }

        public void startJiggle() {
            if (jiggleAnim != null) return;
            jiggleAnim = ObjectAnimator.ofFloat(card, "rotation", -2f, 2f);
            jiggleAnim.setDuration(120);
            jiggleAnim.setRepeatMode(ValueAnimator.REVERSE);
            jiggleAnim.setRepeatCount(ValueAnimator.INFINITE);
            jiggleAnim.start();
        }

        public void stopJiggle() {
            if (jiggleAnim != null) {
                jiggleAnim.cancel();
                jiggleAnim = null;
            }
            card.setRotation(0f);
        }

        public void resetState() {
            if (scaleAnim != null) scaleAnim.cancel();
            if (shimmerAnim != null) shimmerAnim.cancel();
            stopJiggle();
            card.setScaleX(1.0f);
            card.setScaleY(1.0f);
            card.setAlpha(1.0f);
            card.setTranslationZ(dpToPx(4));
            glareOverlay.setAlpha(0f);
            localLabel.setAlpha(0f);
            dotsMenu.setVisibility(View.GONE);
            actionButton.setVisibility(View.GONE);
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
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rootLp.setMargins(itemMargin, itemMargin, itemMargin, itemMargin);
            root.setLayoutParams(rootLp);

            FrameLayout card = new FrameLayout(ctx);
            card.setFocusable(false);
            card.setClickable(false);
            card.setElevation(dpToPx(4));
            card.setLayoutParams(new LinearLayout.LayoutParams(cardWidth, cardHeight));
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(12));
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

            TextView dotsMenu = new TextView(ctx);
            dotsMenu.setText("⋯");
            dotsMenu.setTextColor(Color.WHITE);
            dotsMenu.setTextSize(20f);
            dotsMenu.setGravity(Gravity.CENTER);
            dotsMenu.setBackgroundColor(0x99000000);
            dotsMenu.setFocusable(true);
            dotsMenu.setClickable(true);
            dotsMenu.setVisibility(View.GONE);
            FrameLayout.LayoutParams dotsLp = new FrameLayout.LayoutParams(dpToPx(28), dpToPx(28), Gravity.TOP | Gravity.END);
            dotsLp.setMargins(0, dpToPx(4), dpToPx(4), 0);
            dotsMenu.setLayoutParams(dotsLp);
            card.addView(dotsMenu);

            TextView actionButton = new TextView(ctx);
            actionButton.setTextColor(Color.WHITE);
            actionButton.setTextSize(11f);
            actionButton.setGravity(Gravity.CENTER);
            actionButton.setBackgroundColor(0xCC000000);
            actionButton.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            actionButton.setFocusable(true);
            actionButton.setClickable(true);
            actionButton.setVisibility(View.GONE);
            FrameLayout.LayoutParams actionLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END);
            actionLp.setMargins(0, dpToPx(36), dpToPx(4), 0);
            actionButton.setLayoutParams(actionLp);
            card.addView(actionButton);

            root.addView(card);

            TextView localLabel = new TextView(ctx);
            localLabel.setTextSize(13f);
            localLabel.setTextColor(Color.WHITE);
            localLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            localLabel.setSingleLine(true);
            localLabel.setGravity(Gravity.CENTER);
            localLabel.setAlpha(0f);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelLp.setMargins(0, dpToPx(8), 0, 0);
            localLabel.setLayoutParams(labelLp);
            if (isDock) localLabel.setVisibility(View.GONE);
            root.addView(localLabel);

            return new AppViewHolder(root, card, bgImage, iconImage, localLabel, glareOverlay, dotsMenu, actionButton);
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

            if (isEditMode) {
                vh.startJiggle();
                vh.dotsMenu.setVisibility(View.VISIBLE);
                if (grabbedItem == item) {
                    vh.card.setScaleX(1.15f);
                    vh.card.setScaleY(1.15f);
                }
            }

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
                    int iconSize = dpToPx(56);
                    vh.iconImage.setLayoutParams(new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
                    vh.iconImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
                vh.iconImage.setImageDrawable(item.icon);
                vh.iconImage.setVisibility(View.VISIBLE);
            }

            vh.dotsMenu.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && isEditMode) {
                    vh.actionButton.setText(isDock ? "Убрать из избранного" : "Добавить в избранное");
                    vh.actionButton.setVisibility(View.VISIBLE);
                } else {
                    vh.actionButton.setVisibility(View.GONE);
                }
            });

            vh.dotsMenu.setOnClickListener(v -> {
                if (isEditMode) {
                    vh.actionButton.setText(isDock ? "Убрать из избранного" : "Добавить в избранное");
                    vh.actionButton.setVisibility(View.VISIBLE);
                    vh.actionButton.requestFocus();
                }
            });

            vh.actionButton.setOnClickListener(v -> {
                if (!isEditMode) return;
                if (isDock) removeFromDock(item);
                else addToDock(item);
            });

            vh.actionButton.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) vh.actionButton.setVisibility(View.GONE);
            });

            vh.root.setOnClickListener(v -> {
                if (isEditMode) {
                    if (grabbedItem == item) {
                        grabbedItem = null;
                        if (gridRecycler != null && gridRecycler.getAdapter() != null) gridRecycler.getAdapter().notifyDataSetChanged();
                        if (dockRecycler != null && dockRecycler.getAdapter() != null) dockRecycler.getAdapter().notifyDataSetChanged();
                        return;
                    }
                    if (grabbedItem == null) {
                        grabbedItem = item;
                        vh.card.setScaleX(1.15f);
                        vh.card.setScaleY(1.15f);
                        if (gridRecycler != null && gridRecycler.getAdapter() != null) gridRecycler.getAdapter().notifyDataSetChanged();
                        if (dockRecycler != null && dockRecycler.getAdapter() != null) dockRecycler.getAdapter().notifyDataSetChanged();
                    }
                    return;
                }
                if (VIRTUAL_SETTINGS_PKG.equals(item.packageName)) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return;
                }
                PackageManager pm = getPackageManager();
                Intent launch = pm.getLeanbackLaunchIntentForPackage(item.packageName);
                if (launch == null) launch = pm.getLaunchIntentForPackage(item.packageName);
                if (launch != null) {
                    try {
                        android.graphics.Rect rect = new android.graphics.Rect();
                        vh.card.getGlobalVisibleRect(rect);
                        android.app.ActivityOptions opts = android.app.ActivityOptions.makeScaleUpAnimation(
                                vh.card, rect.left, rect.top, rect.width(), rect.height());
                        startActivity(launch, opts.toBundle());
                    } catch (Exception e) {
                        startActivity(launch);
                    }
                }
            });

            vh.root.setOnLongClickListener(v -> {
                if (!isEditMode) {
                    enterEditMode();
                    return true;
                }
                return false;
            });

            vh.root.setOnKeyListener((v, keyCode, event) -> {
                if (!isEditMode || grabbedItem != item) return false;
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) { if (isDock) moveInDock(item, -1); else moveInGrid(item, -1); return true; }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) { if (isDock) moveInDock(item, 1); else moveInGrid(item, 1); return true; }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && !isDock) { moveInGrid(item, -6); return true; }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && !isDock) { moveInGrid(item, 6); return true; }
                return false;
            });

            vh.root.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    SoundManager.playFocus();
                    if (isDock) lastDockPosition = vh.getAdapterPosition();
                    if (vh.scaleAnim != null) vh.scaleAnim.cancel();
                    vh.scaleAnim = new AnimatorSet();
                    ObjectAnimator sx = ObjectAnimator.ofFloat(vh.card, "scaleX", 1.12f);
                    ObjectAnimator sy = ObjectAnimator.ofFloat(vh.card, "scaleY", 1.12f);
                    ObjectAnimator sz = ObjectAnimator.ofFloat(vh.card, "translationZ", dpToPx(30));
                    vh.scaleAnim.playTogether(sx, sy, sz);
                    vh.scaleAnim.setDuration(350);
                    vh.scaleAnim.setInterpolator(new OvershootInterpolator(1.1f));
                    vh.scaleAnim.start();

                    BannerManager.getInstance().updateBackground(MainActivity.this, item.packageName);
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
                        if (heroTitle != null && !isEditMode) {
                            heroTitle.animate().alpha(1f).setDuration(200).start();
                            heroTitle.setText(item.label);
                        }
                        if (timePillContainer != null) timePillContainer.animate().alpha(1f).setDuration(200).start();
                    } else {
                        if (heroTitle != null && !isEditMode) heroTitle.animate().alpha(0f).setDuration(200).start();
                        if (timePillContainer != null) timePillContainer.animate().alpha(0f).setDuration(200).start();
                        vh.localLabel.animate().alpha(1f).setDuration(200).start();
                    }
                } else {
                    if (vh.scaleAnim != null) vh.scaleAnim.cancel();
                    vh.scaleAnim = new AnimatorSet();
                    ObjectAnimator sx = ObjectAnimator.ofFloat(vh.card, "scaleX", grabbedItem == item ? 1.15f : 1.0f);
                    ObjectAnimator sy = ObjectAnimator.ofFloat(vh.card, "scaleY", grabbedItem == item ? 1.15f : 1.0f);
                    ObjectAnimator sz = ObjectAnimator.ofFloat(vh.card, "translationZ", dpToPx(4));
                    vh.scaleAnim.playTogether(sx, sy, sz);
                    vh.scaleAnim.setDuration(250);
                    vh.scaleAnim.setInterpolator(new DecelerateInterpolator(1.5f));
                    vh.scaleAnim.start();
                    if (vh.shimmerAnim != null) vh.shimmerAnim.cancel();
                    vh.glareOverlay.animate().alpha(0f).setDuration(200).start();
                    if (!isDock) vh.localLabel.animate().alpha(0f).setDuration(200).start();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) exitEditMode();
        else super.onBackPressed();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (isEditMode) resetEditTimeout();
        SettingsManager.startSleepTimer(this);
    }
}
