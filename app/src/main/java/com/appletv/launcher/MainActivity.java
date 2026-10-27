package com.appletv.launcher;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ImageView heroBanner;
    private View ambientGlow;
    private TextView heroTitle;
    private RecyclerView dockRecycler;
    private FrameLayout btnSettings;
    private TextClock mainTextClock;
    private TextView tvDateDisplay;

    private FrameLayout appDrawerOverlay;
    private RecyclerView drawerRecycler;

    private FrameLayout settingsOverlay;
    private FrameLayout tilesManagerOverlay;
    private RecyclerView tilesManagerRecycler;

    private FrameLayout screensaverOverlay;
    private ImageView screensaverBg;
    private TextClock saverTextClock;
    private TextView saverDate;
    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private Runnable idleRunnable;

    private TextView settingsPreviewIcon;
    private TextView settingsPreviewTitle;
    private TextView settingsPreviewDesc;

    private TextView tvSoundVal;
    private TextView tvAmbientVal;
    private TextView tvSizeVal;
    private TextView tvClockVal;
    private TextView tvSaverVal;
    private TextView tvAutostartVal;

    private AppAdapter dockAdapter;
    private DrawerAdapter drawerAdapter;
    private TilesManagerAdapter tilesAdapter;

    private final List<AppItem> homeApps = new ArrayList<>();
    private final List<AppItem> allInstalledApps = new ArrayList<>();
    private SharedPreferences prefs;

    private int currentAmbientColor = 0xFF14101A;

    public static class AppItem {
        public String packageName;
        public String label;
        public Drawable icon;
        public int[] colors;
        public int glowColor;
        public int bannerRes;
        public boolean isActionAllApps = false;

        public AppItem(String packageName, String label, Drawable icon, int[] colors, int glowColor, int bannerRes) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.colors = colors;
            this.glowColor = glowColor;
            this.bannerRes = bannerRes;
        }

        public AppItem(String label, int[] colors, boolean isActionAllApps) {
            this.label = label;
            this.colors = colors;
            this.glowColor = 0xFF353545;
            this.bannerRes = android.R.color.black;
            this.isActionAllApps = isActionAllApps;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("tvos_launcher_prefs", Context.MODE_PRIVATE);

        heroBanner = findViewById(R.id.hero_banner);
        ambientGlow = findViewById(R.id.ambient_glow);
        heroTitle = findViewById(R.id.hero_title);
        
        if (heroTitle != null) {
            heroTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            heroTitle.setLetterSpacing(-0.03f);
        }

        dockRecycler = findViewById(R.id.dock_recycler);
        btnSettings = findViewById(R.id.btn_launcher_settings);
        mainTextClock = findViewById(R.id.main_text_clock);
        tvDateDisplay = findViewById(R.id.tv_date_display);

        appDrawerOverlay = findViewById(R.id.app_drawer_overlay);
        drawerRecycler = findViewById(R.id.drawer_recycler);

        settingsOverlay = findViewById(R.id.settings_overlay);
        tilesManagerOverlay = findViewById(R.id.tiles_manager_overlay);
        tilesManagerRecycler = findViewById(R.id.tiles_manager_recycler);

        screensaverOverlay = findViewById(R.id.screensaver_overlay);
        screensaverBg = findViewById(R.id.screensaver_bg);
        saverTextClock = findViewById(R.id.saver_text_clock);
        saverDate = findViewById(R.id.saver_date);

        settingsPreviewIcon = findViewById(R.id.settings_preview_icon);
        settingsPreviewTitle = findViewById(R.id.settings_preview_title);
        settingsPreviewDesc = findViewById(R.id.settings_preview_desc);

        tvSoundVal = findViewById(R.id.tv_setting_sound_val);
        tvAmbientVal = findViewById(R.id.tv_setting_ambient_val);
        tvSizeVal = findViewById(R.id.tv_setting_size_val);
        tvClockVal = findViewById(R.id.tv_setting_clock_val);
        tvSaverVal = findViewById(R.id.tv_setting_saver_val);
        tvAutostartVal = findViewById(R.id.tv_setting_autostart_val);

        if (dockRecycler != null) {
            dockRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            dockAdapter = new AppAdapter();
            dockRecycler.setAdapter(dockAdapter);
        }

        if (drawerRecycler != null) {
            drawerRecycler.setLayoutManager(new GridLayoutManager(this, 5));
            drawerAdapter = new DrawerAdapter();
            drawerRecycler.setAdapter(drawerAdapter);
        }

        if (tilesManagerRecycler != null) {
            tilesManagerRecycler.setLayoutManager(new GridLayoutManager(this, 2));
            tilesAdapter = new TilesManagerAdapter();
            tilesManagerRecycler.setAdapter(tilesAdapter);
        }

        initSettingsListeners();
        updateDateAndClockUi();
        applyPreferencesUi();
        setupScreensaverTimer();

        loadApps();
        checkAutostart();
    }

    private void updateDateAndClockUi() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", new Locale("ru"));
        String dateStr = sdf.format(new Date());
        if (!dateStr.isEmpty()) {
            dateStr = Character.toUpperCase(dateStr.charAt(0)) + dateStr.substring(1);
        }
        if (tvDateDisplay != null) tvDateDisplay.setText(dateStr);
        if (saverDate != null) saverDate.setText(dateStr);

        boolean is24 = prefs.getBoolean("clock_24h", true);
        if (is24) {
            if (mainTextClock != null) { mainTextClock.setFormat24Hour("HH:mm"); mainTextClock.setFormat12Hour("HH:mm"); }
            if (saverTextClock != null) { saverTextClock.setFormat24Hour("HH:mm"); saverTextClock.setFormat12Hour("HH:mm"); }
        } else {
            if (mainTextClock != null) { mainTextClock.setFormat24Hour("h:mm a"); mainTextClock.setFormat12Hour("h:mm a"); }
            if (saverTextClock != null) { saverTextClock.setFormat24Hour("h:mm a"); saverTextClock.setFormat12Hour("h:mm a"); }
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void applyPreferencesUi() {
        boolean soundOn = prefs.getBoolean("sfx_enabled", true);
        if (tvSoundVal != null) tvSoundVal.setText(soundOn ? "Включены" : "Выключены");

        int ambientLevel = prefs.getInt("ambient_level", 55);
        if (ambientGlow != null) {
            if (ambientLevel == 0) {
                if (tvAmbientVal != null) tvAmbientVal.setText("Выключена");
                ambientGlow.setVisibility(View.GONE);
            } else {
                ambientGlow.setVisibility(View.VISIBLE);
                ambientGlow.setAlpha(ambientLevel / 100f);
                if (tvAmbientVal != null) tvAmbientVal.setText(ambientLevel == 30 ? "Мягкая (30%)" : (ambientLevel == 55 ? "Стандарт (55%)" : "Яркая (80%)"));
            }
        }

        String sizeMode = prefs.getString("tiles_size", "normal");
        if (tvSizeVal != null) tvSizeVal.setText(sizeMode.equals("compact") ? "Компактный" : (sizeMode.equals("large") ? "Крупный" : "Стандарт tvOS"));

        if (tvClockVal != null) tvClockVal.setText(prefs.getBoolean("clock_24h", true) ? "24-часовой" : "12-часовой (AM/PM)");

        int saverMinutes = prefs.getInt("screensaver_timeout", 5);
        if (tvSaverVal != null) tvSaverVal.setText(saverMinutes == 0 ? "Отключено" : "Через " + saverMinutes + " мин");

        String autostartPkg = prefs.getString("autostart_package", "none");
        if (tvAutostartVal != null) {
            if (autostartPkg.equals("none")) {
                tvAutostartVal.setText("Главный экран");
            } else {
                tvAutostartVal.setText(prefs.getString("autostart_label", "Приложение"));
            }
        }
    }

    private void checkAutostart() {
        boolean alreadyStarted = prefs.getBoolean("session_started", false);
        String autostartPkg = prefs.getString("autostart_package", "none");
        if (!alreadyStarted && !autostartPkg.equals("none")) {
            prefs.edit().putBoolean("session_started", true).apply();
            try {
                PackageManager pm = getPackageManager();
                Intent launch = pm.getLeanbackLaunchIntentForPackage(autostartPkg);
                if (launch == null) launch = pm.getLaunchIntentForPackage(autostartPkg);
                if (launch != null) startActivity(launch);
            } catch (Exception ignored) {}
        }
    }

    private void setupScreensaverTimer() {
        int minutes = prefs.getInt("screensaver_timeout", 5);
        if (idleRunnable != null) idleHandler.removeCallbacks(idleRunnable);
        if (minutes <= 0) return;

        idleRunnable = () -> {
            if (screensaverOverlay != null && screensaverOverlay.getVisibility() != View.VISIBLE) {
                openScreensaver();
            }
        };
        resetScreensaverTimer();
    }

    private void resetScreensaverTimer() {
        int minutes = prefs.getInt("screensaver_timeout", 5);
        if (minutes > 0 && idleRunnable != null) {
            idleHandler.removeCallbacks(idleRunnable);
            idleHandler.postDelayed(idleRunnable, minutes * 60 * 1000L);
        }
    }

    private void openScreensaver() {
        if (screensaverOverlay != null) {
            screensaverOverlay.setVisibility(View.VISIBLE);
            screensaverOverlay.setAlpha(0f);
            screensaverOverlay.animate().alpha(1f).setDuration(400).start();
            screensaverOverlay.requestFocus();
        }
    }

    private void closeScreensaver() {
        if (screensaverOverlay != null) {
            screensaverOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                screensaverOverlay.setVisibility(View.GONE);
                if (dockRecycler != null) dockRecycler.requestFocus();
            }).start();
        }
        resetScreensaverTimer();
    }

    private void openSettings() {
        if (settingsOverlay != null) {
            settingsOverlay.setVisibility(View.VISIBLE);
            settingsOverlay.setAlpha(0f);
            settingsOverlay.animate().alpha(1f).setDuration(200).start();
        }
    }

    private void initSettingsListeners() {
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings());
            btnSettings.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    animateScale(v, 1.12f, 1.12f, dpToPx(8));
                    if (heroTitle != null) heroTitle.setText("Настройки");
                } else {
                    animateScale(v, 1.0f, 1.0f, 0f);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (idleRunnable != null) idleHandler.removeCallbacks(idleRunnable);
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Set<String> hiddenApps = prefs.getStringSet("hidden_apps", new HashSet<>());
        List<String> pinnedOrder = loadPinnedOrder();

        Intent tvIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(tvIntent, 0);
        if (infos.isEmpty()) {
            Intent phoneIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            infos = pm.queryIntentActivities(phoneIntent, 0);
        }

        allInstalledApps.clear();
        List<AppItem> visibleApps = new ArrayList<>();

        for (ResolveInfo ri : infos) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;

            String label = ri.loadLabel(pm).toString();
            Drawable icon = ri.loadIcon(pm);
            int[] colors = getThemeColorsForPackage(pkg, icon);
            int glowColor = getGlowColorForPackage(pkg, icon);
            int banner = getPosterForPackage(pkg);

            AppItem item = new AppItem(pkg, label, icon, colors, glowColor, banner);
            allInstalledApps.add(item);

            if (!hiddenApps.contains(pkg)) {
                visibleApps.add(item);
            }
        }

        if (pinnedOrder.isEmpty()) {
            for (AppItem app : visibleApps) {
                pinnedOrder.add(app.packageName);
            }
            savePinnedOrder(pinnedOrder);
        }

        homeApps.clear();
        for (String pkg : pinnedOrder) {
            for (AppItem item : visibleApps) {
                if (item.packageName.equals(pkg)) {
                    homeApps.add(item);
                    break;
                }
            }
        }

        homeApps.add(new AppItem("Все приложения", new int[]{0xFF242430, 0xFF121218}, true));

        if (dockAdapter != null) dockAdapter.notifyDataSetChanged();
        if (drawerAdapter != null) drawerAdapter.notifyDataSetChanged();
        if (tilesAdapter != null) tilesAdapter.notifyDataSetChanged();
    }

    private void animateAmbientGlow(int targetColor) {
        if (prefs.getInt("ambient_level", 55) == 0 || ambientGlow == null) return;

        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), currentAmbientColor, targetColor);
        colorAnim.setDuration(260);
        colorAnim.addUpdateListener(animator -> {
            int c = (int) animator.getAnimatedValue();
            currentAmbientColor = c;
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{c, 0x00050508}
            );
            ambientGlow.setBackground(gd);
        });
        colorAnim.start();
    }

    private List<String> loadPinnedOrder() {
        List<String> list = new ArrayList<>();
        try {
            String json = prefs.getString("pinned_order", null);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private void savePinnedOrder(List<String> list) {
        JSONArray arr = new JSONArray();
        for (String s : list) arr.put(s);
        prefs.edit().putString("pinned_order", arr.toString()).apply();
    }

    private int extractDominantColor(Drawable drawable) {
        if (drawable == null) return 0xFF353545;
        try {
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                bitmap = Bitmap.createBitmap(
                        Math.max(1, drawable.getIntrinsicWidth()),
                        Math.max(1, drawable.getIntrinsicHeight()),
                        Bitmap.Config.ARGB_8888
                );
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
            if (bitmap != null) {
                Bitmap small = Bitmap.createScaledBitmap(bitmap, 16, 16, false);
                long r = 0, g = 0, b = 0;
                int count = 0;
                for (int x = 2; x < 14; x++) {
                    for (int y = 2; y < 14; y++) {
                        int pixel = small.getPixel(x, y);
                        if (Color.alpha(pixel) > 120) {
                            r += Color.red(pixel);
                            g += Color.green(pixel);
                            b += Color.blue(pixel);
                            count++;
                        }
                    }
                }
                if (count > 0) {
                    return Color.rgb((int)(r / count), (int)(g / count), (int)(b / count));
                }
            }
        } catch (Exception ignored) {}
        return 0xFF2A2E3D;
    }

    private int[] getThemeColorsForPackage(String pkg, Drawable icon) {
        int dom = extractDominantColor(icon);
        int darker = Color.rgb(
                (int)(Color.red(dom) * 0.45f),
                (int)(Color.green(dom) * 0.45f),
                (int)(Color.blue(dom) * 0.45f)
        );
        return new int[]{dom, darker};
    }

    private int getGlowColorForPackage(String pkg, Drawable icon) {
        int dom = extractDominantColor(icon);
        return Color.argb(160, Color.red(dom), Color.green(dom), Color.blue(dom));
    }

    private int getPosterForPackage(String pkg) {
        return android.R.color.black;
    }

    private class AppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout card = new FrameLayout(parent.getContext());
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(dpToPx(140), dpToPx(82));
            lp.setMargins(dpToPx(10), dpToPx(12), dpToPx(10), dpToPx(12));
            card.setLayoutParams(lp);
            card.setFocusable(true);
            card.setClickable(true);

            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(18));
                }
            });
            card.setClipToOutline(true);

            ImageView iv = new ImageView(parent.getContext());
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(iv, new FrameLayout.LayoutParams(dpToPx(48), dpToPx(48), Gravity.CENTER));

            TextView tv = new TextView(parent.getContext());
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(14);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            card.addView(tv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            return new RecyclerView.ViewHolder(card) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AppItem item = homeApps.get(position);
            FrameLayout card = (FrameLayout) holder.itemView;

            ImageView iv = (ImageView) card.getChildAt(0);
            TextView tv = (TextView) card.getChildAt(1);

            if (item.isActionAllApps) {
                iv.setVisibility(View.GONE);
                tv.setVisibility(View.VISIBLE);
                tv.setText("⊞\nВсе");
            } else {
                tv.setVisibility(View.GONE);
                iv.setVisibility(View.VISIBLE);
                iv.setImageDrawable(item.icon);
            }

            GradientDrawable normalBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, item.colors);
            normalBg.setCornerRadius(dpToPx(18));
            card.setBackground(normalBg);

            card.setOnFocusChangeListener((v, hasFocus) -> {
                resetScreensaverTimer();
                if (hasFocus) {
                    animateScale(v, 1.12f, 1.12f, dpToPx(16));
                    GradientDrawable focusBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, item.colors);
                    focusBg.setCornerRadius(dpToPx(18));
                    v.setBackground(focusBg);

                    if (heroTitle != null) heroTitle.setText(item.label);
                    animateAmbientGlow(item.glowColor);
                } else {
                    animateScale(v, 1.0f, 1.0f, 0f);
                    v.setBackground(normalBg);
                }
            });

            card.setOnClickListener(v -> {
                resetScreensaverTimer();
                if (item.isActionAllApps) {
                    if (appDrawerOverlay != null) appDrawerOverlay.setVisibility(View.VISIBLE);
                    if (drawerRecycler != null) drawerRecycler.requestFocus();
                } else {
                    PackageManager pm = getPackageManager();
                    Intent launch = pm.getLeanbackLaunchIntentForPackage(item.packageName);
                    if (launch == null) launch = pm.getLaunchIntentForPackage(item.packageName);
                    if (launch != null) startActivity(launch);
                }
            });
        }

        @Override
        public int getItemCount() {
            return homeApps.size();
        }
    }

    private class DrawerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout card = new FrameLayout(parent.getContext());
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(dpToPx(156), dpToPx(92));
            lp.setMargins(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            card.setLayoutParams(lp);
            card.setFocusable(true);
            card.setClickable(true);

            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(18));
                }
            });
            card.setClipToOutline(true);

            ImageView iv = new ImageView(parent.getContext());
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(iv, new FrameLayout.LayoutParams(dpToPx(44), dpToPx(44), Gravity.CENTER));

            TextView tv = new TextView(parent.getContext());
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(12);
            tv.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            tv.setPadding(dpToPx(8), 0, dpToPx(8), dpToPx(8));
            card.addView(tv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            return new RecyclerView.ViewHolder(card) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AppItem item = allInstalledApps.get(position);
            FrameLayout card = (FrameLayout) holder.itemView;
            ImageView iv = (ImageView) card.getChildAt(0);
            TextView tv = (TextView) card.getChildAt(1);

            iv.setImageDrawable(item.icon);
            tv.setText(item.label);

            GradientDrawable normalBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, item.colors);
            normalBg.setCornerRadius(dpToPx(18));
            card.setBackground(normalBg);

            card.setOnFocusChangeListener((v, hasFocus) -> {
                resetScreensaverTimer();
                if (hasFocus) {
                    animateScale(v, 1.12f, 1.12f, dpToPx(10));
                } else {
                    animateScale(v, 1.0f, 1.0f, 0f);
                }
            });

            card.setOnClickListener(v -> {
                resetScreensaverTimer();
                PackageManager pm = getPackageManager();
                Intent launch = pm.getLeanbackLaunchIntentForPackage(item.packageName);
                if (launch == null) launch = pm.getLaunchIntentForPackage(item.packageName);
                if (launch != null) startActivity(launch);
            });
        }

        @Override
        public int getItemCount() {
            return allInstalledApps.size();
        }
    }

    private class TilesManagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout row = new FrameLayout(parent.getContext());
            row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(56)));
            return new RecyclerView.ViewHolder(row) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {}

        @Override
        public int getItemCount() { return 0; }
    }

    private void animateScale(View view, float sx, float sy, float elev) {
        ObjectAnimator ax = ObjectAnimator.ofFloat(view, "scaleX", sx);
        ObjectAnimator ay = ObjectAnimator.ofFloat(view, "scaleY", sy);
        ObjectAnimator az = ObjectAnimator.ofFloat(view, "translationZ", elev);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(ax, ay, az);
        set.setDuration(200);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }
}
