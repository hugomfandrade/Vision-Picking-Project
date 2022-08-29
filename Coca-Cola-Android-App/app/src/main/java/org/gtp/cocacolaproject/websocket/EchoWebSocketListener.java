package org.gtp.cocacolaproject.websocket;

import android.util.Log;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public final class EchoWebSocketListener extends WebSocketListener {

    private static final String TAG = EchoWebSocketListener.class.getSimpleName();

    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private final IEchoWebSocketListener listener;

    public EchoWebSocketListener(IEchoWebSocketListener listener) {
        this.listener = listener;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.e(TAG, "onOpen");
        if (listener != null)
            listener.onOpen("Opening : ");

        //webSocket.send("Hello, it's SSaurel !");
        //webSocket.send("What's up ?");
        //webSocket.send(ByteString.decodeHex("deadbeef"));
        //webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !");
    }
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.e(TAG, "onMessage " + text);
        if (listener != null)
            listener.onOutput("Receiving : " + text);
    }
    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.e(TAG, "onMessage " + bytes.hex());
        Log.e(TAG, "onClose");
        if (listener != null)
            listener.onOutput("Receiving bytes : " + bytes.hex());
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        Log.e(TAG, "onClose");
        //webSocket.close(NORMAL_CLOSURE_STATUS, null);
        if (listener != null) listener.onClose("Closed : " + code + " = " + reason);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        //Log.e(TAG, "onClose");
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        //if (listener != null) listener.onClose("Closing : " + code + " = " + reason);
            //listener.onOutput("Closing : " + code + " = " + reason);
    }
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        Log.e(TAG, "onFailure " + t.getMessage());

        if (listener != null)
            listener.onFailure("Failure : " + t.getMessage());
    }

    public interface IEchoWebSocketListener {

        void onOutput(String message);

        void onClose(String message);

        void onOpen(String message);

        void onFailure(String message);
    }
}