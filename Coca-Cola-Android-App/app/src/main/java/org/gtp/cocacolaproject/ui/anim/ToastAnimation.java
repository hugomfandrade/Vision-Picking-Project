package org.gtp.cocacolaproject.ui.anim;

import android.view.animation.Animation;
import android.view.animation.Transformation;

public final class ToastAnimation extends Animation {

    private final static String TAG = ToastAnimation.class.getSimpleName();

    private long toOpaqueDuration = 200L;
    private long toTransparentDuration = 200L;


    // Constructor
    public ToastAnimation() {
    }

    @Override
    public void applyTransformation(float interpolatedTime, Transformation t) {
        long duration = getDuration();
        long currentDuration = (long) (interpolatedTime * duration);

        if (currentDuration < toOpaqueDuration) {
            final float toOpaqueInterpolatedTime = (float) currentDuration / (float) toOpaqueDuration;
            final float alpha = 0;
            t.setAlpha(alpha + ((1f - alpha) * toOpaqueInterpolatedTime));
        }
        else if ((duration - currentDuration) < toTransparentDuration) {
            final float toTransparentInterpolatedTime = (duration - currentDuration) / (float) toTransparentDuration;
            final float alpha = 1f;
            t.setAlpha(alpha + ((0f - alpha) * (1f - toTransparentInterpolatedTime)));
        }
        else {
            t.setAlpha(1f);
        }

    }

    public void setToOpaqueDuration(long toOpaqueDuration) {
        this.toOpaqueDuration = toOpaqueDuration;
    }

    public void setToTransparentDuration(long toTransparentDuration) {
        this.toTransparentDuration = toTransparentDuration;
    }

    public static class Builder {

        final ToastAnimation t;

        public Builder() {
            t = new ToastAnimation();
        }

        public Builder setDuration(long duration) {
            t.setDuration(duration);
            return this;
        }

        public Builder setToOpaqueDuration(long toOpaqueDuration) {
            t.setToOpaqueDuration(toOpaqueDuration);
            return this;
        }

        public Builder setToTransparentDuration(long toTransparentDuration) {
            t.setToTransparentDuration(toTransparentDuration);
            return this;
        }

        public Builder setFillAfter(boolean fillAfter) {
            t.setFillAfter(fillAfter);
            return this;
        }

        public Builder setAnimationListener(AnimationListener listener) {
            t.setAnimationListener(listener);
            return this;
        }

        public ToastAnimation create() {
            return t;
        }
    }
}
