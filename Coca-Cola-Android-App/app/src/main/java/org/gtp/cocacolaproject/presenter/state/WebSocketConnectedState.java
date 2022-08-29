package org.gtp.cocacolaproject.presenter.state;

import android.content.Context;
import android.os.Handler;

import org.gtp.cocacolaproject.data.LoginData;
import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImpl;
import org.gtp.cocacolaproject.presenter.StateImplListener;
import org.gtp.cocacolaproject.presenter.Transition;
import org.gtp.cocacolaproject.utils.SharedPreferencesUtils;
import org.gtp.cocacolaproject.websocket.WebSocketConstants;
import org.gtp.cocacolaproject.websocket.WebSocketModel;
import org.gtp.cocacolaproject.websocket.WebSocketModelAdapter;
import org.json.JSONException;
import org.json.JSONObject;

public class WebSocketConnectedState extends StateImpl {

    private final State state = State.WEB_SOCKET_CONNECTED;

    private final Context mContext;
    private final WebSocketModel mWebSocketModel;
    private WaitThread mWaitThread;

    private Handler mDelayHandler;
    private Runnable mRunnable;

    /**
     * authenticate
     *
     * @param listener listener to report progress
     */
    public WebSocketConnectedState(WebSocketModel webSocketModel, Context context, StateImplListener listener) {
        super(listener);

        mContext = context;
        mWebSocketModel = webSocketModel;
        mWebSocketModel.addListener(mWebSocketModelListener);

        onProgress(Transition.INITIAL);
    }

    @Override
    public void start() {
        onProgress(Transition.RUNNING);

        mDelayHandler = new Handler();
        mRunnable = new Runnable() {

            @Override
            public void run() {

                LoginData loginData = SharedPreferencesUtils.getLoginData(mContext);

                if (loginData.getUsername() == null || loginData.getPassword() == null) {
                    loginData = new LoginData("Hugo", "Password");
                }

                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(WebSocketConstants.OPERATION_TYPE, WebSocketConstants.OperationType.AUTHENTICATE.shortName());
                    jsonObject.put(LoginData.Entry.USERNAME, loginData.getUsername());
                    jsonObject.put(LoginData.Entry.PASSWORD, loginData.getPassword());
                    mWebSocketModel.sendJsonObject(jsonObject);

                    if (mWaitThread != null) {
                        mWaitThread.interrupt();
                        mWaitThread = null;
                    }

                    mWaitThread = new WaitThread();
                    mWaitThread.start();

                } catch (JSONException e) {
                    e.printStackTrace();

                    onProgress(Transition.FAILED);
                }
            }
        };

        mHandler.postDelayed(mRunnable,1000L);
    }

    @Override
    public void abort() {
        if (mWaitThread != null) {
            mWaitThread.interrupt();
            mWaitThread = null;
        }
        if (mWebSocketModel != null) {
            mWebSocketModel.removeListener(mWebSocketModelListener);
        }
        if (mDelayHandler != null) {
            if (mRunnable != null) {
                mDelayHandler.removeCallbacks(mRunnable);
            }
        }
        if (mHandler != null){
            mHandler.shutdown();
        }

    }

    @Override
    public void progress(Transition transition, String message) {
        if (transition == Transition.FAILED || transition == Transition.SUCCESSFUL) {
            if (mWaitThread != null) {
                mWaitThread.interrupt();
                mWaitThread = null;
            }
            if (mWebSocketModel != null) {
                mWebSocketModel.removeListener(mWebSocketModelListener);
            }
            if (mDelayHandler != null) {
                if (mRunnable != null) {
                    mDelayHandler.removeCallbacks(mRunnable);
                }
            }
        }

        mListener.onProgress(state, transition);
    }

    private final WebSocketModelAdapter mWebSocketModelListener = new WebSocketModelAdapter() {

        @Override
        public void onWebSocketMessage(String message) {

            if (message != null && message.contains("{") && message.contains("}") && message.indexOf("{") < message.lastIndexOf("}")) {
                try {
                    JSONObject jsonObject = new JSONObject(message.substring(message.indexOf("{"), message.lastIndexOf("}") + 1));

                    if (jsonObject.has(WebSocketConstants.OPERATION_TYPE) &&
                            jsonObject.getString(WebSocketConstants.OPERATION_TYPE)
                                    .equals(WebSocketConstants.OperationType.AUTHENTICATE.shortName())) {

                        if (jsonObject.has(WebSocketConstants.OPERATION_RESULT) &&
                                jsonObject.getString(WebSocketConstants.OPERATION_RESULT)
                                        .equals(WebSocketConstants.OPERATION_RESULT_OK)) {

                            if (jsonObject.has(LoginData.Entry.USERNAME) && jsonObject.has(LoginData.Entry.PASSWORD)) {
                                SharedPreferencesUtils.putLoginData(mContext,
                                        new LoginData(
                                                jsonObject.getString(LoginData.Entry.USERNAME),
                                                jsonObject.getString(LoginData.Entry.PASSWORD)));
                            }
                            if (mWaitThread != null) {
                                mWaitThread.success();
                            }
                        } else {
                            if (mWaitThread != null) {
                                mWaitThread.failed();
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };


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
