package com.bitwig.extensions.controllers.nativeinstruments.komplete;

public class KontrolUtil {
    
    public static final byte[] LEVEL_DB_LOOKUP = new byte[201]; // maps level values to align with KK display
    
    static {
        final int[] intervals = {0, 25, 63, 100, 158, 200};
        final int[] pollValues = {0, 14, 38, 67, 108, 127};
        int currentIv = 1;
        int ivLb = 0;
        int plLb = 0;
        int ivUb = intervals[currentIv];
        int plUb = pollValues[currentIv];
        double ratio = (double) (plUb - plLb) / (double) (ivUb - ivLb);
        for (int i = 0; i < LEVEL_DB_LOOKUP.length; i++) {
            if (i > intervals[currentIv]) {
                currentIv++;
                ivLb = ivUb;
                plLb = plUb;
                ivUb = intervals[currentIv];
                plUb = pollValues[currentIv];
                ratio = (double) (plUb - plLb) / (double) (ivUb - ivLb);
            }
            LEVEL_DB_LOOKUP[i] = (byte) Math.round(plLb + (i - ivLb) * ratio);
        }
    }
    
    public static byte toSliderVal(final double value) {
        return LEVEL_DB_LOOKUP[(int) (value * 200)];
    }
    
    public static double roundToNearestPowerOfTwo(final double value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Value must be greater than zero.");
        }
        final double log2 = Math.log(value) / Math.log(2);
        final double roundedPower = Math.round(log2);
        return Math.pow(2, roundedPower);
    }
    
    
}
