package org.gtp.cocacolaproject.presenter.state;

import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImplListener;
import org.gtp.cocacolaproject.presenter.Transition;

public abstract class ReceivedOrdersStateListener implements StateImplListener {

    private final StateImplListener mListener;

    public ReceivedOrdersStateListener(StateImplListener listener) {
        mListener = listener;
    }

    public abstract void showDestinationName(String destinationName);

    @Override
    public void onProgress(State state, Transition transition) {
        if (mListener != null) {
            mListener.onProgress(state, transition);
        }
    }
}
