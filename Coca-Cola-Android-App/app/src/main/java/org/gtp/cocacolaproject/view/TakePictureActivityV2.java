package org.gtp.cocacolaproject.view;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.gtp.cocacolaproject.R;
import org.gtp.cocacolaproject.camera.CameraScaleType;
import org.gtp.cocacolaproject.camera.ImageCropper;
import org.gtp.cocacolaproject.tensorflow.Classifier;
import org.gtp.cocacolaproject.tensorflow.TensorFlowConstants;
import org.gtp.cocacolaproject.tensorflow.TensorFlowModel;
import org.gtp.cocacolaproject.tensorflow.TensorFlowModelListenerExtended;
import org.gtp.cocacolaproject.ui.view.SelectedRectView;
import org.gtp.cocacolaproject.utils.BitmapUtils;
import org.gtp.cocacolaproject.utils.ImageUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TakePictureActivityV2 extends CameraActivity implements TensorFlowModelListenerExtended {

    private SelectedRectView selectedRectView;
    private ImageView ivDisplayCrop;
    private Button btTakePicture;
    private TextView tvRecognitionMessage;

    private TensorFlowModel mTensorFlowModel;

    private List<Classifier.Recognition> lastRecognitions = new ArrayList<Classifier.Recognition>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Toast.makeText(this, getDeviceName(), Toast.LENGTH_SHORT).show();
        // Log.e(TAG, getDeviceName());

        setCameraScaleType(TakePictureActivityConstants.type);

        initializeUI();

        mTensorFlowModel = new TensorFlowModel(this, this,
                new ImageCropper(TakePictureActivityConstants.type)
                        .setScaleAndCropPadding(TakePictureActivityConstants.scaleAndCropPadding));
        mTensorFlowModel.onResume();
        mTensorFlowModel.addListener(this);

        btTakePicture.setText(mTensorFlowModel.isProcessingEnabled() ? "stop" : "start");
    }

    private void initializeUI() {

        setContentView(R.layout.activity_take_picture_v2);

        tvRecognitionMessage = (TextView) findViewById(R.id.tv_recognition);
        tvRecognitionMessage.setText("-");

        selectedRectView = (SelectedRectView) findViewById(R.id.camera_selected_view);
        selectedRectView.setHeight(BitmapUtils.fromDpToPixel(this, TensorFlowConstants.INPUT_SIZE));
        selectedRectView.setWidth(BitmapUtils.fromDpToPixel(this, TensorFlowConstants.INPUT_SIZE));
        selectedRectView.setStrokeWidth(BitmapUtils.fromDpToPixel(this, 6));
        selectedRectView.setStrokeColor(Color.RED);
        selectedRectView.setVisibility(View.VISIBLE);

        ivDisplayCrop = (ImageView) findViewById(R.id.iv_display);


        btTakePicture = (Button) findViewById(R.id.bt_take_picture);
        btTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTensorFlowModel == null) {
                    return;
                }

                if (mTensorFlowModel.isProcessingEnabled()) {
                    startTensorFlow();
                }
                else {
                    stopTensorFlow();
                }
            }
        });
    }

    @Override
    public synchronized void onPause() {
        /*if (!isFinishing()) {
            Log.d(TAG, "Requesting finish");
            finish();
        }/**/

        super.onPause();
    }

    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        // Log.i(TAG, "onPreviewFrame");

        if (mTensorFlowModel != null) {
            mTensorFlowModel.process(bytes, camera);
        }

        if (selectedRectView != null) {
            int size;

            float rectViewRatio = selectedRectView.getMeasuredHeight() / (float) selectedRectView.getMeasuredWidth();
            float desiredViewRatio = desiredSize.getHeight() / (float) desiredSize.getWidth();
            float heightRatio = selectedRectView.getMeasuredHeight() / (float) desiredSize.getHeight();
            float widthRatio = selectedRectView.getMeasuredWidth() / (float) desiredSize.getWidth();

            if (TakePictureActivityConstants.type == CameraScaleType.cropOnly) {
                if (isLandscape()) {
                    if (desiredViewRatio > rectViewRatio) {
                        size = (int) (TensorFlowConstants.INPUT_SIZE * heightRatio);
                    } else {
                        size = (int) (TensorFlowConstants.INPUT_SIZE * widthRatio);
                    }
                } else {
                    if (desiredViewRatio > rectViewRatio) {
                        size = (int) (TensorFlowConstants.INPUT_SIZE * widthRatio);
                    } else {
                        size = (int) (TensorFlowConstants.INPUT_SIZE * heightRatio);
                    }
                }
            }
            else if (TakePictureActivityConstants.type == CameraScaleType.scaleAndCrop) {
                float height = mCameraView.getMeasuredHeight() - TakePictureActivityConstants.scaleAndCropPadding * 2 * heightRatio;
                float width = mCameraView.getMeasuredWidth() - TakePictureActivityConstants.scaleAndCropPadding * 2 * widthRatio;
                float newSize = Math.min(height, width);
                size = (int) newSize;
            }
            else {
                size = 0;
            }

            //Log.e(TAG, "onPreviewSize : " + mCameraView.getWidth() + "x" + mCameraView.getHeight());
            //Log.e(TAG, "onPreviewSize : " + desiredSize.getWidth() + " , " + desiredSize.getHeight());
            //Log.e(TAG, "onPreviewSize : " + selectedRectView.getMeasuredWidth() + " , " + selectedRectView.getMeasuredHeight());
            //Log.e(TAG, "onPreviewSize : " + size);
            selectedRectView.setHeight(size);
            selectedRectView.setWidth(size);
        }
    }

    @Override
    public void displayCroppedUI(Bitmap rgbFrameBitmap) {
        if (ivDisplayCrop != null) {
            /*Matrix matrix = new Matrix();
            matrix.postRotate(-90);
            Bitmap scaled = Bitmap.createScaledBitmap(
                    rgbFrameBitmap,
                    rgbFrameBitmap.getHeight() / 2,
                    rgbFrameBitmap.getWidth() / 2,
                    true);/**/
            // ivDisplayCrop.setImageBitmap(Bitmap.createBitmap(scaled, 0, 0, scaled.getWidth(), scaled.getHeight(), matrix, true));
            ivDisplayCrop.setImageBitmap(rgbFrameBitmap);
        }
    }

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-hhmmssSSS", Locale.UK);

    @Override
    public void displayCompleteInfo(Bitmap rgbFrameBitmap, Bitmap croppedBitmap, List<Classifier.Recognition> recognitionList) {

        CharSequence filenameSuffix = formatter.format(System.currentTimeMillis());

        ImageUtils.saveBitmap(
                rgbFrameBitmap,
                getExternalFilesDir(null),
                filenameSuffix + "_full_picture" + ".jpg"
        );

        ImageUtils.saveBitmap(
                croppedBitmap,
                getExternalFilesDir(null),
                filenameSuffix + "_picture" + ".jpg"
        );

        writeRecognition(
                recognitionList,
                getExternalFilesDir(null),
                filenameSuffix + "_recognition_result" + ".txt"
        );
    }

    private void startTensorFlow() {

        if (mTensorFlowModel != null) {
            mTensorFlowModel.disable();
        }

        if (btTakePicture != null) {
            btTakePicture.setText("start");
        }

    }

    private void stopTensorFlow() {

        if (mTensorFlowModel != null) {
            mTensorFlowModel.enable();
        }

        if (btTakePicture != null) {
            btTakePicture.setText("stop");
        }

        if (tvRecognitionMessage != null) {
            tvRecognitionMessage.setText("-");
        }
    }

    @Override
    public void displayRecognitionResults(List<Classifier.Recognition> srcList) {
        lastRecognitions.clear();
        lastRecognitions.addAll(srcList);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mTensorFlowModel == null || !mTensorFlowModel.isProcessingEnabled()) {
                    return;
                }

                StringBuilder stringBuilder = new StringBuilder("Detect: ");
                for (Classifier.Recognition recognition : lastRecognitions) {
                    stringBuilder.append(recognition.toString())
                            .append("\n");
                }
                Log.i(TAG, stringBuilder.toString());

                applyConfidenceThreshold(lastRecognitions, TensorFlowConstants.THRESHOLD);

                if (lastRecognitions != null && lastRecognitions.size() > 0) {
                    Classifier.Recognition lastRecognition = lastRecognitions.get(0);
                    String message = lastRecognition.getTitle() + ": " + String.format(Locale.UK, "(%.1f%%) ", lastRecognition.getConfidence() * 100.0f);
                    if (tvRecognitionMessage != null) {
                        tvRecognitionMessage.setText(message);
                    }
                }
                else {
                    if (tvRecognitionMessage != null) {
                        tvRecognitionMessage.setText("-");
                    }
                }
            }
        });
    }

    private static void applyConfidenceThreshold(List<Classifier.Recognition> recognitionList, double threshold) {
        for (int i = recognitionList.size() - 1 ; i >= 0 ; i--) {
            if (recognitionList.get(i).getConfidence() < threshold) {
                recognitionList.remove(i);
            }
        }
    }

    private static void writeRecognition(final List<Classifier.Recognition> recognitionList,
                                         final File filesDir,
                                         final String filename) {
        try {
            File file = new File(filesDir, filename);

            /*File mFile = File.createTempFile(
                    "data",
                    ".txt",
                    mView.get().getActivityContext().getFilesDir());/**/

            BufferedWriter mBufferedWriter = new BufferedWriter(new FileWriter(file, true));

            for (Classifier.Recognition recognition : recognitionList) {
                mBufferedWriter.append(recognition.toString());
                mBufferedWriter.newLine();
            }

            mBufferedWriter.close();

            // Log.e(TakePictureActivityV2.class.getSimpleName(), "Save File = " + file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }

        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
