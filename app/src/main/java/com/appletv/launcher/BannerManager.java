package com.appletv.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BannerManager {
    private static BannerManager instance;
    private LruCache<String, Bitmap> memoryCache;
    private ExecutorService executor;
    private Handler mainHandler;
    private boolean isFirstVisible = true;
    private String currentPackage = "";

    private BannerManager() {
        // Выделяем 1/6 доступной памяти под кэш баннеров, чтобы лаунчер не тормозил
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        memoryCache = new LruCache<String, Bitmap>(maxMemory / 6) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static BannerManager getInstance() {
        if (instance == null) instance = new BannerManager();
        return instance;
    }

    public void updateBackground(Activity activity, String packageName) {
        if (packageName.equals(currentPackage)) return;
        currentPackage = packageName;

        SharedPreferences prefs = activity.getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE);
        int bgMode = prefs.getInt("bg_mode", 0); // 0 = Подсветка, 1 = Баннеры
        
        int containerId = activity.getResources().getIdentifier("banner_container", "id", activity.getPackageName());
        View bannerContainer = activity.findViewById(containerId);
        
        if (bannerContainer == null) return;

        // Если выбрана старая подсветка — прячем слой баннеров
        if (bgMode == 0) {
            bannerContainer.setVisibility(View.GONE);
            return;
        }
        
        bannerContainer.setVisibility(View.VISIBLE);
        
        int bg1Id = activity.getResources().getIdentifier("banner_bg_1", "id", activity.getPackageName());
        int bg2Id = activity.getResources().getIdentifier("banner_bg_2", "id", activity.getPackageName());
        ImageView bg1 = activity.findViewById(bg1Id);
        ImageView bg2 = activity.findViewById(bg2Id);
        
        if (bg1 == null || bg2 == null) return;

        Bitmap cached = memoryCache.get(packageName);
        if (cached != null) {
            applyCrossfade(bg1, bg2, cached);
            return;
        }

        executor.execute(() -> {
            Bitmap result = extractBanner(activity, packageName);
            if (result != null) {
                memoryCache.put(packageName, result);
                mainHandler.post(() -> {
                    if (packageName.equals(currentPackage)) {
                        applyCrossfade(bg1, bg2, result);
                    }
                });
            }
        });
    }

    private void applyCrossfade(ImageView bg1, ImageView bg2, Bitmap bitmap) {
        ImageView fadeOut = isFirstVisible ? bg1 : bg2;
        ImageView fadeIn = isFirstVisible ? bg2 : bg1;

        fadeIn.setImageBitmap(bitmap);
        fadeIn.animate().alpha(1f).setDuration(450).start();
        fadeOut.animate().alpha(0f).setDuration(450).start();
        
        isFirstVisible = !isFirstVisible;
    }

    private Bitmap extractBanner(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            // 1. Пробуем получить родной ТВ-баннер из приложения
            Drawable banner = pm.getApplicationBanner(packageName);
            if (banner != null) return drawableToBitmap(banner, false);
            
            // 2. Если баннера нет, берем иконку и блюрим её аппаратно
            Drawable icon = pm.getApplicationIcon(packageName);
            if (icon != null) return blurBitmap(context, drawableToBitmap(icon, true));
        } catch (Exception e) {}
        return null;
    }

    private Bitmap drawableToBitmap(Drawable drawable, boolean scaleDown) {
        int width = scaleDown ? 250 : Math.max(drawable.getIntrinsicWidth(), 1);
        int height = scaleDown ? 250 : Math.max(drawable.getIntrinsicHeight(), 1);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    private Bitmap blurBitmap(Context context, Bitmap image) {
        try {
            Bitmap outputBitmap = Bitmap.createBitmap(image);
            RenderScript rs = RenderScript.create(context);
            ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation tmpIn = Allocation.createFromBitmap(rs, image);
            Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
            intrinsicBlur.setRadius(25f); // Максимальное размытие (радиус 25)
            intrinsicBlur.setInput(tmpIn);
            intrinsicBlur.forEach(tmpOut);
            tmpOut.copyTo(outputBitmap);
            rs.destroy();
            return outputBitmap;
        } catch (Exception e) {
            return image;
        }
    }
}
