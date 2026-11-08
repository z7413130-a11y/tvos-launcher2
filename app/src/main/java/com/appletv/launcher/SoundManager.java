package com.appletv.launcher;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;

public class SoundManager {
    private static SoundPool soundPool;
    private static int clickSoundId = -1;
    private static boolean initialized = false;

    public static void init(Context context) {
        if (initialized) return;
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                soundPool = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build();
            } else {
                soundPool = new SoundPool(3, android.media.AudioManager.STREAM_SYSTEM, 0);
            }
            clickSoundId = soundPool.load(context, android.R.raw.blank, 1);
        } catch (Exception ignored) {
            clickSoundId = -1;
        }
        initialized = true;
    }

    public static void playFocus() {
        if (soundPool == null || clickSoundId < 0) return;
        try {
            soundPool.play(clickSoundId, 0.05f, 0.05f, 1, 0, 1f);
        } catch (Exception ignored) {}
    }
}
