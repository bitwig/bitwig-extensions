package com.bitwig.extensions.controllers.novation.launchkey_mk4.display;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.MidiProcessor;
import com.bitwig.extensions.framework.di.Component;

@Component
public class DisplayControl {
    private static final byte[] TEXT_CONFIG_COMMAND =
        {(byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x14, 0x04, 0x00, 0x00, (byte) 0xF7};
    private static String TEXT_COMMAND;
    private final MidiOut midiOut;
    
    public DisplayControl(final MidiProcessor processor) {
        this.midiOut = processor.getMidiOut();
        if (processor.isMiniVersion()) {
            TEXT_CONFIG_COMMAND[5] = 0x13;
        }
        TEXT_COMMAND = processor.getSysexHeader() + " 06 ";
    }
    
    public void showText(final ScreenTarget target, final String t1, final String t2) {
        final byte targetId = target.getId();
        configureDisplay(targetId, Arrangement.TWO_LINES);
        setText(targetId, 0, t1);
        setText(targetId, 1, t2);
        showDisplay(targetId);
    }
    
    public void configureDisplay(final int targetId, final Arrangement arr) {
        TEXT_CONFIG_COMMAND[7] = (byte) targetId;
        TEXT_CONFIG_COMMAND[8] = (byte) (arr.getVal() | 0x60);
        midiOut.sendSysex(TEXT_CONFIG_COMMAND);
    }
    
    public void showDisplay(final int targetId) {
        TEXT_CONFIG_COMMAND[7] = (byte) targetId;
        TEXT_CONFIG_COMMAND[8] = 0x7F;
        midiOut.sendSysex(TEXT_CONFIG_COMMAND);
    }
    
    public void hideDisplay(final int targetId) {
        TEXT_CONFIG_COMMAND[7] = (byte) targetId;
        TEXT_CONFIG_COMMAND[8] = 0;
        midiOut.sendSysex(TEXT_CONFIG_COMMAND);
    }
    
    public void setText(final int target, final int field, final String text) {
        final StringBuilder msg = new StringBuilder(TEXT_COMMAND);
        msg.append("%02X ".formatted(target));
        msg.append("%02X ".formatted(field));
        final String validText = StringUtil.toAsciiDisplay(text, 16);
        for (int i = 0; i < validText.length(); i++) {
            msg.append("%02X ".formatted((int) validText.charAt(i)));
        }
        msg.append("F7");
        midiOut.sendSysex(msg.toString());
    }
}
