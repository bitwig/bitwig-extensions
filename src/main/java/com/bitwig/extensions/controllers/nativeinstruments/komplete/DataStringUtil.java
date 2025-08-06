package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataStringUtil {
    
    private static final double TEN_NS_PER_MINUTE = 60e8;
    
    public static String toString(final byte[] data) {
        final List<String> values = new ArrayList<>();
        for (final byte d : data) {
            values.add("%02x".formatted(d));
        }
        return values.stream().collect(Collectors.joining());
    }
    
    public static byte[] toTempo10nsData(final double tempo) {
        final byte[] data = new byte[5];
        final long nsPerMinute = (long) (TEN_NS_PER_MINUTE / tempo);
        for (int i = 0; i < 5; i++) {
            data[i] = (byte) ((nsPerMinute >> (i * 7)) & 0x7F);
        }
        //KompleteKontrolExtension.println(" TEMPO = %f  %X %s", tempo, nsPerMinute, toString(data));
        return data;
    }
    
    public static double toTempo(final int[] data) {
        long nsPerMinute = 0;
        for (int i = 0; i < 5; i++) {
            nsPerMinute |= (long) data[i] << (i * 7);
        }
        final double bpm = TEN_NS_PER_MINUTE / nsPerMinute;
        return bpm;
    }
}
