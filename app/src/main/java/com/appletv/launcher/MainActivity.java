package com.appletv.launcher;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
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
import java.util.List;

public class MainActivity extends Activity {

    private TextView heroTitle;
    private RecyclerView dockRecycler;
    private RecyclerView gridRecycler;
    private FrameLayout btnSettings;
    
    private final List<AppItem> dockApps = new ArrayList<>();
    private final List<AppItem> gridApps = new ArrayList<>();
    private SharedPreferences prefs;

    public static class AppItem {
        public String packageName;
        public String label;
        public Drawable icon;
        public int[] colors;

        public AppItem(String packageName, String label, Drawable icon, int[] colors) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.colors = colors;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);

        heroTitle = findViewById(R.id.hero_title);
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

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
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
                int[] colors = new int[]{0xFF2C2C2E, 0xFF1C1C1E};
                allApps.add(new AppItem(pkg, label, icon, colors));
            }
        }

        Collections.sort(allApps, (a, b) -> a.label.compareToIgnoreCase(b.label));

        dockApps.clear();
        gridApps.clear();

        // Ровно 6 иконок в верхнем доке, как на референсе Apple TV
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
            LinearLayout container = new LinearLayout(parent.getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER_HORIZONTAL);
            
            int width = dpToPx(132);
            int height = dpToPx(76);
            
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(width, height + dpToPx(28));
            lp.setMargins(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
            container.setLayoutParams(lp);
            container.setFocusable(true);
            container.setClickable(true);
            container.setClipChildren(false);

            FrameLayout card = new FrameLayout(parent.getContext());
            ViewGroup.LayoutParams cardLp = new ViewGroup.LayoutParams(width, height);
            card.setLayoutParams(cardLp);
            
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(16));
                }
            });
            card.setClipToOutline(true);

            ImageView iv = new ImageView(parent.getContext());
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(iv, new FrameLayout.LayoutParams(dpToPx(44), dpToPx(44), Gravity.CENTER));
            container.addView(card);

            TextView tv = new TextView(parent.getContext());
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(12);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            tv.setSingleLine(true);
            tv.setAlpha(0f);
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tvLp.setMargins(0, dpToPx(6), 0, 0);
            container.addView(tv, tvLp);

            return new RecyclerView.ViewHolder(container) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AppItem item = items.get(position);
            LinearLayout container = (LinearLayout) holder.itemView;
            FrameLayout card = (FrameLayout) container.getChildAt(0);
            ImageView iv = (ImageView) card.getChildAt(0);
            TextView tv = (TextView) container.getChildAt(1);

            iv.setImageDrawable(item.icon);
            tv.setText(item.label);

            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, item.colors);
            bg.setCornerRadius(dpToPx(16));
            card.setBackground(bg);

            container.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    animateScale(container, 1.15f, 1.15f, dpToPx(20));
                    tv.animate().alpha(1f).setDuration(150).start();
                    if (heroTitle != null) heroTitle.setText(item.label);
                } else {
                    animateScale(container, 1.0f, 1.0f, 0f);
                    tv.animate().alpha(0f).setDuration(120).start();
                }
            });

            container.setOnClickListener(v -> {
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
        set.setDuration(160);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }
}
