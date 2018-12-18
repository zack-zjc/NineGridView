package com.realcloud.view.video.basecomponent;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.util.TypedValue;
import android.view.WindowManager;

import java.util.Formatter;
import java.util.Locale;

/**
 * 工具类.
 */
public class NineGridUtil {

    /**
     * Get activity from context object
     * @param context something
     * @return object of Activity or null if it is not Activity
     */
    static Activity scanForActivity(Context context) {
        if (context == null) return null;
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return scanForActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

    /**
     * 获取屏幕宽度
     *
     * @param context
     * @return width of the screen.
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取屏幕高度
     *
     * @param context
     * @return heiht of the screen.
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * dp转px
     *
     * @param context
     * @param dpVal   dp value
     * @return px value
     */
    static int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal,
                context.getResources().getDisplayMetrics());
    }

    /**
     * 将毫秒数格式化为"##:##"的时间
     *
     * @param milliseconds 毫秒数
     * @return ##:##
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds <= 0 || milliseconds >= 24 * 60 * 60 * 1000) {
            return "00:00";
        }
        long totalSeconds = milliseconds / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        StringBuilder stringBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    /**
     * 保存播放位置，以便下次播放时接着上次的位置继续播放.
     *
     * @param context
     * @param url     视频链接url
     */
    static void savePlayPosition(Context context, String url, long position) {
        context.getSharedPreferences("NINE_GRID__PALYER_PLAY_POSITION",
                Context.MODE_PRIVATE)
                .edit()
                .putLong(url, position)
                .apply();
    }

    /**
     * 取出上次保存的播放位置
     *
     * @param context
     * @param url     视频链接url
     * @return 上次保存的播放位置
     */
    static long getSavedPlayPosition(Context context, String url) {
        return context.getSharedPreferences("NINE_GRID__PALYER_PLAY_POSITION",
                Context.MODE_PRIVATE)
                .getLong(url, 0);
    }

    /**
     * 设置播放是否静音
     */
    static void savePlayVolumeEnable(Context context, boolean enable) {
        context.getSharedPreferences("NINE_GRID_VIDEO_PALYER_PLAY_POSITION",
                Context.MODE_PRIVATE)
                .edit()
                .putBoolean("NINE_GRID_VIDEO_VOLUME_ENABLE", enable)
                .apply();
    }

    /**
     * 取出播放是否静音
     */
    static boolean getSavedVolumeEnable(Context context) {
        return context.getSharedPreferences("NINE_GRID__PALYER_PLAY_POSITION",
                Context.MODE_PRIVATE)
                .getBoolean("NINE_GRID_VIDEO_VOLUME_ENABLE", false);
    }
}
