package org.gtp.cocacolaproject.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BitmapUtils {

    private final static String TAG = BitmapUtils.class.toString();

    /**
     * Ensure this class is only used as a utility.
     */
    private BitmapUtils() {
        throw new AssertionError();
    }

    public static int fromDpToPixel(Context context, Number value) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (value.doubleValue() * metrics.density);
    }

    public static int fromPixelToDp(Context context, Number value) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (value.doubleValue() / metrics.density);
    }
    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
