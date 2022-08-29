package org.gtp.cocacolaproject.presenter;

import org.gtp.cocacolaproject.websocket.WebSocketModel;

public interface StateImplListener {

    void onProgress(State state, Transition transition);
}
