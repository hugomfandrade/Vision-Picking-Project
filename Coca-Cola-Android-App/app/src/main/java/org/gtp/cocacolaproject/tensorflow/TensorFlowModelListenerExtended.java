package org.gtp.cocacolaproject.tensorflow;

import android.graphics.Bitmap;

import org.gtp.cocacolaproject.utils.BitmapUtils;

import java.util.List;

public interface TensorFlowModelListenerExtended extends TensorFlowModelListener {

    void displayCompleteInfo(Bitmap rgbFrameBitmap, Bitmap croppedBitmap, List<Classifier.Recognition> recognitionList);
}

