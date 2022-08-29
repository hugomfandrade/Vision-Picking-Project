package org.gtp.cocacolaproject.presenter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import org.gtp.cocacolaproject.data.Order;
import org.gtp.cocacolaproject.data.parser.JsonParser;
import org.gtp.cocacolaproject.presenter.state.AuthenticatedState;
import org.gtp.cocacolaproject.presenter.state.AuthenticatedStateListener;
import org.gtp.cocacolaproject.presenter.state.InitialState;
import org.gtp.cocacolaproject.presenter.state.ReceivedOrdersState;
import org.gtp.cocacolaproject.presenter.state.ReceivedOrdersStateListener;
import org.gtp.cocacolaproject.presenter.state.RecognizeProductState;
import org.gtp.cocacolaproject.presenter.state.RecognizeProductStateListener;
import org.gtp.cocacolaproject.presenter.state.ShowOrderState;
import org.gtp.cocacolaproject.presenter.state.ShowWeightState;
import org.gtp.cocacolaproject.presenter.state.ShowWeightStateListener;
import org.gtp.cocacolaproject.presenter.state.WebSocketConnectedState;
import org.gtp.cocacolaproject.presenter.state.WifiConnectedState;
import org.gtp.cocacolaproject.presenter.state.WifiConnectedStateListener;
import org.gtp.cocacolaproject.tensorflow.TensorFlowConstants;
import org.gtp.cocacolaproject.tensorflow.TensorFlowModel;
import org.gtp.cocacolaproject.utils.NetworkBroadcastReceiverUtils;
import org.gtp.cocacolaproject.view.MainActivityListener;
import org.gtp.cocacolaproject.websocket.WebSocketConstants;
import org.gtp.cocacolaproject.websocket.WebSocketModel;
import org.gtp.cocacolaproject.websocket.WebSocketModelListener;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainPresenter implements WebSocketModelListener, StateImplListener {

    private final static String TAG = MainPresenter.class.getSimpleName();

    private WeakReference<MainActivityListener> mView;

    private BroadcastReceiver mNetworkBroadcastReceiver;
    private WebSocketModel mWebSocketModel;
    private TensorFlowModel mTensorFlowModel;

    private ArrayList<Order> mOrders = new ArrayList<Order>();
    private int mOrderIt = 0;

    private State currentState = State.INITIAL;

    private StateImpl currentStateImpl;

    public void onCreate(MainActivityListener view) {

        // Set the WeakReference.
        mView = new WeakReference<MainActivityListener>(view);

        mWebSocketModel = new WebSocketModel(getApplicationContext());
        mWebSocketModel.addListener(this);

        mNetworkBroadcastReceiver = NetworkBroadcastReceiverUtils.register(getApplicationContext(), iNetworkBroadcastReceiver);

        startState();
        mView.get().hideButton();
    }

    public void onResume() {

        if (mTensorFlowModel != null) {
            mTensorFlowModel.onResume();
        }
    }

    public void onPause() {

        if (mTensorFlowModel != null) {
            mTensorFlowModel.onPause();
        }
    }

    public void onDestroy(boolean isChangingConfigurations) {
        if (isChangingConfigurations)
            Log.d(TAG,
                    "just a configuration change - unbindService() not called");
        else {
            if (currentStateImpl != null) {
                currentStateImpl.abort();
            }
            if (mWebSocketModel != null) {
                mWebSocketModel.removeListener(this);
            }
            NetworkBroadcastReceiverUtils.unregister(getApplicationContext(), mNetworkBroadcastReceiver);

        }
        if (mWebSocketModel != null) {
            mWebSocketModel.onDestroy(isChangingConfigurations);
        }
    }
    private NetworkBroadcastReceiverUtils.INetworkBroadcastReceiver iNetworkBroadcastReceiver = new NetworkBroadcastReceiverUtils.INetworkBroadcastReceiver() {
        @Override
        public void setNetworkAvailable(boolean isNetworkAvailable) {
            if (!isNetworkAvailable) {
                setState(State.INITIAL);
            }
        }
    };

    public boolean onPreviewFrame(byte[] bytes, Camera camera) {
        if (currentStateImpl instanceof RecognizeProductState) {
            return ((RecognizeProductState) currentStateImpl).onPreviewFrame(bytes, camera);
        }
        return false;
    }

    public void onButtonClicked() {
        if (currentStateImpl == null) {
            startState();
            mView.get().hideButton();
        }
        else {
            if (!currentStateImpl.onButtonClicked()) {
                startState();
                mView.get().hideButton();
            }
        }

    }

    public void onCheatButtonClicked() {
        if (currentStateImpl instanceof RecognizeProductState) {

            final Order order =  (mOrderIt < mOrders.size()) ? mOrders.get(mOrderIt) : null;
            currentStateImpl.abort();
            recognizeProductStateListener.setRecognitionTimerState(true);
            recognizeProductStateListener.setRecognitionTimerVisibilityState(true);

            final int[] recognitionIteration = {0};
            final long recognitionTimer = TensorFlowConstants.recognitionDuration; // 4 seconds
            final long recognitionRate = 30L; // 30 hertz
            final long recognitionPeriod = 1000 / recognitionRate; // period
            final int numberOfIterations = (int) (recognitionTimer / recognitionRate) + 1;
            final Timer mRecognitionTimer = new Timer();
            mRecognitionTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    recognizeProductStateListener.showRecognition(order.getProductDescription() + ": " + String.format(Locale.UK, "(%.1f%%) ", 0.93 * 100.0f) );

                    if (recognitionIteration[0] >= numberOfIterations) {

                        mRecognitionTimer.cancel();

                        recognizeProductStateListener.setRecognitionTimerState(false);
                        recognizeProductStateListener.setRecognitionTimerVisibilityState(false);

                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(WebSocketConstants.OPERATION_TYPE, WebSocketConstants.OperationType.REPORT_RECOGNITION.shortName());
                            jsonObject.put(WebSocketConstants.REPORT_RECOGNITION_RESULT, "Success");
                            jsonObject.put(WebSocketConstants.ORDER, new JsonParser().format(order));
                            mWebSocketModel.sendJsonObject(jsonObject);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        recognizeProductStateListener.showMessageToast("Recognized " + order.getProductDescription());

                        startNextState();
                    }
                    else {
                        recognitionIteration[0]++;
                    }
                }
            }, 0L, recognitionPeriod);
        }
    }

    private void setState(State state) {
        if (currentStateImpl != null) {
            currentStateImpl.abort();
        }

        currentState = state;
    }

    private void startNextState() {
        currentState = currentState.nextState();

        if (currentState == State.RECEIVED_ORDERS && mOrderIt >= mOrders.size()) {
            currentState = State.FINALLY;
        }

        startState();
    }

    private void startState() {
        if (currentStateImpl != null) {
            currentStateImpl.abort();
        }

        Log.d(TAG, "startState = " + currentState);

        switch (currentState) {
            case INITIAL:
                currentStateImpl = new InitialState(getApplicationContext(), this);
                currentStateImpl.start();
                break;
            case WIFI_CONNECTED:
                currentStateImpl = new WifiConnectedState(mWebSocketModel, getApplicationContext(), wifiConnectedStateListener);
                currentStateImpl.start();
                break;
            case WEB_SOCKET_CONNECTED:
                currentStateImpl = new WebSocketConnectedState(mWebSocketModel, getApplicationContext(), this);
                currentStateImpl.start();
                break;
            case AUTHENTICATED:
                currentStateImpl = new AuthenticatedState(mWebSocketModel, authenticatedStateListener);
                currentStateImpl.start();
                break;
            case RECEIVED_ORDERS:
                currentStateImpl = new ReceivedOrdersState(mWebSocketModel, receivedOrdersStateListener, (mOrderIt < mOrders.size()) ? mOrders.get(mOrderIt) : null);
                currentStateImpl.start();
                break;
            case SHOW_ORDER:
                currentStateImpl = new ShowOrderState(this);
                currentStateImpl.start();
                break;
            case RECOGNIZE_PRODUCT:
                currentStateImpl = new RecognizeProductState(mWebSocketModel, getActivity(), getApplicationContext(), (mOrderIt < mOrders.size()) ? mOrders.get(mOrderIt) : null, recognizeProductStateListener);
                currentStateImpl.start();
                break;
            case SHOW_WEIGHT:
                currentStateImpl = new ShowWeightState(mWebSocketModel, (mOrderIt < mOrders.size()) ? mOrders.get(mOrderIt) : null, showWeightStateListener);
                currentStateImpl.start();
                break;
            case FINALLY:
                break;
        }
    }

    private ReceivedOrdersStateListener receivedOrdersStateListener = new ReceivedOrdersStateListener(this) {
        @Override
        public void showDestinationName(String destinationName) {
            mView.get().displayMessage(destinationName);
        }
    };

    private WifiConnectedStateListener wifiConnectedStateListener = new WifiConnectedStateListener(this) {
        @Override
        public void setWebSocketModel(WebSocketModel webSocketModel) {
            mWebSocketModel = webSocketModel;
            mWebSocketModel.addListener(MainPresenter.this);
        }
    };

    private ShowWeightStateListener showWeightStateListener = new ShowWeightStateListener(this) {

        @Override
        public void showButton(String message) {
            mView.get().showButton(message);
        }

        @Override
        public void hideButton() {
            mView.get().hideButton();
        }

        @Override
        public void showPackageVolumeMessage(String message) {
            mView.get().displayMessage(message);
        }

        @Override
        public void hidePackageVolumeMessage() {
            mView.get().hideMessage();
        }

        @Override
        public void showToastMessage(String message) {
            mView.get().displayToastMessage(message);
        }
    };

    private RecognizeProductStateListener recognizeProductStateListener = new RecognizeProductStateListener(this) {
        @Override
        public void setTensorFlowModel(TensorFlowModel tensorFlowModel) {
            mTensorFlowModel = tensorFlowModel;
        }

        @Override
        public void setRecognitionTimerVisibilityState(boolean isVisible) {
            mView.get().setRecognitionTimerVisibilityState(isVisible);
        }

        @Override
        public void setRecognitionTimerState(boolean enable) {
            mView.get().setRecognitionTimerState(enable);
        }

        @Override
        public void showMessageToast(String message) {
            mView.get().displayToastMessage(message);
        }

        @Override
        public void showRecognition(String message) {
            mView.get().showRecognition(message);
        }
    };

    private AuthenticatedStateListener authenticatedStateListener = new AuthenticatedStateListener(this) {
        @Override
        public void setOrders(ArrayList<Order> orders) {
            MainPresenter.this.mOrders = orders;
        }
    };

    @Override
    public void onProgress(final State state, final Transition transition) {
        if (mView.get() == null) {
            return;
        }
        Log.e(TAG, "onProgress " + state + " - " + transition);
        if (transition == Transition.INITIAL) {
            if (state == State.RECEIVED_ORDERS
                    || state == State.SHOW_ORDER
                    || state == State.RECOGNIZE_PRODUCT
                    || state == State.SHOW_WEIGHT) {
                mView.get().showOrder((mOrderIt < mOrders.size()) ? mOrders.get(mOrderIt) : null);
            }
            else {
                mView.get().hideOrder();
            }

            //mView.get().hideButton();
            //mView.get().hideMessage();
        }
        else if (transition == Transition.RUNNING) {
            //mView.get().hideButton();
        }
        else if (transition == Transition.FAILED) {
            if (state == State.RECEIVED_ORDERS || state == State.SHOW_WEIGHT) {
                mView.get().hideMessage();
            }
            //if (state == State.WEB_SOCKET_CONNECTED) { setState(State.WIFI_CONNECTED); }
            mView.get().displayToastMessage("An Error occured in " + state);
            mView.get().showButton("Reset");
        }
        else if (transition == Transition.SUCCESSFUL) {
            if (state == State.AUTHENTICATED) {
                mOrderIt = 0;
            }
            if (state == State.SHOW_WEIGHT) {
                mOrderIt++;
            }
            if (state == State.RECEIVED_ORDERS || state == State.SHOW_WEIGHT) {
                mView.get().hideMessage();
            }
            /*if (state == State.WIFI_CONNECTED) {
                Log.e(TAG, "add Listener");
                mWebSocketModel.addListener(mWebSocketModelAdapter);
            }/**/
            startNextState();
        }
    }

    @Override
    public void onWebSocketErrorMessage(String message) {
        if (message.contains(WebSocketConstants.connectionResetMessage)) {
            setState(State.WIFI_CONNECTED);

            mView.get().displayToastMessage("Lost WebSocket");
            mView.get().showButton("Reset");
        }
    }

    @Override
    public void onWebSocketMessage(String message) {
        // No-ops
    }

    @Override
    public void onWebSocketClose() {
        Log.e(TAG, "onWebSocketClose");
        setState(State.WIFI_CONNECTED);

        mView.get().displayToastMessage("Lost WebSocket");
        mView.get().showButton("Reset");
    }

    @Override
    public void onWebSocketOpen() {
        Log.e(TAG, "onWebSocketOpen");
    }

    private Context getApplicationContext() {
        return mView.get().getApplicationContext();
    }

    private Activity getActivity() {
        return mView.get().getActivity();
    }
}
