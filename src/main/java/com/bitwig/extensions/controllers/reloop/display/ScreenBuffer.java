package com.bitwig.extensions.controllers.reloop.display;

import java.util.Arrays;

import com.bitwig.extension.controller.api.MidiOut;

public class ScreenBuffer {
    private final String[] lastSentLines = new String[4];
    private final String[] pendingLines = new String[4];
    private final MidiOut midiOut;
    
    public ScreenBuffer(final MidiOut midiOut) {
        Arrays.fill(lastSentLines, "");
        Arrays.fill(pendingLines, "");
        this.midiOut = midiOut;
    }
    
    public void showConnected() {
        updateLine(0, " ");
        updateLine(1, "   Bitwig");
        updateLine(2, "   Connected");
        updateLine(3, " ");
    }
    
    public void updateLine(final int row, final String text) {
        this.pendingLines[row] = text;
    }
    
    private void sendLine(final int row, final String text) {
        final StringBuilder sysEx = new StringBuilder("F0 26 4A 16 04 %02X ".formatted(row));
        for (int i = 0; i < 16; i++) {
            if (i < text.length()) {
                sysEx.append("%02X ".formatted(StringUtil.toValue(text.charAt(i))));
            } else {
                sysEx.append("20 ");
            }
        }
        sysEx.append("F7");
        midiOut.sendSysex(sysEx.toString());
        lastSentLines[row] = text;
    }
    
    public void updateDisplay() {
        for (int i = 0; i < pendingLines.length; i++) {
            if (!pendingLines[i].equals(lastSentLines[i])) {
                sendLine(i, pendingLines[i]);
                return;
            }
        }
    }
}
