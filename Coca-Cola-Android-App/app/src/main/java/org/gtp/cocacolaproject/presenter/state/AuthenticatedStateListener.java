package org.gtp.cocacolaproject.presenter.state;

import org.gtp.cocacolaproject.data.Order;
import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImplListener;
import org.gtp.cocacolaproject.presenter.Transition;

import java.util.ArrayList;

public abstract class AuthenticatedStateListener implements StateImplListener {

    private final StateImplListener mListener;

    public AuthenticatedStateListener(StateImplListener listener) {
        mListener = listener;
    }

    public abstract void setOrders(ArrayList<Order> orders);

    @Override
    public void onProgress(State state, Transition transition) {
        if (mListener != null) {
            mListener.onProgress(state, transition);
        }
    }
}
