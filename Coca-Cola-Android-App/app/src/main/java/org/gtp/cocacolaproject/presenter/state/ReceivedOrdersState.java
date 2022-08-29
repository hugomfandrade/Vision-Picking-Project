package org.gtp.cocacolaproject.presenter.state;

import android.util.Log;

import org.gtp.cocacolaproject.data.Order;
import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImpl;
import org.gtp.cocacolaproject.presenter.Transition;
import org.gtp.cocacolaproject.websocket.WebSocketConstants;
import org.gtp.cocacolaproject.websocket.WebSocketModel;
import org.gtp.cocacolaproject.websocket.WebSocketModelAdapter;
import org.json.JSONException;
import org.json.JSONObject;

public class ReceivedOrdersState extends StateImpl {

    private final State state = State.RECEIVED_ORDERS;

    private final WebSocketModel mWebSocketModel;
    private final Order mOrder;
    private WaitThread mWaitThread;

    /**
     * show first in list
     *
     * @param listener listener to report progress
     */
    public ReceivedOrdersState(WebSocketModel webSocketModel, ReceivedOrdersStateListener listener, Order order) {
        super(listener);

        mWebSocketModel = webSocketModel;
        mWebSocketModel.addListener(mWebSocketModelListener);

        mOrder = order;

        onProgress(Transition.INITIAL);
    }

    @Override
    public void start() {
        onProgress(Transition.RUNNING);

        if (mOrder == null) {
            onProgress(Transition.FAILED);
        }
        else {

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(WebSocketConstants.OPERATION_TYPE, WebSocketConstants.OperationType.REQUEST_DESTINATION_CONFIRMATION.shortName());
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
    }

    @Override
    public void progress(Transition transition, String message) {

        mListener.onProgress(state, transition);

        if (transition == Transition.RUNNING && mOrder != null) {
            ((ReceivedOrdersStateListener) mListener).showDestinationName("Go to\n" + mOrder.getDestinationName());
        }

    }

    private final WebSocketModelAdapter mWebSocketModelListener = new WebSocketModelAdapter() {

        @Override
        public void onWebSocketMessage(String message) {

            if (message != null) {
                try {
                    Log.e(TAG, "onWebSocketMessage: " + message);

                    JSONObject jsonObject = new JSONObject(message.substring(message.indexOf("{"), message.lastIndexOf("}") + 1));

                    if (jsonObject.has(WebSocketConstants.OPERATION_TYPE) &&
                            jsonObject.getString(WebSocketConstants.OPERATION_TYPE)
                                    .equals(WebSocketConstants.OperationType.DESTINATION_CONFIRMATION.shortName())) {

                        if (jsonObject.has(WebSocketConstants.OPERATION_RESULT) &&
                                jsonObject.getString(WebSocketConstants.OPERATION_RESULT)
                                        .equals(WebSocketConstants.OPERATION_RESULT_OK)) {


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
