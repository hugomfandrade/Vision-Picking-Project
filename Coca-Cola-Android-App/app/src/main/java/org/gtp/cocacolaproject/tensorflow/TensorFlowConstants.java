package org.gtp.cocacolaproject.tensorflow;

import org.gtp.cocacolaproject.camera.PixelRange;

public interface TensorFlowConstants {

    class ModelState {

        final int inputSize;
        final PixelRange pixelRange;
        final String modelFile;
        final String labelFile;
        final String inputName;
        final String outputName;

        public ModelState(int inputSize,
                          PixelRange pixelRange,
                          String modelFile,
                          String labelFile,
                          String inputName,
                          String outputName) {
            this.inputSize = inputSize;
            this.pixelRange = pixelRange;
            this.modelFile = modelFile;
            this.labelFile = labelFile;
            this.inputName = inputName;
            this.outputName = outputName;
        }
    }

    ModelState modelStateV1 = new ModelState(
            197,
            PixelRange.of(0, 1),
            "file:///android_asset/coke__inception.pb",
            "file:///android_asset/coke_labels.txt",
            "input_1",
            "predictions/Softmax"
            );

    ModelState modelStateV2 = new ModelState(
            197,
            PixelRange.of(0, 1),//.addOffset(-0.3255926078171155f, -0.43017545938102325f, -0.5999109157206813f),
            "file:///android_asset/densenet.h5.pb",
            "file:///android_asset/densenet_labels.txt",
            "input_1",
            "output_node0"
            );

    ModelState modelStateV3 = new ModelState(
            197,
            PixelRange.of(0, 1),//.addOffset(-0.3255926078171155f, -0.43017545938102325f, -0.5999109157206813f),
            "file:///android_asset/retrained_mobile.pb",
            "file:///android_asset/retrained_mobile.txt",
            "input:0",
            "final_result"
            );

    ModelState modelStateV4 = new ModelState(
            197,
            PixelRange.of(0, 1),
            "file:///android_asset/all_net.h5.pb",
            "file:///android_asset/all_net.h5.txt",
            "input:0",
            "output_node0"
            );

    ModelState modelStateV5 = new ModelState(
            224,
            PixelRange.of(0, 1),
            "file:///android_asset/mobilenet_v2.h5.pb",
            "file:///android_asset/mobilenet_v2.h5.txt",
            "input_1",
            "output_node0"
            );

    ModelState modelStateV6 = new ModelState(
            224,
            PixelRange.of(0, 1),
            "file:///android_asset/mobilenet_v3.h5.pb",
            "file:///android_asset/mobilenet_v3.h5.txt",
            "input_1",
            "output_node0"
            );

    ModelState selectedModelState = modelStateV6;


    int INPUT_SIZE = selectedModelState.inputSize;
    PixelRange PIXEL_RANGE = selectedModelState.pixelRange;
    String MODEL_FILE = selectedModelState.modelFile;
    String LABEL_FILE = selectedModelState.labelFile;
    String INPUT_NAME = selectedModelState.inputName;
    String OUTPUT_NAME = selectedModelState.outputName;

    long recognitionDuration = 4000L;
    double THRESHOLD = 0.80;
    int MAX_NUMBER_OF_TRIES = 3;
}
