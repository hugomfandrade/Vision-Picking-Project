package org.gtp.cocacolaproject.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;

public class SelectedRectView extends View {

    private Paint mPaint;
    private Paint mProgressPaint;
    private float height = 0f;
    private float width = 0f;

    private Rect rect = new Rect();
    private Path pathProgress = new Path();
    private float mProgress = 0f;

    public SelectedRectView(Context context) {
        super(context);
        init();
    }

    public SelectedRectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SelectedRectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        mPaint = new Paint();
        mPaint.setStrokeWidth(0);
        mPaint.setColor(Color.TRANSPARENT);
        mPaint.setStyle(Paint.Style.STROKE);

        mProgressPaint = new Paint();
        mProgressPaint.setStrokeWidth(0);
        mProgressPaint.setColor(Color.TRANSPARENT);
        mProgressPaint.setStyle(Paint.Style.STROKE);

    }

    public void setHeight(float height) {
        if (this.height != height) {
            this.height = height;
            invalidate();
        }
    }

    public void setWidth(float width) {
        if (this.width != width) {
            this.width = width;
            invalidate();
        }
    }

    public void setStrokeWidth(float strokeWidth) {

        if (mPaint.getStrokeWidth() != strokeWidth) {
            mPaint.setStrokeWidth(strokeWidth);
            invalidate();
        }
    }

    public void setStrokeColor(int strokeColor) {

        if (mPaint.getColor() != strokeColor) {
            mPaint.setColor(strokeColor);
            invalidate();
        }
    }

    public void setProgressStrokeWidth(float strokeWidth) {

        if (mProgressPaint.getStrokeWidth() != strokeWidth) {
            mProgressPaint.setStrokeWidth(strokeWidth);
            invalidate();
        }
    }

    public void setProgressStrokeColor(int strokeColor) {

        if (mProgressPaint.getColor() != strokeColor) {
            mProgressPaint.setColor(strokeColor);
            invalidate();
        }
    }

    public void setProgress(float progress) {

        if (mProgress != progress) {
            mProgress = progress;
            invalidate();
        }
    }

    @Override
    public void setAnimation(Animation animation) {
        if (animation instanceof SelectedRectViewAnimation) {
            ((SelectedRectViewAnimation) animation).setSelectedRectView(this);
        }
        super.setAnimation(animation);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(getRect(), mPaint);
        canvas.drawPath(getProgressPath(), mProgressPaint);
    }

    private Rect getRect() {

        float nleftX = getMeasuredWidth() / 2 - width / 2;
        float nrightX = getMeasuredWidth() / 2 + width / 2;
        float ntopY = getMeasuredHeight() / 2 - height / 2;
        float nbottomY = getMeasuredHeight() / 2 + height / 2;

        if (nleftX != rect.left ||
                nrightX != rect.right ||
                ntopY != rect.top ||
                nbottomY != rect.bottom) {

            rect.set((int) nleftX, (int) ntopY, (int) nrightX, (int) nbottomY);
        }

        return rect;
    }

    private Path getProgressPath() {

        float nleftX = getMeasuredWidth() / 2 - width / 2;
        float nrightX = getMeasuredWidth() / 2 + width / 2;
        float ntopY = getMeasuredHeight() / 2 - height / 2;
        float nbottomY = getMeasuredHeight() / 2 + height / 2;

        float startX = (nleftX + nrightX) / 2;
        float startY = ntopY;

        pathProgress = new Path();
        pathProgress.moveTo(startX, startY);

        if (mProgress > 0) {
            if (mProgress > 0.125f) {
                pathProgress.lineTo(nrightX, ntopY);
            }
            else {
                float finalX = mProgress / 0.125f * (nrightX - startX) + startX;
                pathProgress.lineTo(finalX, ntopY);
            }
        }
        if (mProgress > 0.125f) {
            if (mProgress > 0.375f) {
                pathProgress.lineTo(nrightX, nbottomY);
            }
            else {
                float finalY = (mProgress - 0.125f) / 0.25f * (nbottomY - ntopY) + ntopY;
                pathProgress.lineTo(nrightX, finalY);
            }
        }
        if (mProgress > 0.375f) {
            if (mProgress > 0.625f) {
                pathProgress.lineTo(nleftX, nbottomY);
            }
            else {
                float finalX = nrightX - (mProgress - 0.375f) / 0.25f * (nrightX - nleftX);
                pathProgress.lineTo(finalX, nbottomY);
            }
        }
        if (mProgress > 0.625f) {
            if (mProgress > 0.875f) {
                pathProgress.lineTo(nleftX, ntopY);
            }
            else {
                float finalY = nbottomY - (mProgress - 0.625f) / 0.25f * (nbottomY - ntopY);
                pathProgress.lineTo(nleftX, finalY);
            }
        }
        if (mProgress > 0.875f) {
            if (mProgress >= 1.0f) {
                pathProgress.lineTo(startX, ntopY);
            }
            else {
                float finalX = (mProgress - 0.875f) / 0.125f * (startX - nleftX) + nleftX;
                pathProgress.lineTo(finalX, ntopY);
            }
        }

        return pathProgress;
    }
}
