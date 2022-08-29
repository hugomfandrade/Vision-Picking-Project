package org.gtp.cocacolaproject.ui.view;

import android.view.animation.Animation;
import android.view.animation.Transformation;

@SuppressWarnings("FieldCanBeLocal")
public class SelectedRectViewAnimation extends Animation {

    private static final String TAG = SelectedRectViewAnimation.class.getSimpleName();

    // The views to be animated
    private SelectedRectView selectedRectView;
    private float mFromValue = 0f;
    private float mToValue = 1f;

    // Constructor
    public SelectedRectViewAnimation() {
    }

    @Override
    public void applyTransformation(float interpolatedTime, Transformation t) {
        // Used to apply the animation to the view
        // Animate given the height or parentWidth
        if (selectedRectView != null) {
            selectedRectView.setProgress(mFromValue + (mToValue - mFromValue) * interpolatedTime);
        }
    }

    void setSelectedRectView(SelectedRectView selectedRectView) {
        this.selectedRectView = selectedRectView;
    }

    @Override
    public void cancel() {
        super.cancel();
    }

    @Override
    public void reset() {
        super.reset();
    }
}
