package com.xiaomi.oga.utils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Display;

import com.xiaomi.oga.start.OgaAppContext;

/**
 * Created by cox
 * on 5/31/17.
 */

public class ResourceUtils {
    public static String getString(int id) {
        return OgaAppContext.getAppContext().getString(id);
    }
    public static String getString(int id, Object ... args) {
        return OgaAppContext.getAppContext().getString(id, args);
    }
    public static String [] getStringArray(int id) {
        return OgaAppContext.getAppContext().getResources().getStringArray(id);
    }
    public static Drawable getDrawable(int id) {
        return OgaAppContext.getAppContext().getResources().getDrawable(id);
    }
    public static int getColor(int id) {
        return OgaAppContext.getAppContext().getResources().getColor(id);
    }
    public static float getDimen(int id) {
        return OgaAppContext.getAppContext().getResources().getDimension(id);
    }
    public static int getDimensionPixelSize(int id) {
        return OgaAppContext.getAppContext().getResources().getDimensionPixelSize(id);
    }
    public static int getDimensionPixelOffset(int id) {
        return OgaAppContext.getAppContext().getResources().getDimensionPixelOffset(id);
    }

    public static DisplayMetrics getDisplayMetrics(){
        return OgaAppContext.getAppContext().getResources().getDisplayMetrics();
    }

    public static AssetManager getAssets(){
        return OgaAppContext.getAppContext().getResources().getAssets();
    }

    public static Bitmap getDecodedBitmap(int id) {
        return BitmapFactory.decodeResource(OgaAppContext.getAppContext().getResources(), id);
    }
}
