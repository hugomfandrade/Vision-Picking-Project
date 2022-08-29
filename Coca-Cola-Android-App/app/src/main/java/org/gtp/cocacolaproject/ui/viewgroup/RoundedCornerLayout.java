package org.gtp.cocacolaproject.ui.viewgroup;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import org.gtp.cocacolaproject.R;

@SuppressWarnings("unused")
public class RoundedCornerLayout extends LinearLayout {

    private static final String TAG = RoundedCornerLayout.class.getSimpleName();

    private float[] cornerRadius = new float[8];
    private float[] elevation = new float[4]; // Start, Top, End, Bottom
    private float percentileHeight, percentileWidth;
    private int borderColor;
    private int backgroundColor;

    private android.graphics.RectF rect2 = new RectF();
    private Path path = new Path();
    private boolean mIntercept = false;

    public RoundedCornerLayout(Context context) {
        super(context);
    }

    public RoundedCornerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RoundedCornerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundedCornerLayout, 0, 0);

        float cornerRadiusGlobal =
                a.getDimension(R.styleable.RoundedCornerLayout_corner_radius,0f);
        float cornerRadiusTopStart =
                a.getDimension(R.styleable.RoundedCornerLayout_corner_radius_top_start, cornerRadiusGlobal);
        float cornerRadiusTopEnd =
                a.getDimension(R.styleable.RoundedCornerLayout_corner_radius_top_end, cornerRadiusGlobal);
        float cornerRadiusBottomStart =
                a.getDimension(R.styleable.RoundedCornerLayout_corner_radius_bottom_start, cornerRadiusGlobal);
        float cornerRadiusBottomEnd =
                a.getDimension(R.styleable.RoundedCornerLayout_corner_radius_bottom_end, cornerRadiusGlobal);
        float elevationGlobal =
                a.getDimension(R.styleable.RoundedCornerLayout_border_elevation, 0f);
        elevation[0] =
                a.getDimension(R.styleable.RoundedCornerLayout_border_elevation_start, elevationGlobal);
        elevation[1] =
                a.getDimension(R.styleable.RoundedCornerLayout_border_elevation_top, elevationGlobal);
        elevation[2] =
                a.getDimension(R.styleable.RoundedCornerLayout_border_elevation_end, elevationGlobal);
        elevation[3] =
                a.getDimension(R.styleable.RoundedCornerLayout_border_elevation_bottom, elevationGlobal);
        borderColor =
                a.getColor(R.styleable.RoundedCornerLayout_border_color, Color.TRANSPARENT);
        backgroundColor =
                a.getColor(R.styleable.RoundedCornerLayout_background_color, Color.TRANSPARENT);
        percentileHeight =
                a.getFloat(R.styleable.RoundedCornerLayout_percentile_height, Float.NaN);
        percentileWidth =
                a.getFloat(R.styleable.RoundedCornerLayout_percentile_width, Float.NaN);
        boolean isClickable =
                attrs.getAttributeBooleanValue("http://schemas.android.com/apk/res/android", "clickable", false);
        a.recycle();

        if (!Float.isNaN(percentileWidth) && !Float.isNaN(percentileHeight)) {
            percentileHeight = Float.NaN;
            percentileWidth = Float.NaN;
        }
        else {
            if (!Float.isNaN(percentileWidth)) {
                if (percentileWidth < 0)
                    percentileWidth = 0;
                //else if (percentileWidth > 100) percentileWidth = 100;
            }
            if (!Float.isNaN(percentileHeight)) {
                if (percentileHeight < 0)
                    percentileHeight = 0;
                //else if (percentileHeight > 100) percentileHeight = 100;
            }
        }

        cornerRadius[0] = cornerRadiusTopStart;
        cornerRadius[1] = cornerRadiusTopStart;
        cornerRadius[2] = cornerRadiusTopEnd;
        cornerRadius[3] = cornerRadiusTopEnd;
        cornerRadius[4] = cornerRadiusBottomEnd;
        cornerRadius[5] = cornerRadiusBottomEnd;
        cornerRadius[6] = cornerRadiusBottomStart;
        cornerRadius[7] = cornerRadiusBottomStart;

        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        setClickable(isClickable);
        setBackgroundDrawable(
                makeSelectorBackgroundDrawable(backgroundColor, borderColor, cornerRadius, elevation));

        setWillNotDraw(false);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        try {
            super.setPadding(
                    (int) (left + (elevation[0] < 0 ? 0 : elevation[0])),
                    (int) (top + (elevation[1] < 0 ? 0 : elevation[1])),
                    (int) (right + (elevation[2] < 0 ? 0 : elevation[2])),
                    (int) (bottom + (elevation[3] < 0 ? 0 : elevation[3])));
        } catch (NullPointerException e) {
            Log.e(TAG, "e = " + e.getMessage());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (w != oldw || h != oldh)
            requestLayout();

        // compute the path
        path.reset();
        rect2.set(
                0 + (elevation[0] < 0 ? 0 : elevation[0]),
                0 + (elevation[1] < 0 ? 0 : elevation[1]),
                w - (elevation[2] < 0 ? 0 : elevation[2]),
                h - (elevation[3] < 0 ? 0 : elevation[3]));
        path.addRoundRect(rect2, cornerRadius, Path.Direction.CW);
        path.close();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (!Float.isNaN(percentileHeight)) {
            super.onMeasure(
                    widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(
                            (int) (width * percentileHeight / 100f),
                            MeasureSpec.EXACTLY
                    ));
        } else if (!Float.isNaN(percentileWidth)) {
            //if (getHeight() == 0 && heightMode == MeasureSpec.AT_MOST) super.onMeasure(widthMeasureSpec, heightMeasureSpec);else
            super.onMeasure(

                    //((heightMode == MeasureSpec.AT_MOST && getHeight() == 0)? getHeight() :
                    MeasureSpec.makeMeasureSpec(
                            (int) (height * percentileWidth / 100f)
                            , MeasureSpec.EXACTLY)
                    //)
                    ,
                    heightMeasureSpec);
        }
        else
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);/**/
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isHardwareAccelerated() && Build.VERSION.SDK_INT < 17) {
            super.dispatchDraw(canvas);
        }
        else {
            int save = canvas.save();
            canvas.clipPath(path);
            super.dispatchDraw(canvas);
            canvas.restoreToCount(save);
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        if (backgroundColor != color) {
            backgroundColor = color;
            setBackgroundDrawable(makeSelectorBackgroundDrawable(
                    backgroundColor, borderColor, cornerRadius, elevation));
        }
    }

    public void setBorderColor(int color) {
        if (backgroundColor != color) {
            borderColor = color;
            setBackgroundDrawable(makeSelectorBackgroundDrawable(
                    backgroundColor, borderColor, cornerRadius, elevation));
        }
    }

    public void disableHeightPercentile() {
        percentileHeight = Float.NaN;
    }

    public void disableWidthPercentile() {
        percentileWidth = Float.NaN;
    }

    public void setCornerRadius(int size) {
        cornerRadius[0] = size;
        cornerRadius[1] = size;
        cornerRadius[2] = size;
        cornerRadius[3] = size;
        cornerRadius[4] = size;
        cornerRadius[5] = size;
        cornerRadius[6] = size;
        cornerRadius[7] = size;
        invalidate();
    }

    public void setInterceptTouchEvent(boolean intercept) {
        mIntercept = intercept;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mIntercept || super.onInterceptTouchEvent(ev);
    }

    private static Drawable makeSelectorBackgroundDrawable(int backgroundColor,
                                                           int borderColor,
                                                           float[] cornerRadius,
                                                           float[] elevation) {
        StateListDrawable res = new StateListDrawable();
        res.addState(new int[]{android.R.attr.state_pressed},
                makeBackgroundDrawable(backgroundColor, borderColor, cornerRadius, elevation));
        res.addState(new int[]{android.R.attr.state_selected},
                makeBackgroundDrawable(backgroundColor, borderColor, cornerRadius, elevation));
        res.addState(new int[]{},
                makeBackgroundDrawable(backgroundColor, borderColor, cornerRadius, elevation));
        return res;
    }

    private static Drawable makeBackgroundDrawable(int backgroundColor,
                                                   int borderColor,
                                                   float[] cornerRadius,
                                                   float[] elevation) {

        //ShapeDrawable border = new ShapeDrawable(new RoundRectShape(cornerRadius, null, null));
        //border.getPaint().setColor(borderColor);
        //border.getPaint().setStrokeWidth(max(elevation));

        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.TRANSPARENT); //white background
        border.setStroke((int) max(elevation) + 1, borderColor); //black border with full opacity
        border.setCornerRadii(cornerRadius); //black border with full opacity

        ShapeDrawable background = new ShapeDrawable(new RoundRectShape(cornerRadius, null, null));
        background.getPaint().setColor(backgroundColor);

        Drawable[] drawableArray = {border, background};
        LayerDrawable l = new LayerDrawable(drawableArray);
        l.setLayerInset(0,
                (int) (elevation[0] < 0? -elevation[0] : 0),
                (int) (elevation[1] < 0? -elevation[1] : 0),
                (int) (elevation[2] < 0? -elevation[2] : 0),
                (int) (elevation[3] < 0? -elevation[3] : 0));
        l.setLayerInset(1,
                (int) (elevation[0] < 0? 0 : elevation[0]),
                (int) (elevation[1] < 0? 0 : elevation[1]),
                (int) (elevation[2] < 0? 0 : elevation[2]),
                (int) (elevation[3] < 0? 0 : elevation[3]));

        return l;
    }

    private static float max(float[] elevation) {
        float max = Float.NaN;
        for (float f : elevation)
            if (Float.isNaN(max) || f > max)
                max = f;
        return max;
    }
}
