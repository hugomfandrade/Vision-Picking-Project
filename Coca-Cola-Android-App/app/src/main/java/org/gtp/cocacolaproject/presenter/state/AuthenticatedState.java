package org.gtp.cocacolaproject.presenter.state;

import android.util.Log;

import org.gtp.cocacolaproject.data.LoginData;
import org.gtp.cocacolaproject.data.Order;
import org.gtp.cocacolaproject.data.parser.JsonParser;
import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImpl;
import org.gtp.cocacolaproject.presenter.StateImplListener;
import org.gtp.cocacolaproject.presenter.Transition;
import org.gtp.cocacolaproject.utils.SharedPreferencesUtils;
import org.gtp.cocacolaproject.websocket.WebSocketConstants;
import org.gtp.cocacolaproject.websocket.WebSocketModel;
import org.gtp.cocacolaproject.websocket.WebSocketModelAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AuthenticatedState extends StateImpl {

    private final State state = State.AUTHENTICATED;

    private final WebSocketModel mWebSocketModel;

    private WaitThread mWaitThread;

    private ArrayList<Order> mOrders = new ArrayList<Order>();

    /**
     * wait for orders from web socket
     *
     * @param listener listener to report progress
     */
    public AuthenticatedState(WebSocketModel webSocketModel, AuthenticatedStateListener listener) {
        super(listener);

        mWebSocketModel = webSocketModel;
        mWebSocketModel.addListener(mWebSocketModelListener);

        onProgress(Transition.INITIAL);
    }

    @Override
    public void start() {
        onProgress(Transition.RUNNING);

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(WebSocketConstants.OPERATION_TYPE, WebSocketConstants.OperationType.REQUEST_ORDER_LIST.shortName());
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

    @Override
    public void abort() {
        if (mWaitThread != null) {
            mWaitThread.interrupt();
            mWaitThread = null;
        }
        if (mWebSocketModel != null) {
            mWebSocketModel.removeListener(mWebSocketModelListener);
        }
        if (mHandler != null){
            mHandler.shutdown();
        }
    }

    @Override
    public void progress(Transition transition, String message) {
        if (transition == Transition.FAILED || transition == Transition.SUCCESSFUL) {
            if (Transition.SUCCESSFUL == transition) {
                ((AuthenticatedStateListener) mListener).setOrders(mOrders);
            }
            if (mWaitThread != null) {
                mWaitThread.interrupt();
                mWaitThread = null;
            }
            if (mWebSocketModel != null) {
                mWebSocketModel.removeListener(mWebSocketModelListener);
            }
        }

        mListener.onProgress(state, transition);

    }

    private final WebSocketModelAdapter mWebSocketModelListener = new WebSocketModelAdapter() {

        @Override
        public void onWebSocketMessage(String message) {

            if (message != null && message.contains("{") && message.contains("}") && message.indexOf("{") < message.lastIndexOf("}")) {
                try {
                    Log.e(TAG, "onWebSocketMessage: " + message);

                    JSONObject jsonObject = new JSONObject(message.substring(message.indexOf("{"), message.lastIndexOf("}") + 1));

                    if (jsonObject.has(WebSocketConstants.OPERATION_TYPE) &&
                            jsonObject.getString(WebSocketConstants.OPERATION_TYPE)
                                    .equals(WebSocketConstants.OperationType.ORDER_LIST.shortName())) {


                        if (jsonObject.has(WebSocketConstants.OperationType.ORDER_LIST.shortName())) {

                            JSONArray jsonArray;
                            try {
                                jsonArray = jsonObject.getJSONArray(WebSocketConstants.OperationType.ORDER_LIST.shortName());
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                                if (mWaitThread != null) {
                                    mWaitThread.failed();
                                }
                                return;
                            }

                            mOrders = new JsonParser().parseOrders(jsonArray);

                            if (mWaitThread != null) {
                                mWaitThread.success();
                            }
                        } else {/**/
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
