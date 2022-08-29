package org.gtp.cocacolaproject.presenter.state;

import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImplListener;
import org.gtp.cocacolaproject.presenter.Transition;
import org.gtp.cocacolaproject.tensorflow.TensorFlowModel;

public abstract class ShowWeightStateListener implements StateImplListener {

    private final StateImplListener mListener;

    public ShowWeightStateListener(StateImplListener listener) {
        mListener = listener;
    }

    @Override
    public void onProgress(State state, Transition transition) {
        if (mListener != null) {
            mListener.onProgress(state, transition);
        }
    }

    public abstract void showButton(String message);

    public abstract void hideButton();

    public abstract void showPackageVolumeMessage(String message);

    public abstract void hidePackageVolumeMessage();

    public abstract void showToastMessage(String message);
}
