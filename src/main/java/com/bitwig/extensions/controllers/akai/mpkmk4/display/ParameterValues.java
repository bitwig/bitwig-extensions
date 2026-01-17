package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import java.util.Arrays;

import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;

public class ParameterValues {
    
    private final MpkMidiProcessor midiProcessor;
    private final String[] values = new String[8];
    private final String[] names = new String[8];
    private final byte[] data = new byte[136];
    
    public ParameterValues(final MpkMidiProcessor midiProcessor) {
        this.midiProcessor = midiProcessor;
        Arrays.fill(this.data, (byte) 0x20);
        data[0] = (byte) 0xF0;
        data[1] = (byte) 0x47;
        data[2] = (byte) 0x7F;
        data[3] = (byte) 0x5D;
        data[4] = (byte) 0x1D;
        data[5] = (byte) 0x01;
        data[6] = (byte) 0x00;
        data[135] = (byte) 0xF7;
    }
    
    public void setValue(final int index, final String value) {
        values[index] = StringUtil.toAsciiDisplay(value, 8);
        final String strVal = values[index];
        for (int i = 0; i < 8; i++) {
            if (i < strVal.length()) {
                data[index * 8 + 71 + i] = (byte) strVal.charAt(i);
            } else {
                data[index * 8 + 71 + i] = 0x20;
            }
        }
    }
    
    public void setNames(final int index, final String name) {
        final String strVal = StringUtil.toAsciiDisplay(name, 8);
        names[index] = strVal;
        for (int i = 0; i < 8; i++) {
            if (i < strVal.length()) {
                data[index * 8 + 7 + i] = (byte) strVal.charAt(i);
            } else {
                data[index * 8 + 7 + i] = 0x20;
            }
        }
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void update() {
        midiProcessor.sendSysEx(data);
    }
}
