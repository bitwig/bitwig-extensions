package com.bitwig.extensions.controllers.novation.launchkey_mk4.display;

import com.bitwig.extensions.controllers.novation.launchkey_mk4.MidiProcessor;
import com.bitwig.extensions.framework.di.Component;

@Component
public class DisplayControl {
    private static final byte[] TEXT_CONFIG_COMMAND =
        {(byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x14, 0x04, 0x00, 0x00, (byte) 0xF7};
    private static final String CONFIG_COMMAND = "F0 00 20 29 02 14 04 %02X %02X F7";
    private static String TEXT_COMMAND;
    private final MidiProcessor midiProcessor;

    public DisplayControl(final MidiProcessor processor) {
        this.midiProcessor = processor;
        if (processor.isMiniVersion()) {
            TEXT_CONFIG_COMMAND[5] = 0x13;
        }
        TEXT_COMMAND = processor.getSysexHeader() + "06 ";
    }

    public void configureDisplay(final int targetId, int config) {
        TEXT_CONFIG_COMMAND[7] = (byte) targetId;
        TEXT_CONFIG_COMMAND[8] = (byte) config;
        midiProcessor.sendSysExBytes(TEXT_CONFIG_COMMAND);
        //midiProcessor.sendSysExString(CONFIG_COMMAND.formatted(targetId, config));
    }

    public void fixDisplayUpdate(int lineIndex, String text) {
        setText(0x20, lineIndex, text);
        configureDisplay(0x21, 0x61);
        setText(0x21, lineIndex, text);
        showDisplay(0x21);
    }


    public void showDisplay(final int targetId) {
        TEXT_CONFIG_COMMAND[7] = (byte) targetId;
        TEXT_CONFIG_COMMAND[8] = 0x7F;
        //midiProcessor.sendSysExString(CONFIG_COMMAND.formatted(targetId, 0x7F));
        midiProcessor.sendSysExBytes(TEXT_CONFIG_COMMAND);
    }

    public void hideDisplay(final int targetId) {
        TEXT_CONFIG_COMMAND[7] = (byte) targetId;
        TEXT_CONFIG_COMMAND[8] = 0;
        //midiProcessor.sendSysExString(CONFIG_COMMAND.formatted(targetId, 0));
        midiProcessor.sendSysExBytes(TEXT_CONFIG_COMMAND);
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
        midiProcessor.sendSysExString(msg.toString());
    }

    public void initTemps() {
        configureDisplay(0x21, 0x61);
        configureDisplay(0x20, 0x61);
        for (int i = 0; i < 16; i++) {
            configureDisplay(0x05 + i, 0x62);
        }
    }

}
