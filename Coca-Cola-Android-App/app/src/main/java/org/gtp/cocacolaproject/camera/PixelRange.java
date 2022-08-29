package org.gtp.cocacolaproject.camera;

public class PixelRange {

    public enum RGB {
        R,
        G,
        B
    }

    private final static float originalMin = 0f;
    private final static float originalMax = 255f;

    private final float min;
    private final float max;

    private float offsetR = 0;
    private float offsetG = 0;
    private float offsetB = 0;

    public static PixelRange of(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException("max ("
                    + Float.toString(max) +
                    ") is not bigger than min("
                    + Float.toString(min) + ").");
        }

        return new PixelRange(min, max);
    }

    private PixelRange(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public PixelRange addOffset(float offsetR, float offsetB, float offsetG) {
        this.offsetR = offsetR;
        this.offsetB = offsetB;
        this.offsetG = offsetG;
        return this;
    }

    public float putInRange(float val) {
        return (((val - originalMin) * (max - min)) / (originalMax - originalMin)) + min;
    }

    public float putInRangeAndOffset(float val, RGB rgb) {
        float value = putInRange(val);
        if (rgb == RGB.R) {
            value = value + offsetR;
        }
        else if (rgb == RGB.G) {
            value = value + offsetG;
        }
        else if (rgb == RGB.B) {
            value = value + offsetB;
        }
        return value;
    }
}
