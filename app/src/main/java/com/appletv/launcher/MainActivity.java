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
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private TextView heroTitle;
    private View ambientGlowBg;
    private LinearLayout dockContainerLayout;
    private RelativeLayout gridContainer;
    private RecyclerView dockRecycler;
    private RecyclerView gridRecycler;
    private FrameLayout btnSettings;
    
    private final List<AppItem> dockApps = new ArrayList<>();
    private final List<AppItem> gridApps = new ArrayList<>();
    private SharedPreferences prefs;
    private int currentGlowColor = 0xFF22222E;
    private boolean isGridViewActive = false;

    public static class AppItem {
        public String packageName;
        public String label;
        public Drawable icon;
        public int dominantColor;

        public AppItem(String packageName, String label, Drawable icon, int dominantColor) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.dominantColor = dominantColor;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);

        heroTitle = findViewById(R.id.hero_title);
        ambientGlowBg = findViewById(R.id.ambient_glow_bg);
        dockContainerLayout = findViewById(R.id.dock_container_layout);
        gridContainer = findViewById(R.id.grid_container);
        dockRecycler = findViewById(R.id.dock_recycler);
        gridRecycler = findViewById(R.id.grid_recycler);
        btnSettings = findViewById(R.id.btn_launcher_settings);

        if (heroTitle != null) {
            heroTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        }

        if (dockRecycler != null) {
            dockRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            dockRecycler.setAdapter(new TvosAppAdapter(dockApps, true));
        }

        if (gridRecycler != null) {
            gridRecycler.setLayoutManager(new GridLayoutManager(this, 6));
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (!isGridViewActive && (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN)) {
                showGridView();
                if (gridRecycler != null && gridRecycler.getChildCount() > 0) {
                    gridRecycler.getChildAt(0).requestFocus();
                }
                return true;
            } else if (isGridViewActive && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                View focusedChild = gridRecycler.getFocusedChild();
                int pos = focusedChild != null ? gridRecycler.getChildAdapterPosition(focusedChild) : -1;
                if (pos >= 0 && pos < 6) {
                    hideGridView();
                    if (dockRecycler != null && dockRecycler.getChildCount() > 0) {
                        dockRecycler.getChildAt(0).requestFocus();
                    }
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void showGridView() {
        isGridViewActive = true;
        if (gridContainer != null) {
            gridContainer.setVisibility(View.VISIBLE);
            gridContainer.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
        }
        if (dockContainerLayout != null) {
            dockContainerLayout.animate().alpha(0f).translationY(50f).setDuration(180).withEndAction(() -> dockContainerLayout.setVisibility(View.GONE)).start();
        }
    }

    private void hideGridView() {
        isGridViewActive = false;
        if (dockContainerLayout != null) {
            dockContainerLayout.setVisibility(View.VISIBLE);
            dockContainerLayout.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
        }
        if (gridContainer != null) {
            gridContainer.animate().alpha(0f).translationY(50f).setDuration(180).withEndAction(() -> gridContainer.setVisibility(View.GONE)).start();
        }
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
                if (count > 0) {
                    return Color.rgb((int)(r / count), (int)(g / count), (int)(b / count));
                }
            }
        } catch (Exception ignored) {}
        return 0xFF2C2C2E;
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> availableActivities = pm.queryIntentActivities(intent, 0);

        List<AppItem> allApps = new ArrayList<>();
        for (ResolveInfo ri : availableActivities) {
            if (!ri.activityInfo.packageName.equals(getPackageName())) {
                String pkg = ri.activityInfo.packageName;
                String label = ri.loadLabel(pm).toString();
                Drawable icon = ri.loadIcon(pm);
                int domColor = extractDominantColor(icon);
                allApps.add(new AppItem(pkg, label, icon, domColor));
            }
        }

        Collections.sort(allApps, (a, b) -> a.label.compareToIgnoreCase(b.label));

        dockApps.clear();
        gridApps.clear();

        int limit = Math.min(6, allApps.size());
        for (int i = 0; i < limit; i++) {
            dockApps.add(allApps.get(i));
        }
        for (int i = limit; i < allApps.size(); i++) {
            gridApps.add(allApps.get(i));
        }

        if (dockRecycler != null && dockRecycler.getAdapter() != null) {
            dockRecycler.getAdapter().notifyDataSetChanged();
        }
        if (gridRecycler != null && gridRecycler.getAdapter() != null) {
            gridRecycler.getAdapter().notifyDataSetChanged();
        }
    }

    private void animateAmbientGlow(int targetColor) {
        if (ambientGlowBg == null) return;
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), currentGlowColor, targetColor);
        colorAnim.setDuration(280);
        colorAnim.addUpdateListener(animator -> {
            int c = (int) animator.getAnimatedValue();
            currentGlowColor = c;
            GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{c, 0xFF050508}
            );
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
            
            int width = dpToPx(138);
            int height = dpToPx(82);
            
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(width, height);
            lp.setMargins(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
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
            card.addView(iv, new FrameLayout.LayoutParams(dpToPx(50), dpToPx(50), Gravity.CENTER));

            return new RecyclerView.ViewHolder(card) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AppItem item = items.get(position);
            FrameLayout card = (FrameLayout) holder.itemView;
            ImageView iv = (ImageView) card.getChildAt(0);

            iv.setImageDrawable(item.icon);

            int baseColor = item.dominantColor;
            int darkColor = Color.rgb(
                (int)(Color.red(baseColor) * 0.5f),
                (int)(Color.green(baseColor) * 0.5f),
                (int)(Color.blue(baseColor) * 0.5f)
            );

            GradientDrawable cardBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{baseColor, darkColor});
            cardBg.setCornerRadius(dpToPx(18));
            card.setBackground(cardBg);

            card.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    animateScale(card, 1.15f, 1.15f, dpToPx(28));
                    animateAmbientGlow(baseColor);
                    if (heroTitle != null) {
                        heroTitle.setText(item.label);
                    }
                } else {
                    animateScale(card, 1.0f, 1.0f, 0f);
                }
            });

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
