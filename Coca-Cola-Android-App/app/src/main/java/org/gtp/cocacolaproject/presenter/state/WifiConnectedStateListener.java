package org.gtp.cocacolaproject.presenter.state;

import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImplListener;
import org.gtp.cocacolaproject.presenter.Transition;
import org.gtp.cocacolaproject.websocket.WebSocketModel;

public abstract class WifiConnectedStateListener implements StateImplListener {

    private final StateImplListener mListener;

    public WifiConnectedStateListener(StateImplListener listener) {
        mListener = listener;
    }

    public abstract void setWebSocketModel(WebSocketModel webSocketModel);

    @Override
    public void onProgress(State state, Transition transition) {
        if (mListener != null) {
            mListener.onProgress(state, transition);
        }
    }
}
