package com.appletv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {

    public static final String PREFS = "tvos_prefs";
    public static final String KEY_LANG = "app_language"; // "ru" или "en"

    public static Context onAttach(Context context) {
        return setLocale(context, getLanguage(context));
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, "ru");
    }

    public static void setLanguage(Context context, String lang) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();
    }

    public static Context setLocale(Context context, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    public static String tr(Context context, String ru, String en) {
        return "en".equals(getLanguage(context)) ? en : ru;
    }
}
