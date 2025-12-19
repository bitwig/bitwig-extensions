package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import java.util.Arrays;

public class ParameterValues {
    
    private final String[] values = new String[8];
    private final String[] names = new String[8];
    private final byte[] data = new byte[135];
    
    public ParameterValues() {
        data[0] = (byte) 0xF0;
        data[1] = (byte) 0x47;
        data[2] = (byte) 0x7F;
        data[3] = (byte) 0x5D;
        data[4] = (byte) 0x1D;
        data[5] = (byte) 0x01;
        data[6] = (byte) 0x00;
        data[134] = (byte) 0xF7;
        
        Arrays.fill(this.data, (byte) 0);
    }
    
    private void setValue(final int index, final String value) {
        values[index] = StringUtil.toAsciiDisplay(value, 8);
        for (int i = 0; i < 8; i++) {
            if (i < values[i].length()) {
                data[index * 8 + 64 + i] = (byte) values[i].charAt(i);
            } else {
                data[index * 8 + 64 + i] = 0;
            }
        }
    }
    
    private void setNames(final int index, final String name) {
        names[index] = StringUtil.toAsciiDisplay(name, 8);
        for (int i = 0; i < 8; i++) {
            if (i < values[i].length()) {
                data[index * 8 + 7 + i] = (byte) values[i].charAt(i);
            } else {
                data[index * 8 + 7 + i] = 0;
            }
        }
    }
    
    public byte[] getData() {
        return data;
    }
}
