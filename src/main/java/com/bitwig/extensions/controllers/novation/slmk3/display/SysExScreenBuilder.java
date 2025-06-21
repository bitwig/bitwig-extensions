package com.bitwig.extensions.controllers.novation.slmk3.display;

import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.slmk3.StringUtil;

public class SysExScreenBuilder {
    
    private final StringBuilder sb;
    private int data = 0;
    
    public SysExScreenBuilder() {
        sb = new StringBuilder(MidiProcessor.PROPERTY_SET_HEADER);
    }
    
    private void appendText(final StringBuilder sb, final String text, final int maxLen) {
        data++;
        final String validText = StringUtil.toAsciiDisplay(text, maxLen);
        for (int i = 0; i < validText.length(); i++) {
            sb.append("%02X ".formatted((int) validText.charAt(i)));
        }
        sb.append("00 ");
    }
    
    public SysExScreenBuilder appendText(final int columnIndex, final int objectIndex, final String text) {
        data++;
        sb.append("%02X %02X %02X ".formatted(columnIndex, 1, objectIndex));
        appendText(sb, text, 9);
        return this;
    }
    
    public SysExScreenBuilder appendColor(final int columnIndex, final int objectIndex, final int color) {
        data++;
        sb.append("%02X %02X %02X %02X ".formatted(columnIndex, 2, objectIndex, color));
        return this;
    }
    
    public SysExScreenBuilder appendValue(final int columnIndex, final int objectIndex, final int value) {
        data++;
        sb.append("%02X %02X %02X %02X ".formatted(columnIndex, 3, objectIndex, value));
        return this;
    }
    
    public SysExScreenBuilder appendRgb(final int columnIndex, final int objectIndex, final SlRgbState color) {
        data++;
        sb.append(
            "%02X %02X %02X %02X %02X %02X ".formatted(columnIndex, 4, objectIndex, color.getRed(), color.getGreen(),
                color.getBlue()));
        return this;
    }
    
    public boolean hasData() {
        return data > 0;
    }
    
    public void complete() {
        sb.append("F7");
    }
    
    public String getString() {
        return sb.toString();
    }
    
}
