package org.gtp.cocacolaproject.websocket;

public interface WebSocketModelListener {

    void onWebSocketErrorMessage(String message);

    void onWebSocketMessage(String message);

    void onWebSocketClose();

    void onWebSocketOpen();
}
