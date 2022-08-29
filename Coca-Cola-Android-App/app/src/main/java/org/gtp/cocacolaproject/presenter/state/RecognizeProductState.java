package org.gtp.cocacolaproject.presenter.state;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;

import org.gtp.cocacolaproject.camera.ImageCropper;
import org.gtp.cocacolaproject.data.Order;
import org.gtp.cocacolaproject.data.parser.JsonParser;
import org.gtp.cocacolaproject.presenter.State;
import org.gtp.cocacolaproject.presenter.StateImpl;
import org.gtp.cocacolaproject.presenter.Transition;
import org.gtp.cocacolaproject.tensorflow.Classifier;
import org.gtp.cocacolaproject.tensorflow.TensorFlowConstants;
import org.gtp.cocacolaproject.tensorflow.TensorFlowModel;
import org.gtp.cocacolaproject.tensorflow.TensorFlowModelListener;
import org.gtp.cocacolaproject.utils.ProductRecognitionUtils;
import org.gtp.cocacolaproject.view.MainActivityConstants;
import org.gtp.cocacolaproject.websocket.WebSocketConstants;
import org.gtp.cocacolaproject.websocket.WebSocketModel;
import org.gtp.cocacolaproject.websocket.WebSocketModelAdapter;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class RecognizeProductState extends StateImpl {

    private final State state = State.RECOGNIZE_PRODUCT;

    private final Activity mActivity;
    private final Context mContext;
    private final Order mOrder;

    private final WebSocketModel mWebSocketModel;

    private int numberOfTries = 0;

    private TensorFlowModel mTensorFlowModel;

    /**
     * enable tensor flow, wait (max 3x times) for recognition output and confirmation from hunter
     *
     * @param listener listener to report progress
     */
    public RecognizeProductState(WebSocketModel webSocketModel, Activity activity, Context context, Order order, RecognizeProductStateListener listener) {
        super(listener);

        mWebSocketModel = webSocketModel;
        mWebSocketModel.addListener(mWebSocketModelListener);

        mActivity = activity;
        mContext = context;
        mOrder = order;

        if (mTensorFlowModel == null) {
            mTensorFlowModel = new TensorFlowModel(mActivity, mContext,
                    new ImageCropper(MainActivityConstants.type).setScaleAndCropPadding(MainActivityConstants.scaleAndCropPadding));
            ((RecognizeProductStateListener) mListener).setTensorFlowModel(mTensorFlowModel);
        }
        mTensorFlowModel.addListener(mTensorFlowModelListener);
        mTensorFlowModel.onResume();

        onProgress(Transition.INITIAL);
    }


    private final WebSocketModelAdapter mWebSocketModelListener = new WebSocketModelAdapter() {

        @Override
        public void onWebSocketMessage(String message) {
            // No-ops
        }
    };

    private boolean isRunning = false;
    private Timer mRecognitionTimer;
    private final long recognitionTimer = TensorFlowConstants.recognitionDuration; // 4 seconds
    private final long recognitionRate = 30L; // 30 hertz
    private final long recognitionPeriod = 1000 / recognitionRate; // period
    private final int numberOfIterations = (int) (recognitionTimer / recognitionRate) + 1;
    private int recognitionIteration = 0;

    private List<Classifier.Recognition> lastRecognitions;

    @Override
    public void start() {
        onProgress(Transition.RUNNING);

        isRunning = true;
        mTensorFlowModel.enable();

        ((RecognizeProductStateListener) mListener).setRecognitionTimerVisibilityState(true);

    }

    @Override
    public void abort() {

        isRunning = false;

        if (mTensorFlowModel != null) {
            mTensorFlowModel.disable();
            mTensorFlowModel.removeListener(mTensorFlowModelListener);
        }

        if (mRecognitionTimer != null) {
            mRecognitionTimer.cancel();
        }

        if (mWebSocketModel != null) {
            mWebSocketModel.removeListener(mWebSocketModelListener);
        }

        ((RecognizeProductStateListener) mListener).setRecognitionTimerVisibilityState(false);
    }

    @Override
    public void progress(Transition transition, String message) {
        if (transition == Transition.FAILED || transition == Transition.SUCCESSFUL) {
            isRunning = false;
            mTensorFlowModel.disable();
            mTensorFlowModel.removeListener(mTensorFlowModelListener);
            ((RecognizeProductStateListener) mListener).setRecognitionTimerVisibilityState(false);
        }
        mListener.onProgress(state, transition);
    }

    public boolean onPreviewFrame(byte[] bytes, Camera camera) {
        if (isRunning && mTensorFlowModel != null) {
            mTensorFlowModel.process(bytes, camera);
            return true;
        }
        return false;
    }

    private final TensorFlowModelListener mTensorFlowModelListener = new TensorFlowModelListener() {

        @Override
        public void displayCroppedUI(Bitmap rgbFrameBitmap) { }

        @Override
        public void displayRecognitionResults(List<Classifier.Recognition> recognitionList) {
            Log.e(TAG, "displayRecognitionResults::" + recognitionList.size());

            applyConfidenceThreshold(recognitionList, TensorFlowConstants.THRESHOLD);

            if (recognitionList.size() > 0) {


                if (lastRecognitions == null) {
                    lastRecognitions = new ArrayList<Classifier.Recognition>();
                    lastRecognitions.addAll(recognitionList);

                    startRecognitionTimer();
                }
                else {
                    removeThoseWhichAreNotInList(lastRecognitions, recognitionList);
                    updateConfidence(lastRecognitions, recognitionList);

                    if (lastRecognitions.size() == 0) {

                        lastRecognitions = null;

                        resetRecognition();
                    }
                }
            }
            else {
                lastRecognitions = null;

                resetRecognition();
            }
        }
    };

    private void startRecognitionTimer() {

        if (mRecognitionTimer != null) {
            mRecognitionTimer.cancel();
        }

        recognitionIteration = 0;
        mRecognitionTimer = new Timer();
        mRecognitionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                if (lastRecognitions != null && lastRecognitions.size() > 0) {
                    Classifier.Recognition lastRecognition = lastRecognitions.get(0);
                    ((RecognizeProductStateListener) mListener).showRecognition(lastRecognition.getTitle() + ": " + String.format(Locale.UK, "(%.1f%%) ", lastRecognition.getConfidence() * 100.0f) );
                }
                if (recognitionIteration >= numberOfIterations) {

                    successfulRecognition();
                }
                else {
                    recognitionIteration++;
                }
            }
        }, 0L, recognitionPeriod);

        ((RecognizeProductStateListener) mListener).setRecognitionTimerState(true);
    }

    private void resetRecognition() {

        if (mRecognitionTimer != null) {
            mRecognitionTimer.cancel();
        }

        ((RecognizeProductStateListener) mListener).setRecognitionTimerState(false);
    }

    private void successfulRecognition() {

        resetRecognition();

        // is of current Order
        boolean isCurrentOrder = false;

        for (Classifier.Recognition recognition : lastRecognitions) {
            if (ProductRecognitionUtils.equals(recognition, mOrder)) {
                isCurrentOrder = true;
                break;
            }
        }

        if (isCurrentOrder) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(WebSocketConstants.OPERATION_TYPE, WebSocketConstants.OperationType.REPORT_RECOGNITION.shortName());
                jsonObject.put(WebSocketConstants.REPORT_RECOGNITION_RESULT, "Success");
                jsonObject.put(WebSocketConstants.ORDER, new JsonParser().format(mOrder));
                mWebSocketModel.sendJsonObject(jsonObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            ((RecognizeProductStateListener) mListener).showMessageToast("Recognized " + mOrder.getProductDescription());
            onProgress(Transition.SUCCESSFUL);
        }
        else {
            numberOfTries++;
            if (numberOfTries >= TensorFlowConstants.MAX_NUMBER_OF_TRIES) {

                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(WebSocketConstants.OPERATION_TYPE, WebSocketConstants.OperationType.REPORT_RECOGNITION.shortName());
                    jsonObject.put(WebSocketConstants.REPORT_RECOGNITION_RESULT, "Failed");
                    jsonObject.put(WebSocketConstants.ORDER, new JsonParser().format(mOrder));
                    mWebSocketModel.sendJsonObject(jsonObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ((RecognizeProductStateListener) mListener).showMessageToast("Could not recognized " + mOrder.getProductDescription());
                onProgress(Transition.SUCCESSFUL);
            }
            else {
                ((RecognizeProductStateListener) mListener).showMessageToast("Try again");
                resetRecognition();
            }
        }
    }

    private static void applyConfidenceThreshold(List<Classifier.Recognition> recognitionList, double threshold) {
        for (int i = recognitionList.size() - 1 ; i >= 0 ; i--) {
            if (recognitionList.get(i).getConfidence() < threshold) {
                recognitionList.remove(i);
            }
        }
    }

    private static void updateConfidence(List<Classifier.Recognition> lastClassifierRecognitions,
                                         List<Classifier.Recognition> classifierRecognitions) {

        for (int i = 0 ; i < lastClassifierRecognitions.size(); i++) {
            for (Classifier.Recognition r : classifierRecognitions) {
                if (lastClassifierRecognitions.get(i).getTitle().equals(r.getTitle())) {
                    lastClassifierRecognitions.set(i, r);
                }
            }
        }
    }

    private static void removeThoseWhichAreNotInList(List<Classifier.Recognition> lastClassifierRecognitions,
                                                     List<Classifier.Recognition> classifierRecognitions) {

        for (int i = lastClassifierRecognitions.size() - 1 ; i >= 0 ; i--) {
            if (contains(classifierRecognitions, lastClassifierRecognitions.get(i))) {

            }
            else {
                lastClassifierRecognitions.remove(i);
            }
        }
    }

    private static boolean contains(List<Classifier.Recognition> classifierRecognitions,
                                    Classifier.Recognition classifierRecognition) {

        for (Classifier.Recognition r : classifierRecognitions) {
            if (r.getTitle().equals(classifierRecognition.getTitle())) {
                return true;
            }
        }
        return false;
    }
}
