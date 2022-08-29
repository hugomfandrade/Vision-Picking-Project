package org.gtp.cocacolaproject.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;

import org.gtp.cocacolaproject.utils.ImageUtils;

public class ImageCropper {

    private final String TAG = getClass().getSimpleName();

    private static final int DEFAULT_OUT_WIDTH = 100;
    private static final int DEFAULT_OUT_HEIGHT = 100;

    private final CameraScaleType mType;
    private int mScaleAndCropPadding = 0;

    private int mOutWidth = DEFAULT_OUT_WIDTH;
    private int mOutHeight = DEFAULT_OUT_HEIGHT;

    private int[] rgbBytes;
    private int previewWidth = 0;
    private int previewHeight = 0;
    private byte[] lastPreviewFrame;
    private byte[][] yuvBytes = new byte[3][];
    private int yRowStride;

    private Runnable imageConverter;
    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private int sensorOrientation;

    private static final boolean MAINTAIN_ASPECT = true;

    public ImageCropper(CameraScaleType type) {
        mType = type;
    }

    public Bitmap process(Activity activity, final byte[] bytes, final Camera camera) {

        if (bytes == null) {
            return null;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            Camera.Size previewSize = camera.getParameters().getPreviewSize();
            if (rgbBytes == null
                    || previewHeight != previewSize.height
                    || previewWidth != previewSize.width) {
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(activity, new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

        lastPreviewFrame = bytes;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                    }
                };

        try {
            imageConverter.run();
        }
        catch (Exception e) {
            return null;
        }
        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        return croppedBitmap;
    }

    public ImageCropper setScaleAndCropPadding(int padding) {
        mScaleAndCropPadding = padding;

        if (mType == CameraScaleType.scaleAndCrop) {
            rebuildBitmaps();
        }
        return this;
    }

    public ImageCropper setOutDimensions(int width, int height) {
        mOutWidth = width;
        mOutHeight = height;

        rebuildBitmaps();
        return this;
    }

    private void onPreviewSizeChosen(Activity activity, final Size size, final int rotation) {


        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation(activity);
        Log.i(TAG, "Camera orientation relative to screen canvas: " + sensorOrientation);

        rebuildBitmaps();
    }

    private void rebuildBitmaps() {

        if (previewWidth == 0 || previewHeight == 0) {
            Log.i(TAG, "couldn't initialize at size " + previewWidth + "x" + previewHeight);
            return;
        }

        Log.i(TAG, "Initializing at size " + previewWidth + "x" + previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(mOutWidth, mOutHeight, Bitmap.Config.ARGB_8888);

        int croppedWidth = mOutWidth;
        int croppedHeight = mOutHeight;

        if (mType == CameraScaleType.scaleAndCrop) {
            croppedWidth = previewWidth - mScaleAndCropPadding * 2;
            croppedHeight = previewHeight - mScaleAndCropPadding * 2;
        }

        frameToCropTransform = ImageUtils.getTransformationMatrix(
                previewWidth, previewHeight,
                croppedWidth, croppedHeight,
                mOutWidth, mOutHeight,
                sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }

    public Bitmap getOriginalBitmap() {
        return rgbFrameBitmap;
    }

    public Bitmap getCroppedBitmap() {
        return croppedBitmap;
    }

    private int getScreenOrientation(Activity activity) {
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_0:
            default:
                return 0;
        }
    }
}
