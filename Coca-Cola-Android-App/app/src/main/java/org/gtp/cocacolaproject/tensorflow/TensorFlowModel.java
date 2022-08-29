package org.gtp.cocacolaproject.tensorflow;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import org.gtp.cocacolaproject.camera.ImageCropper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class TensorFlowModel {

    private final static String TAG = TensorFlowModel.class.getSimpleName();

    protected static final boolean SAVE_PREVIEW_BITMAP = false;

    private static final boolean MAINTAIN_ASPECT = true;

    private final Activity mActivity;
    private final Context mContext;

    private boolean isProcessingEnabled = false;

    private Classifier classifier;


    private Handler handler;
    private HandlerThread handlerThread;

    private boolean isProcessingFrame = false;
    private long lastProcessingTimeMs;
    private Runnable postInferenceCallback;
    public Bitmap cropCopyBitmap;

    private final ImageCropper mImageCropper;

    private Set<TensorFlowModelListener> mListeners = new HashSet<TensorFlowModelListener>();

    public TensorFlowModel(Activity activity, Context context, ImageCropper imageCropper) {
        mActivity = activity;
        mContext = context;

        mImageCropper = imageCropper
                .setOutDimensions(TensorFlowConstants.INPUT_SIZE, TensorFlowConstants.INPUT_SIZE);
    }

    public void addListener(TensorFlowModelListener listener) {

        mListeners.add(listener);
    }

    public void removeListener(TensorFlowModelListener listener) {

        mListeners.remove(listener);
    }

    public void onResume() {
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public void onPause() {
        handlerThread.quit();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void enable() {
        if (!isProcessingEnabled) {
            isProcessingEnabled = true;
        }
    }

    public void disable() {
        if (isProcessingEnabled) {
            isProcessingEnabled = false;
        }
    }

    public boolean isProcessingEnabled() {
        return isProcessingEnabled;
    }

    public void process(final byte[] bytes, final Camera camera) {

        if (isProcessingFrame) {
            Log.w(TAG, "Dropping frame!");
            return;
        }

        // Initialize the classifier
        if (classifier == null) {
            classifier = TensorFlowImageClassifier.create(
                            mContext.getAssets(),
                            TensorFlowConstants.MODEL_FILE,
                            TensorFlowConstants.LABEL_FILE,
                            TensorFlowConstants.INPUT_SIZE,
                            TensorFlowConstants.PIXEL_RANGE,
                            TensorFlowConstants.INPUT_NAME,
                            TensorFlowConstants.OUTPUT_NAME);
        }

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };

        isProcessingFrame = true;
        if (mImageCropper != null) {
            mImageCropper.process(mActivity, bytes, camera);
        }
        else {
            postInferenceCallback.run();
            return;
        }

        for (TensorFlowModelListener listener : mListeners) {
            listener.displayCroppedUI(mImageCropper.getCroppedBitmap());
        }

        if (!isProcessingEnabled) {
            if (postInferenceCallback != null) {
                postInferenceCallback.run();
            }
            return;
        }

        runInBackground(
                new Runnable() {

                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = classifier.recognizeImage(mImageCropper.getCroppedBitmap());
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        StringBuilder stringBuilder = new StringBuilder("Detect: ");
                        for (Classifier.Recognition recognition : results) {
                            stringBuilder.append(recognition.toString())
                                    .append("\n");
                        }
                        Log.i(TAG, stringBuilder.toString());
                        for (TensorFlowModelListener listener : mListeners) {
                            listener.displayRecognitionResults(results);
                        }
                        for (TensorFlowModelListener listener : mListeners) {
                            if (listener instanceof TensorFlowModelListenerExtended) {
                                ((TensorFlowModelListenerExtended) listener).displayCompleteInfo(mImageCropper.getOriginalBitmap(), mImageCropper.getCroppedBitmap(), results);
                            }
                        }
                        cropCopyBitmap = Bitmap.createBitmap(mImageCropper.getCroppedBitmap());
                        //cropCopyBitmap = Bitmap.createBitmap(croppedCenterBitmap);
                        // Matrix matrix = new Matrix();
                        // matrix.postRotate(-90);
                        // cropCopyBitmap = Bitmap.createBitmap(croppedCenterBitmap, 0, 0, croppedCenterBitmap.getWidth(), croppedCenterBitmap.getHeight(), matrix, true);
                        if (postInferenceCallback != null) {
                            postInferenceCallback.run();
                        }
                    }
                });
    }

    private synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }
}
