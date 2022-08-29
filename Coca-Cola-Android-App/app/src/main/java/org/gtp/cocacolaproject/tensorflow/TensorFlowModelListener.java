package org.gtp.cocacolaproject.tensorflow;

import android.graphics.Bitmap;

import java.util.List;

public interface TensorFlowModelListener  {

    void displayCroppedUI(Bitmap rgbFrameBitmap);

    void displayRecognitionResults(List<Classifier.Recognition> recognitionList);
}
