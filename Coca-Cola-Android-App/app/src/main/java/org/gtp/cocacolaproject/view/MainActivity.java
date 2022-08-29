package org.gtp.cocacolaproject.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.gtp.cocacolaproject.R;
import org.gtp.cocacolaproject.camera.CameraScaleType;
import org.gtp.cocacolaproject.camera.ImageCropper;
import org.gtp.cocacolaproject.data.Order;
import org.gtp.cocacolaproject.presenter.MainPresenter;
import org.gtp.cocacolaproject.tensorflow.TensorFlowConstants;
import org.gtp.cocacolaproject.ui.anim.ToastAnimation;
import org.gtp.cocacolaproject.ui.view.SelectedRectView;
import org.gtp.cocacolaproject.ui.view.SelectedRectViewAnimation;
import org.gtp.cocacolaproject.utils.BitmapUtils;

public class MainActivity extends CameraActivity

        implements MainActivityListener {

    private final static long TOAST_DURATION = 4000L;
    private final static long TOAST_TRANSITION_DURATION = 300L;

    private View vProgressBar;

    private Button button;

    private SelectedRectView selectedRectView;

    private View tvMessageToastContainer;
    private TextView tvMessageToast;

    private View tvOrderContainer;
    private TextView tvOrder;

    private View tvMessageContainer;
    private TextView tvMessage;

    private View tvRecognitionMessageContainer;
    private TextView tvRecognitionMessage;

    private View ivWebSocketConnectionStatusContainer;
    private ImageView ivWebSocketConnectionStatus;

    private ImageView ivDisplayCrop;

    private ImageCropper mImageCropper;

    private MainPresenter mMainPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mImageCropper = new ImageCropper(MainActivityConstants.type)
                .setScaleAndCropPadding(MainActivityConstants.scaleAndCropPadding)
                .setOutDimensions(TensorFlowConstants.INPUT_SIZE, TensorFlowConstants.INPUT_SIZE);

        setCameraScaleType(MainActivityConstants.type);

        initializeUI();

        mMainPresenter = new MainPresenter();
        mMainPresenter.onCreate(this);
    }

    private void initializeUI() {

        setContentView(R.layout.activity_main_v2);

        vProgressBar = findViewById(R.id.progressBar_waiting);
        vProgressBar.setVisibility(View.INVISIBLE);

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMainPresenter.onButtonClicked();
            }
        });

        selectedRectView = (SelectedRectView) findViewById(R.id.camera_selected_view);
        selectedRectView.setHeight(BitmapUtils.fromDpToPixel(this, TensorFlowConstants.INPUT_SIZE));
        selectedRectView.setWidth(BitmapUtils.fromDpToPixel(this, TensorFlowConstants.INPUT_SIZE));
        selectedRectView.setStrokeWidth(BitmapUtils.fromDpToPixel(this, 6));
        selectedRectView.setStrokeColor(Color.RED);
        selectedRectView.setProgressStrokeWidth(BitmapUtils.fromDpToPixel(this, 6));
        selectedRectView.setProgressStrokeColor(Color.BLUE);
        selectedRectView.setVisibility(View.INVISIBLE);

        tvMessageToastContainer = findViewById(R.id.container_message_toast);
        tvMessageToastContainer.setVisibility(View.INVISIBLE);
        tvMessageToast = (TextView) findViewById(R.id.tv_message_toast);

        tvOrderContainer = findViewById(R.id.container_order);
        tvOrderContainer.setVisibility(View.INVISIBLE);
        tvOrder = (TextView) findViewById(R.id.tv_order);

        tvMessageContainer = findViewById(R.id.container_message);
        tvMessageContainer.setVisibility(View.INVISIBLE);
        tvMessage = (TextView) findViewById(R.id.tv_message);

        tvRecognitionMessageContainer = findViewById(R.id.container_recognition);
        tvRecognitionMessageContainer.setVisibility(View.INVISIBLE);
        tvRecognitionMessage = (TextView) findViewById(R.id.tv_recognition);

        ivWebSocketConnectionStatus = (ImageView) findViewById(R.id.iv_web_socket_connection_status);
        ivWebSocketConnectionStatusContainer = findViewById(R.id.container_web_socket_connection_status);
        ivWebSocketConnectionStatusContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMainPresenter.onCheatButtonClicked();
                //tryToConnect();
            }
        });
        ivWebSocketConnectionStatusContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMainPresenter.onCheatButtonClicked();
                //startClassifier();
                return true;
            }
        });

        ivDisplayCrop = (ImageView) findViewById(R.id.iv_display);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        mMainPresenter.onResume();
    }

    @Override
    public synchronized void onPause() {
        /*if (!isFinishing()) {
            Log.d(TAG, "Requesting finish");
            finish();
        }/**/

        super.onPause();

        mMainPresenter.onPause();
    }

    @Override
    protected void onDestroy() {
        mMainPresenter.onDestroy(isChangingConfigurations());

        super.onDestroy();
    }

    @Override
    public void setRecognitionTimerVisibilityState(final boolean isVisible) {

        runOnUiThread(new Runnable() {
            @SuppressLint("DefaultLocale")
            @Override
            public void run() {
                if (selectedRectView != null) {
                    selectedRectView.setVisibility(isVisible? View.VISIBLE : View.INVISIBLE);
                }
                if (tvRecognitionMessageContainer != null) {
                    tvRecognitionMessageContainer.setVisibility(isVisible? View.VISIBLE : View.INVISIBLE);
                }
                if (tvRecognitionMessageContainer != null) {
                    tvRecognitionMessageContainer.setVisibility(isVisible? View.VISIBLE : View.INVISIBLE);
                }
                if (tvRecognitionMessage != null) {
                    tvRecognitionMessage.setText("-");
                }
            }
        });
    }

    @Override
    public void setRecognitionTimerState(boolean enable) {

        if (enable) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (tvRecognitionMessage != null) {
                        tvRecognitionMessage.setText("-");
                    }

                    SelectedRectViewAnimation anim = new SelectedRectViewAnimation();
                    anim.setDuration(TensorFlowConstants.recognitionDuration);

                    if (selectedRectView != null) {
                        selectedRectView.startAnimation(anim);
                    }
                }
            });

        }
        else {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (selectedRectView != null) {
                        selectedRectView.clearAnimation();
                        selectedRectView.setProgress(0);
                    }
                    if (tvRecognitionMessage != null) {
                        tvRecognitionMessage.setText("-");
                    }
                }
            });
        }
    }

    @Override
    public void showRecognition(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvRecognitionMessage != null) {
                    tvRecognitionMessage.setText(message);
                }
            }
        });
    }


    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (ivDisplayCrop != null) {
            ivDisplayCrop.setImageBitmap(mImageCropper == null? null : mImageCropper.process(this, bytes, camera));
        }

        boolean isConsumed = false;
        if (mMainPresenter != null) {
            isConsumed = mMainPresenter.onPreviewFrame(bytes, camera);
        }
        if (!isConsumed) {
            camera.addCallbackBuffer(bytes);
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
    public void displayToastMessage(final String message) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMessageToastContainer.clearAnimation();

                tvMessageToast.setText(message);

                tvMessageToastContainer.startAnimation(new ToastAnimation.Builder()
                        .setDuration(TOAST_DURATION)
                        .setToOpaqueDuration(TOAST_TRANSITION_DURATION)
                        .setToTransparentDuration(TOAST_TRANSITION_DURATION)
                        .setFillAfter(true)
                        .setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                tvMessageToastContainer.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                tvMessageToastContainer.setVisibility(View.INVISIBLE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        })
                        .create());
            }
        });
    }

    @Override
    public void hideButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (button != null) {
                    button.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void showOrder(final Order order) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvOrderContainer != null) {
                    tvOrderContainer.setVisibility(View.VISIBLE);
                }
                if (tvOrder != null) {
                    tvOrder.setText(order == null ? "unknown" : order.getProductDescription());
                }
            }
        });
    }

    @Override
    public void hideOrder() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvOrderContainer != null) {
                    tvOrderContainer.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void showButton(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (button != null) {
                    button.setVisibility(View.VISIBLE);
                    button.setText(text);
                }
            }
        });
    }

    @Override
    public void displayMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (tvMessageContainer != null) {
                    tvMessageContainer.setVisibility(View.VISIBLE);
                }
                if (tvMessage != null) {
                    tvMessage.setText(message);
                }
            }
        });
    }

    @Override
    public void hideMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvMessageContainer != null) {
                    tvMessageContainer.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public Activity getActivity() {
        return this;
    }
}
