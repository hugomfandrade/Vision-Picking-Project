package org.gtp.cocacolaproject.view;

import android.Manifest;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import org.gtp.cocacolaproject.camera.CameraScaleType;
import org.gtp.cocacolaproject.camera.DisplayOrientationDetector;
import org.gtp.cocacolaproject.camera.SurfaceTextureAdapter;
import org.gtp.cocacolaproject.tensorflow.TensorFlowConstants;
import org.gtp.cocacolaproject.ui.view.CameraView;
import org.gtp.cocacolaproject.common.LifeCycleActivity;
import org.gtp.cocacolaproject.R;
import org.gtp.cocacolaproject.camera.Size;
import org.gtp.cocacolaproject.utils.ImageUtils;
import org.gtp.cocacolaproject.utils.PermissionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class CameraActivity extends LifeCycleActivity

        implements Camera.PreviewCallback, Camera.ErrorCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * The camera preview size will be chosen to be the smallest frame by pixel size capable of
     * containing a DESIRED_SIZE x DESIRED_SIZE square.
     */
    private static final int MINIMUM_PREVIEW_SIZE = 320;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    /**
     * An {@link CameraView} for camera preview.
     */
    protected CameraView mCameraView;

    private CameraScaleType mCameraScaleType = CameraScaleType.cropOnly;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;

    private Camera mCamera;

    private Camera.Parameters mCameraParameters;

    protected Size desiredSize = DESIRED_PREVIEW_SIZE;

    private boolean mShowingPreview;

    private DisplayOrientationDetector mDisplayOrientationDetector;

    private int mDisplayOrientation;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDisplayOrientationDetector = new DisplayOrientationDetector(this) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                setDisplayOrientation(displayOrientation);
            }
        };
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        if (findViewById(R.id.camera) != null) {
            mCameraView = (CameraView) findViewById(R.id.camera);
            mCameraView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        else {
            mCameraView = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).

        if (PermissionUtils.hasGrantedPermission(this, Manifest.permission.CAMERA)) {

            openCamera();
            setUpPreview();
            mShowingPreview = true;
            mCamera.startPreview();
        }
        else {
            PermissionUtils.requestPermission(this, Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionUtils.onRequestPermissionsResult(this,
                requestCode,
                permissions,
                grantResults,
                new PermissionUtils.OnRequestPermissionsResultCallback() {
                    @Override
                    public void onRequestPermissionsResult(String permissionType, boolean wasPermissionGranted) {
                        if (Manifest.permission.CAMERA.equals(permissionType)) {
                            if (!wasPermissionGranted) {
                                finish();
                            }
                            else {
                                openCamera();
                                setUpPreview();
                                mShowingPreview = true;
                                mCamera.startPreview();
                            }
                        }
                    }
                });
    }

    @Override
    protected void onPause() {
        stopCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mDisplayOrientationDetector != null) {
            mDisplayOrientationDetector.enable(getWindowManager().getDefaultDisplay());
        }
    }

    @Override
    public void onDetachedFromWindow() {
        if (mDisplayOrientationDetector != null) {
            mDisplayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        backgroundThread.quit();
        try {
            backgroundThread.join();
            backgroundThread = null;
        } catch (final InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void openCamera() {
        // make sure any previous setup is stopped
        stopCamera();

        mCamera = Camera.open(getCameraID());
        mCameraParameters = mCamera.getParameters();

        adjustCameraParameters();

        mCamera.setDisplayOrientation(calcDisplayOrientation(mDisplayOrientation));
    }

    private void adjustCameraParameters() {
        if (mShowingPreview) {
            mCamera.stopPreview();
        }

        List<String> focusModes = mCameraParameters.getSupportedFocusModes();
        if (focusModes != null
                && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        Camera.Size sss = getOptimalPreviewSize();

        Log.i(TAG, "Chosen preview size: " + sss.width + "x" + sss.height);

        mCameraParameters.setPreviewSize(sss.width, sss.height);
        mCameraParameters.setRotation(calcCameraRotation(mDisplayOrientation));
        mCamera.setParameters(mCameraParameters);

        if (mShowingPreview) {
            mCamera.startPreview();
        }

        // mCameraView.setAspectRatio(sss.width, sss.height);
    }

    private void setUpPreview() {
        if (mCameraView != null) {
            if (mCameraView.isAvailable()) {
                try {
                    mCamera.setPreviewTexture(mCameraView.getSurfaceTexture());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void resetCamera() {
        mShowingPreview = false;
        mCamera.stopPreview();
        stopCamera();
        openCamera();
        setUpPreview();
        mShowingPreview = true;
        mCamera.startPreview();
    }

    protected void stopCamera() {
        mShowingPreview = false;
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private int getCameraID() {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                return i;
        }
        return -1; // No camera found
    }

    private void setDisplayOrientation(int displayOrientation) {

        if (mDisplayOrientation == displayOrientation) {
            return;
        }
        mDisplayOrientation = displayOrientation;
        //mDisplayOrientation = 0;

        if (mCamera != null) {
            mCameraParameters.setRotation(calcCameraRotation(displayOrientation));
            mCamera.setParameters(mCameraParameters);
            mCamera.setDisplayOrientation(calcDisplayOrientation(displayOrientation));
        }
        configureTransform();
    }

    void configureTransform() {

        if (mCamera == null || mCamera.getParameters() == null) {
            return;
        }

        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();

        final int width = mCameraView.getMeasuredWidth();
        final int height = mCameraView.getMeasuredHeight();

        float ratioSurface = mCameraView.getMeasuredWidth() > mCameraView.getMeasuredHeight() ? (float) mCameraView.getMeasuredWidth() / mCameraView.getMeasuredHeight() : (float) height / mCameraView.getMeasuredWidth();
        float ratioPreview = (float) previewSize.width / previewSize.height;

        int scaledHeight = 0;
        int scaledWidth = 0;
        float scaleX = 1f;
        float scaleY = 1f;

        boolean isPortrait = !isLandscape(mDisplayOrientation);

        if (isPortrait && ratioPreview > ratioSurface) {
            scaledWidth = width;
            scaledHeight = (int) (((float) previewSize.width / previewSize.height) * width);
            scaleX = 1f;
            scaleY = (float) scaledHeight / height;
        } else if (isPortrait && ratioPreview < ratioSurface) {
            scaledWidth = (int) (height / ((float) previewSize.width / previewSize.height));
            scaledHeight = height;
            scaleX = (float) scaledWidth / width;
            scaleY = 1f;
        } else if (!isPortrait && ratioPreview < ratioSurface) {
            scaledWidth = width;
            scaledHeight = (int) (width / ((float) previewSize.width / previewSize.height));
            scaleX = 1f;
            scaleY = (float) scaledHeight / height;
        } else if (!isPortrait && ratioPreview > ratioSurface) {
            scaledWidth = (int) (((float) previewSize.width / previewSize.height) * width);
            scaledHeight = height;
            scaleX = (float) scaledWidth / width;
            scaleY = 1f;
        }

        Matrix matrix = new Matrix();
        if (mDisplayOrientation % 180 == 90) {
            //if (mDisplayOrientation % 180 != 90) {
            //final int width = getWidth() / 2;
            //final int height = getHeight() / 2;
            // final int width = mCameraView.getMeasuredWidth();
            // final int height = mCameraView.getMeasuredHeight();
            // Rotate the camera preview when the screen is landscape.
            matrix.setPolyToPoly(
                    new float[]{
                            0.f, 0.f, // top left
                            width, 0.f, // top right
                            0.f, height, // bottom left
                            width, height, // bottom right
                    }, 0,
                    mDisplayOrientation == 90 ?
                            // Clockwise
                            new float[]{
                                    0.f, height, // top left
                                    0.f, 0.f, // top right
                                    width, height, // bottom left
                                    width, 0.f, // bottom right
                            } : // mDisplayOrientation == 270
                            // Counter-clockwise
                            new float[]{
                                    width, 0.f, // top left
                                    width, height, // top right
                                    0.f, 0.f, // bottom left
                                    0.f, height, // bottom right
                            }, 0,
                    4);
        } else if (mDisplayOrientation == 180) {
            matrix.postRotate(180, mCameraView.getMeasuredWidth() / 2, mCameraView.getMeasuredHeight() / 2);
            //matrix.postRotate(180, getWidth(), getHeight());
        }
        matrix.setScale(scaleX, scaleY);
        mCameraView.setTransform(matrix);
    }

    @Override
    public abstract void onPreviewFrame(byte[] data, Camera camera);

    /**
     * {@link android.view.TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final SurfaceTextureAdapter mSurfaceTextureListener = new SurfaceTextureAdapter() {

        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture texture,
                                              final int width,
                                              final int height) {
            if (mCamera != null) {
                setUpPreview();
                adjustCameraParameters();
                mCamera.setPreviewCallbackWithBuffer(CameraActivity.this);
                Camera.Size s = mCamera.getParameters().getPreviewSize();
                //mCamera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(s.height, s.width)]);
                if (mDisplayOrientation == 90 || mDisplayOrientation == 270) {
                    mCamera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(s.width, s.height)]);
                    desiredSize = new Size(s.width, s.height);
                }
                else {
                    mCamera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(s.height, s.width)]);
                    desiredSize = new Size(s.height, s.width);
                }
                //Log.e(TAG, "desired size = " + desiredSize.getHeight() + " , " + desiredSize.getWidth()););
            }
        }
    };

    protected Size chooseOptimalSize(final Size[] choices, final int width, final int height) {
        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
        final Size desiredSize = new Size(width, height);

        // Collect the supported resolutions that are at least as big as the preview Surface
        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<Size>();
        final List<Size> tooSmall = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.equals(desiredSize)) {
                // Set the size but don't return yet so that remaining sizes will still be logged.
                exactSizeFound = true;
            }

            if (option.getHeight() >= minSize && option.getWidth() >= minSize) {
                bigEnough.add(option);
            } else {
                tooSmall.add(option);
            }
        }

        final List<Size> pictureSizes = new ArrayList<Size>();
        for (final Camera.Size s : mCamera.getParameters().getSupportedPictureSizes()) {
            pictureSizes.add(new Size(s.width, s.height));
        }

        Log.i(TAG, "Desired size: " + desiredSize + ", min size: " + minSize + "x" + minSize);
        Log.i(TAG, "Valid preview sizes: [" + TextUtils.join(", ", bigEnough) + "]");
        Log.i(TAG, "Picture sizes: [" + TextUtils.join(", ", pictureSizes) + "]");
        Log.i(TAG, "Rejected preview sizes: [" + TextUtils.join(", ", tooSmall) + "]");

        if (exactSizeFound) {
            Log.i(TAG, "Exact size match found.");
            return desiredSize;
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            final Size chosenSize = Collections.min(bigEnough, new Comparator<Size>() {
                @Override
                public int compare(final Size lhs, final Size rhs) {
                    // We cast here to ensure the multiplications won't overflow
                    return Long.signum(
                            (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());

                }
            });
            Log.i(TAG, "Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
            return chosenSize;
        } else {
            Log.i(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    protected void setCameraScaleType(CameraScaleType cameraScaleType) {
        mCameraScaleType = cameraScaleType;
    }

    private Camera.Size getOptimalPreviewSize() {
    // private Camera.Size getOptimalPreviewSize(int w, int h) {
    // private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {

        int viewWidth = mCameraView.getMeasuredWidth();
        int viewHeight = mCameraView.getMeasuredHeight();

        if (isLandscape(mDisplayOrientation)) {
            int c = viewWidth;
            viewWidth = viewHeight;
            viewHeight = c;
        } else {
            viewWidth = viewWidth;
            viewHeight = viewHeight;
        }

        List<Camera.Size> mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();

        final double ASPECT_TOLERANCE = 0.2;
        final double DIFF_TOLERANCE = 0.05;
        double targetRatio = (double) viewHeight / viewWidth;

        if (mSupportedPreviewSizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        //int targetHeight = h;
        int targetHeight = TensorFlowConstants.INPUT_SIZE ;//* 3;// h / 8;

        for (Camera.Size size : mSupportedPreviewSizes) {
            double ratio = (double) size.width / size.height;
            /*Log.e(TAG, "mSupportedPreviewSizes size: " +
                    size.width + "x" + size.height +
                    " (" + ratio + ") " +
                    viewWidth + "x" + viewHeight + " - " +
                    " (" + targetRatio + ") "
            );/**/
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (size.width < targetHeight) continue;
            if (size.height < targetHeight) continue;

            if (mCameraScaleType == CameraScaleType.cropOnly) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
            else if (mCameraScaleType == CameraScaleType.scaleAndCrop) {
                if (optimalSize == null || Math.abs(ratio - ((double) optimalSize.width / optimalSize.height)) > DIFF_TOLERANCE) {
                    /*if (optimalSize != null) {
                        Log.e(TAG, "mSupportedPreviewSizes ratio optimal size: " + (optimalSize.width / optimalSize.height));
                        Log.e(TAG, "mSupportedPreviewSizes ratio optimal size: " + ratio);
                        Log.e(TAG, "mSupportedPreviewSizes ratio optimal size: " + Math.abs(ratio - (optimalSize.width / optimalSize.height)));
                    }/**/
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
            //Log.e(TAG, "mSupportedPreviewSizes optimal size: " + optimalSize.width + "x" + optimalSize.height);
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : mSupportedPreviewSizes) {
                if (size.width < targetHeight) continue;
                if (size.height < targetHeight) continue;
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    //Log.e(TAG, "optimalSize size: " + size.width + "x" + size.height);
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void onError(int error, Camera camera) {
        if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
            resetCamera();
        }
    }

    private int calcDisplayOrientation(int screenOrientationDegrees) {
        Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(getCameraID(), mCameraInfo);
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (mCameraInfo.orientation + screenOrientationDegrees) % 360) % 360;
        } else {  // back-facing
            return (mCameraInfo.orientation - screenOrientationDegrees + 360) % 360;
        }
    }

    private int calcCameraRotation(int screenOrientationDegrees) {
        Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(getCameraID(), mCameraInfo);

        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (mCameraInfo.orientation + screenOrientationDegrees) % 360;
        } else {  // back-facing
            final int landscapeFlip = isLandscape(screenOrientationDegrees) ? 180 : 0;
            return (mCameraInfo.orientation + screenOrientationDegrees + landscapeFlip) % 360;
        }
    }

    private boolean isLandscape(int orientationDegrees) {
        return (orientationDegrees == 90 || orientationDegrees == 270);
    }

    protected boolean isLandscape() {
        return (mDisplayOrientation == 90 || mDisplayOrientation == 270);
    }
}
