package com.appletv.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Deque;

public class SettingsActivity extends Activity {

    private ImageView dynamicLeftIcon;
    private LinearLayout listContainer;
    private TextView titleView;
    private SharedPreferences prefs;

    private enum MenuState { MAIN, GENERAL, APPEARANCE, HOME_SCREEN, SYSTEM, ABOUT, SLEEP_OPTIONS, LANGUAGE }
    private MenuState currentState = MenuState.MAIN;
    private final Deque<MenuState> backStack = new ArrayDeque<>();
    private int currentLeftIconId = -1;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SoundManager.init(this);
        prefs = getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);
        dynamicLeftIcon = findViewById(R.id.dynamic_left_icon);
        listContainer = findViewById(R.id.settings_list_container);
        titleView = findViewById(R.id.settings_title);
        loadMenu(MenuState.MAIN, false);
    }

    private void loadMenu(MenuState state, boolean push) {
        if (push && currentState != state) backStack.push(currentState);
        currentState = state;
        switch (state) {
            case MAIN: loadMainMenu(); break;
            case GENERAL: loadGeneralMenu(); break;
            case APPEARANCE: loadAppearanceMenu(); break;
            case HOME_SCREEN: loadHomeScreenMenu(); break;
            case SYSTEM: loadSystemMenu(); break;
            case ABOUT: loadAboutMenu(); break;
            case SLEEP_OPTIONS: loadSleepOptionsMenu(); break;
            case LANGUAGE: loadLanguageMenu(); break;
        }
    }

    private void updateLeftIcon(int resId) {
        if (currentLeftIconId == resId || dynamicLeftIcon == null) return;
        currentLeftIconId = resId;
        dynamicLeftIcon.animate().alpha(0f).setDuration(120).withEndAction(() -> {
            dynamicLeftIcon.setImageResource(resId);
            dynamicLeftIcon.animate().alpha(1f).setDuration(120).start();
        }).start();
    }

    private void loadMainMenu() {
        titleView.setText(R.string.settings_title);
        listContainer.removeAllViews();
        currentLeftIconId = -1;
        addItem(getString(R.string.settings_general), "", R.drawable.ic_panel_info, () -> loadMenu(MenuState.GENERAL, true), true);
        addItem(getString(R.string.settings_appearance), "", R.drawable.ic_panel_appearance, () -> loadMenu(MenuState.APPEARANCE, true), true);
        addItem(getString(R.string.settings_home_screen), "", R.drawable.ic_panel_home, () -> loadMenu(MenuState.HOME_SCREEN, true), true);
        addItem(getString(R.string.settings_system), "", R.drawable.ic_panel_system, () -> loadMenu(MenuState.SYSTEM, true), true);
        addItem(getString(R.string.settings_about), "", R.drawable.ic_panel_info, () -> loadMenu(MenuState.ABOUT, true), true);
        requestFirstFocus();
    }

    private void loadGeneralMenu() {
        titleView.setText(R.string.settings_general);
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_info);

        int timeFormat = prefs.getInt("time_format", 0);
        String[] timeOptions = {getString(R.string.settings_time_24), getString(R.string.settings_time_12)};
        addItem(getString(R.string.settings_time_format), timeOptions[timeFormat], R.drawable.ic_panel_info, () -> {
            int next = (timeFormat + 1) % timeOptions.length;
            prefs.edit().putInt("time_format", next).apply();
            SettingsManager.applyTimeFormat(this);
            loadGeneralMenu();
            showToast(getString(R.string.toast_time_changed));
        }, true);

        int sleepTime = prefs.getInt("sleep_timeout", 0);
        String[] sleepLabels = {getString(R.string.settings_sleep_off), getString(R.string.settings_sleep_5), getString(R.string.settings_sleep_15), getString(R.string.settings_sleep_30)};
        addItem(getString(R.string.settings_sleep), sleepLabels[sleepTime], R.drawable.ic_panel_info, () -> loadMenu(MenuState.SLEEP_OPTIONS, true), true);

        addItem(getString(R.string.settings_default_launcher), "", R.drawable.ic_panel_info, () -> {
            try {
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.addCategory(Intent.CATEGORY_HOME);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(home);
                showToast(getString(R.string.toast_choose_launcher));
                return;
            } catch (Exception ignored) {}
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
                    showToast(getString(R.string.toast_open_default_apps));
                    return;
                } catch (Exception ignored) {}
            }
            try { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); }
            catch (Exception e) { showToast(getString(R.string.toast_cant_open)); }
        }, true);
        requestFirstFocus();
    }

    private void loadSleepOptionsMenu() {
        titleView.setText(R.string.settings_sleep_title);
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_info);
        int currentSleep = prefs.getInt("sleep_timeout", 0);
        String[] sleepLabels = {getString(R.string.settings_sleep_off), getString(R.string.settings_sleep_5), getString(R.string.settings_sleep_15), getString(R.string.settings_sleep_30)};
        for (int i = 0; i < sleepLabels.length; i++) {
            final int idx = i;
            addItem(sleepLabels[i], currentSleep == idx ? "✓" : "", R.drawable.ic_panel_info, () -> {
                prefs.edit().putInt("sleep_timeout", idx).apply();
                SettingsManager.startSleepTimer(this);
                loadSleepOptionsMenu();
                showToast(getString(R.string.toast_sleep_changed));
            }, false);
        }
        requestFirstFocus();
    }

    private void loadAppearanceMenu() {
        titleView.setText(R.string.settings_appearance);
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_appearance);
        int bgMode = prefs.getInt("bg_mode", 0);
        String[] bgLabels = {getString(R.string.settings_bg_ambient), getString(R.string.settings_bg_banners)};
        addItem(getString(R.string.settings_bg_mode), bgLabels[bgMode], R.drawable.ic_panel_appearance, () -> {
            int next = (bgMode + 1) % bgLabels.length;
            prefs.edit().putInt("bg_mode", next).apply();
            SettingsManager.applyBackgroundMode(this);
            loadAppearanceMenu();
            showToast(getString(R.string.toast_bg_changed, bgLabels[next]));
        }, true);
        requestFirstFocus();
    }

    private void loadHomeScreenMenu() {
        titleView.setText(R.string.settings_home_screen);
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_home);
        boolean showClock = prefs.getBoolean("show_clock", true);
        addItem(getString(R.string.settings_show_clock), showClock ? getString(R.string.settings_on) : getString(R.string.settings_off), R.drawable.ic_panel_home, () -> {
            boolean next = !prefs.getBoolean("show_clock", true);
            prefs.edit().putBoolean("show_clock", next).apply();
            SettingsManager.applyClockVisibility(this);
            loadHomeScreenMenu();
            showToast(next ? getString(R.string.toast_clock_on) : getString(R.string.toast_clock_off));
        }, true);
        addItem(getString(R.string.settings_reset_favorites), "", R.drawable.ic_panel_home, () -> {
            prefs.edit().putString("favorites", MainActivity.VIRTUAL_SETTINGS_PKG).putBoolean("settings_injected_v2", true).apply();
            showToast(getString(R.string.toast_favorites_reset));
        }, true);
        requestFirstFocus();
    }

    private void loadSystemMenu() {
        titleView.setText(R.string.settings_system);
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_system);
        addItem(getString(R.string.settings_android_tv), "", R.drawable.ic_panel_system, () -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); }
            catch (Exception e) { showToast(getString(R.string.toast_cant_open)); }
        }, true);
        addItem(getString(R.string.settings_language), getCurrentLangLabel(), R.drawable.ic_panel_system, () -> loadMenu(MenuState.LANGUAGE, true), true);
        addItem(getString(R.string.settings_reset_all), "", R.drawable.ic_panel_system, () -> {
            prefs.edit().clear().apply();
            showToast(getString(R.string.toast_settings_reset));
            loadMainMenu();
        }, true);
        requestFirstFocus();
    }

    private void loadAboutMenu() {
        titleView.setText(R.string.settings_about);
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_info);
        addItem(getString(R.string.settings_version, "0.9-beta"), "", R.drawable.ic_panel_info, null, false);
        addItem(getString(R.string.settings_build), "", R.drawable.ic_panel_info, null, false);
        addItem(getString(R.string.settings_developer), "", R.drawable.ic_panel_info, null, false);
        addItem(getString(R.string.settings_check_updates), "", R.drawable.ic_panel_info, () -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(android.net.Uri.parse("https://github.com/z7413130-a11y/tvos-launcher2"));
                startActivity(i);
            } catch (Exception e) { showToast(getString(R.string.toast_no_browser)); }
        }, true);
        requestFirstFocus();
    }

    private String getCurrentLangLabel() {
        return "en".equals(LocaleHelper.getLanguage(this)) ? getString(R.string.lang_en) : getString(R.string.lang_ru);
    }

    private void loadLanguageMenu() {
        titleView.setText(R.string.lang_title);
        listContainer.removeAllViews();
        updateLeftIcon(R.drawable.ic_panel_system);
        String current = LocaleHelper.getLanguage(this);
        addItem(getString(R.string.lang_ru), "ru".equals(current) ? "✓" : "", R.drawable.ic_panel_system, () -> { LocaleHelper.setLanguage(this, "ru"); recreate(); }, false);
        addItem(getString(R.string.lang_en), "en".equals(current) ? "✓" : "", R.drawable.ic_panel_system, () -> { LocaleHelper.setLanguage(this, "en"); recreate(); }, false);
        requestFirstFocus();
    }

    private void requestFirstFocus() {
        listContainer.post(() -> { if (listContainer.getChildCount() > 0) listContainer.getChildAt(0).requestFocus(); });
    }

    private void showToast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    private void addItem(String text, String value, int iconResId, Runnable onClick, boolean hasArrow) {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48));
        rowParams.setMargins(0, 0, 0, dpToPx(6));
        row.setLayoutParams(rowParams);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (onClick == null) { row.setFocusable(false); row.setClickable(false); }
        else { row.setFocusable(true); row.setClickable(true); row.setOnClickListener(v -> onClick.run()); }
        row.setPadding(dpToPx(16), 0, dpToPx(16), 0);
        row.setBackgroundResource(R.drawable.bg_settings_row_normal);

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16f);
        label.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        row.addView(label, textParams);

        if (value != null && !value.isEmpty()) {
            TextView valView = new TextView(this);
            valView.setText(value);
            valView.setTextColor(Color.parseColor("#999999"));
            valView.setTextSize(14f);
            valView.setPadding(0, 0, dpToPx(12), 0);
            row.addView(valView);
        }

        TextView arrow = new TextView(this);
        arrow.setText(">");
        arrow.setTextColor(Color.parseColor("#777777"));
        arrow.setTextSize(16f);
        if (!hasArrow) arrow.setVisibility(View.GONE);
        row.addView(arrow);

        row.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                SoundManager.playFocus();
                row.setBackgroundResource(R.drawable.bg_settings_row_focused);
                label.setTextColor(Color.BLACK);
                arrow.setTextColor(Color.BLACK);
                row.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start();
                if (currentState == MenuState.MAIN) updateLeftIcon(iconResId);
            } else {
                row.setBackgroundResource(R.drawable.bg_settings_row_normal);
                label.setTextColor(Color.WHITE);
                arrow.setTextColor(Color.parseColor("#777777"));
                row.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            }
        });
        listContainer.addView(row);
    }

    @Override
    public void onBackPressed() {
        if (backStack.isEmpty()) super.onBackPressed();
        else loadMenu(backStack.pop(), false);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        SettingsManager.startSleepTimer(this);
    }

    private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density + 0.5f); }
}
