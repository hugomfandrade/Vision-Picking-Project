package org.gtp.cocacolaproject.presenter.state;

import android.content.Context;
import android.util.Log;

import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImpl;
import org.gtp.cocacolaproject.presenter.StateImplListener;
import org.gtp.cocacolaproject.presenter.Transition;
import org.gtp.cocacolaproject.utils.NetworkUtils;
import org.gtp.cocacolaproject.websocket.FindLocalIPAddressReceiver;
import org.gtp.cocacolaproject.websocket.FindLocalIPAddressService;
import org.gtp.cocacolaproject.websocket.WebSocketConstants;
import org.gtp.cocacolaproject.websocket.WebSocketModel;
import org.gtp.cocacolaproject.websocket.WebSocketModelAdapter;

import java.util.List;

public class WifiConnectedState extends StateImpl {

    private final State state = State.WIFI_CONNECTED;

    private Context mContext;

    private WebSocketModel mWebSocketModel;

    private WaitThread waitThread;

    /**
     * connect to web socket
     */
    public WifiConnectedState(WebSocketModel webSocketModel, Context context, WifiConnectedStateListener listener) {
        super(listener);

        mContext = context;
        mWebSocketModel = webSocketModel;

        if (mWebSocketModel == null) {
            mWebSocketModel = new WebSocketModel(mContext);
        }

        onProgress(Transition.INITIAL);
    }

    @Override
    public void start() {
        onProgress(Transition.RUNNING);

        tryToConnect();
    }

    @Override
    public void abort() {
        if (mWebSocketModel != null) {
            mWebSocketModel.removeListener(mWebSocketModelAdapter);
            mWebSocketModel = null;
        }
        if (mHandler != null){
            mHandler.shutdown();
        }
    }

    @Override
    public void progress(Transition transition, String message) {
        Log.e(TAG, "progress = " + transition + " = " + message);

        if (transition == Transition.INITIAL) {
            if (mWebSocketModel != null)
                mWebSocketModel.addListener(mWebSocketModelAdapter);
        }
        else if (transition == Transition.FAILED || transition == Transition.SUCCESSFUL) {
            if (mWebSocketModel != null)
                mWebSocketModel.removeListener(mWebSocketModelAdapter);

            if (waitThread != null) {
                waitThread.interrupt();
                waitThread = null;
            }
        }

        if (mWebSocketModel != null) {
            ((WifiConnectedStateListener) mListener).setWebSocketModel(mWebSocketModel);
        }

        mListener.onProgress(state, transition);
    }

    private WebSocketModelAdapter mWebSocketModelAdapter = new WebSocketModelAdapter() {

        @Override
        public void onWebSocketOpen() {
            Log.e(TAG, "onWebSocketOpen");
            if (waitThread != null) {
                waitThread.success();
            }
        }

        @Override
        public void onWebSocketClose() {
            Log.e(TAG, "onWebSocketClose");
        }

        @Override
        public void onWebSocketErrorMessage(String message) {
            Log.e(TAG, "onWebSocketErrorMessage = " + message);
            if (!mWebSocketModel.isConnected) {
                if (waitThread != null) {
                    waitThread.failed();
                }
            }
        }

        @Override
        public void onWebSocketMessage(String message) {
            //Log.e(TAG, "onWebSocketMessage = " + message);

        }
    };

    private void tryToConnect() {
        Log.e(TAG, "tryToConnect = ");

        if (!mWebSocketModel.isConnected) {
            lookForIP();
        }
        else {
            onProgress(Transition.SUCCESSFUL);
        }
    }

    private void lookForIP() {
        Log.e(TAG, "lookForIP = ");

        FindLocalIPAddressService.startService(mContext, new FindLocalIPAddressReceiver.ResultReceiverCallBack() {

            @Override
            public void onSuccess(List<String> localIPAddressList) {

                if (localIPAddressList.size() == 0 || localIPAddressList.size() != 1) {
                    String message;
                    if (localIPAddressList.size() == 0)
                        message = "No local IP address with port " + WebSocketConstants.PORT + " found";
                    else
                        message = "(E) Multiple local IP addresses with port " + WebSocketConstants.PORT + " found";

                    onProgress(Transition.FAILED, message);
                }
                else {
                    String localIPAddress = localIPAddressList.get(0);
                    connect(localIPAddress);
                }
            }

            @Override
            public void onError(String errorMessage) {
                onProgress(Transition.FAILED, errorMessage);
            }
        });
    }

    private void connect(String localIPAddress) {
        Log.e(TAG, "connect = " + localIPAddress);

        if (mWebSocketModel.connect(NetworkUtils.buildUrl(localIPAddress, WebSocketConstants.PORT))) {
            // No-ops
            if (waitThread != null) {
                waitThread.interrupt();
                waitThread = null;
            }

            waitThread = new WaitThread();
            waitThread.start();
        }
        else {
            onProgress(Transition.FAILED);
        }
    }

    private class WaitThread extends Thread {

        boolean wasSuccessful = false;
        boolean hasResponse = false;

        final Object lock = new Object();

        @Override
        public void run() {

            boolean hasResponse;
            boolean wasSuccessful = false;
            do {

                if (isInterrupted())
                    return;

                synchronized (lock) {
                    hasResponse = doWork();

                    if (hasResponse) {
                        wasSuccessful = this.wasSuccessful;
                    }
                }
            } while (!hasResponse);

            onProgress(wasSuccessful ? Transition.SUCCESSFUL : Transition.FAILED);
        }

        private boolean doWork() {
            return hasResponse;
        }

        public synchronized void success() {
            synchronized (lock) {
                wasSuccessful = true;
                hasResponse = true;
            }
        }

        public synchronized void failed() {
            synchronized (lock) {
                wasSuccessful = false;
                hasResponse = true;
            }
        }
    }
}
