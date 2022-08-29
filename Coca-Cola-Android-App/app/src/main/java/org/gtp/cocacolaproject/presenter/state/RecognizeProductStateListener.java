package org.gtp.cocacolaproject.presenter.state;

import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImplListener;
import org.gtp.cocacolaproject.presenter.Transition;
import org.gtp.cocacolaproject.tensorflow.TensorFlowModel;

public abstract class RecognizeProductStateListener implements StateImplListener {

    private final StateImplListener mListener;

    public RecognizeProductStateListener(StateImplListener listener) {
        mListener = listener;
    }

    @Override
    public void onProgress(State state, Transition transition) {
        if (mListener != null) {
            mListener.onProgress(state, transition);
        }
    }

    public abstract void setTensorFlowModel(TensorFlowModel tensorFlowModel);

    public abstract void setRecognitionTimerVisibilityState(boolean isVisible);

    public abstract void setRecognitionTimerState(boolean enable);

    public abstract void showMessageToast(String message);

    public abstract void showRecognition(String message);
}
