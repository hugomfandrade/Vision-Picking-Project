package org.gtp.cocacolaproject.presenter.state;

import android.util.Log;

import org.gtp.cocacolaproject.data.Order;
import org.gtp.cocacolaproject.data.parser.JsonParser;
import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImpl;
import org.gtp.cocacolaproject.presenter.Transition;
import org.gtp.cocacolaproject.tensorflow.TensorFlowConstants;
import org.gtp.cocacolaproject.websocket.WebSocketConstants;
import org.gtp.cocacolaproject.websocket.WebSocketModel;
import org.gtp.cocacolaproject.websocket.WebSocketModelAdapter;
import org.json.JSONException;
import org.json.JSONObject;

public class ShowWeightState extends StateImpl {

    private final State state = State.SHOW_WEIGHT;

    private final WebSocketModel mWebSocketModel;
    private final Order mOrder;

    private enum WaitingState {
        waitingForClick,
        waitingForServer
    }

    private WaitingState waitingState;

    private WaitThread mWaitThread;

    /**
     * show weight or number of items and wait (max 3 times) for confirmation from hunter
     *
     * @param listener listener to report progress
     */
    public ShowWeightState(WebSocketModel webSocketModel, Order order, ShowWeightStateListener listener) {
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
            startWaitingForUser();
        }

    }

    private void startWaitingForUser() {

        waitingState = WaitingState.waitingForClick;

        if (mWaitThread != null) {
            mWaitThread.interrupt();
            mWaitThread = null;
        }

        ((ShowWeightStateListener) mListener).showButton("Ok");
        ((ShowWeightStateListener) mListener).showPackageVolumeMessage(((int) mOrder.getPackageCount()) + " packages");

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
        mListener.onProgress(state, transition);
    }

    @Override
    public boolean onButtonClicked() {
        if (waitingState == WaitingState.waitingForClick) {
            waitingState = WaitingState.waitingForServer;
            ((ShowWeightStateListener) mListener).hideButton();


            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(WebSocketConstants.OPERATION_TYPE, WebSocketConstants.OperationType.REQUEST_PACKAGE_VOLUME_CONFIRMATION.shortName());
                jsonObject.put(WebSocketConstants.ORDER, new JsonParser().format(mOrder));
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
            return true;
        }
        return super.onButtonClicked();
    }

    private final WebSocketModelAdapter mWebSocketModelListener = new WebSocketModelAdapter() {

        @Override
        public void onWebSocketMessage(String message) {

            if (waitingState != WaitingState.waitingForServer) {
                return;
            }
            if (message != null && message.contains("{") && message.contains("}") && message.indexOf("{") < message.lastIndexOf("}")) {
                try {
                    Log.e(TAG, "onWebSocketMessage: " + message);

                    JSONObject jsonObject = new JSONObject(message.substring(message.indexOf("{"), message.lastIndexOf("}") + 1));

                    if (jsonObject.has(WebSocketConstants.OPERATION_TYPE) &&
                            jsonObject.getString(WebSocketConstants.OPERATION_TYPE)
                                    .equals(WebSocketConstants.OperationType.PACKAGE_VOLUME_CONFIRMATION.shortName())) {

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

                    if (mWaitThread != null) {
                        mWaitThread.failed();
                    }
                }
            }
        }
    };

    private int numberOfTries = 0;

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



            if (wasSuccessful) {
                ((ShowWeightStateListener) mListener).showToastMessage("Successfully packed " + mOrder.getProductDescription());
                onProgress(Transition.SUCCESSFUL);
            }
            else {
                numberOfTries++;
                if (numberOfTries >= TensorFlowConstants.MAX_NUMBER_OF_TRIES) {

                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(WebSocketConstants.OPERATION_TYPE, WebSocketConstants.OperationType.REPORT_PACKAGE_VOLUME.shortName());
                        jsonObject.put(WebSocketConstants.REPORT_PACKAGE_VOLUME, "Failed");
                        jsonObject.put(WebSocketConstants.ORDER, new JsonParser().format(mOrder));
                        mWebSocketModel.sendJsonObject(jsonObject);

                        ((ShowWeightStateListener) mListener).showToastMessage("wrong package volume of " + mOrder.getProductDescription());
                        onProgress(Transition.SUCCESSFUL);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        onProgress(Transition.FAILED);
                    }
                }
                else {
                    ((ShowWeightStateListener) mListener).showToastMessage("wrong package volume of " + mOrder.getProductDescription());
                    //((ShowWeightStateListener) mListener).showToastMessage("Try again");
                    startWaitingForUser();
                }
            }

            //onProgress(wasSuccessful ? Transition.SUCCESSFUL : Transition.FAILED);
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
