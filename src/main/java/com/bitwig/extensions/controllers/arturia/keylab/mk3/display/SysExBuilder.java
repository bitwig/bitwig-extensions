package com.bitwig.extensions.controllers.arturia.keylab.mk3.display;

import com.bitwig.extensions.controllers.arturia.keylab.mk3.MidiProcessor;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.SysExUtil;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbColor;

public class SysExBuilder {
    private static final String SCREEN_HEADER = MidiProcessor.ARTURIA_HEADER + "00 02 04 %02X ";
    
    private int index = 0;
    private StringBuilder sysEx = new StringBuilder();
    
    public SysExBuilder(final ScreenTarget target) {
        sysEx = new StringBuilder(SCREEN_HEADER.formatted(target.getId()));
    }
    
    public SysExBuilder(final int target) {
        sysEx = new StringBuilder(SCREEN_HEADER.formatted(target));
    }
    
    public void complete() {
        sysEx.append("F7");
    }
    
    public String getData() {
        return sysEx.toString();
    }
    
    public void appendText(final String text) {
        sysEx.append("%02X ".formatted(index++));
        for (int i = 0; i < text.length(); i++) {
            final char c = SysExUtil.convert(text.charAt(i));
            final String hexValue = Integer.toHexString((byte) c);
            sysEx.append(hexValue.length() < 2 ? "0" + hexValue : hexValue);
            sysEx.append(" ");
        }
        sysEx.append("00 ");
    }
    
    public void appendValue(final int value) {
        sysEx.append("%02X %02X 00 ".formatted(index++, value));
    }
    
    public void appendColor(final RgbColor color) {
        sysEx.append("%02X %02X %02X %02X 00 ".formatted(index++, color.getRedValue(), color.getGreenValue(),
            color.getBlueValue()));
    }
    
    public void appendIcon(final IdItem icon) {
        if (icon != null) {
            appendValue(icon.getId());
        } else {
            index++;
        }
    }
}
