package org.gtp.cocacolaproject.utils;

import org.gtp.cocacolaproject.data.Order;
import org.gtp.cocacolaproject.tensorflow.Classifier;

public final class ProductRecognitionUtils {

    private final static String TAG = ProductRecognitionUtils.class.toString();

    /**
     * Ensure this class is only used as a utility.
     */
    private ProductRecognitionUtils() {
        throw new AssertionError();
    }

    public static boolean equals(Classifier.Recognition recognition, Order product) {


        if (recognition == null || product == null) {
            return false;
        }

        if (recognition.getTitle().equals(product.getProductDescription())) {
            return true;
        }

        String recStr = recognition.getTitle();
        String proStr = product.getProductDescription();

        if (isCocaCola(recStr) && isCocaCola(proStr)) {

            if (isCafeina(recStr) && isCafeina(proStr)) {
                return equalsSize(recStr, proStr);
            }

            if (isZero(recStr) && isZero(proStr)) {
                return equalsSize(recStr, proStr);
            }

            if (isLight(recStr) && isLight(proStr)) {
                return equalsSize(recStr, proStr);
            }

            return false;
        }

        if (isFanta(recStr) && isFanta(proStr)) {

            if (isZero(recStr) && isZero(proStr)) {
                return equalsSize(recStr, proStr);
            }

            return equalsSize(recStr, proStr);
        }

        return false;
    }

    private static boolean equalsSize(String recognitionDescription, String productDescription) {
        if (recognitionDescription == null || productDescription == null) {
            return false;
        }

        int recSize = getProductSize(recognitionDescription);
        int proSize = getProductSize(productDescription);

        if (recSize != -1 && proSize != -1) {
            if (recSize == proSize) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCocaCola(String description) {
        if (description == null) {
            return false;
        }
        String cpy = description;
        cpy = cpy.toLowerCase();

        if (cpy.contains("coca cola") ||
                cpy.contains("coca-cola") ||
                cpy.contains("coca") ||
                cpy.contains("coca_cola")) {
            return true;
        }
        return false;
    }

    private static boolean isFanta(String description) {
        if (description == null) {
            return false;
        }
        String cpy = description;
        cpy = cpy.toLowerCase();

        if (cpy.contains("fanta")) {
            return true;
        }
        return false;
    }

    private static boolean isZero(String description) {
        if (description == null) {
            return false;
        }
        String cpy = description;
        cpy = cpy.toLowerCase();

        if (cpy.contains("zero")) {
            return true;
        }
        return false;
    }

    private static boolean isCafeina(String description) {
        if (description == null) {
            return false;
        }
        String cpy = description;
        cpy = cpy.toLowerCase();

        if (cpy.contains("cafeina")) {
            return true;
        }
        return false;
    }

    private static boolean isLight(String description) {
        if (description == null) {
            return false;
        }
        String cpy = description;
        cpy = cpy.toLowerCase();

        if (cpy.contains("light")) {
            return true;
        }
        return false;
    }

    private static int getProductSize(String description) {
        if (description == null) {
            return -1;
        }

        String cpy = description;
        cpy = cpy.toLowerCase();
        cpy = cpy.replaceAll("^\\d+$", "");

        try {
            return Integer.parseInt(cpy.replaceAll("\\D+", ""));
        }
        catch (NumberFormatException ee) {
            return -1;
        }
    }

    public static void main(String... args) {
        System.err.println(getProductSize("coca_cola_1,75L"));
    }
}
