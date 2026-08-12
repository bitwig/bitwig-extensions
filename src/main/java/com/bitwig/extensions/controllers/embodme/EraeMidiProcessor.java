package com.bitwig.extensions.controllers.embodme;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;

public class EraeMidiProcessor {
    private static final String IDENT_REQUEST = "f0 7e 7f 06 01 f7";
    private static final String IDENT_REPLY = "f07e0106020021500001000256010102f7";
    private static final String ERAE_SYSEX_HEADER = "F0 00 21 50 00 01 00 02 01 01 ";
    private static final String REQUEST_STATE = ERAE_SYSEX_HEADER + "02 03 F7";
    private static final String IN_ERAE_SYSEX_HEADER = "f0002150000300012";
    
    private static final byte[] BUTTON_UPDATE = {
        (byte) 0xF0, 0x00, 0x21, 0x50, 0x00, 0x01, 0x00, 0x02, 0x01, 0x01, 0x02, 0x02, //
        0x1B,// 12: index row*12 + columnn
        0x01, // 13: color style 0x0 const 0x1 blink 0x2 pulse
        0x05, // 14: color
        (byte) 0xF7
    };
    
    private static final byte[] ACTION_BUTTON_UPDATE = {
        (byte) 0xF0, 0x00, 0x21, 0x50, 0x00, 0x01, 0x00, 0x02, 0x01, 0x01, 0x02, 0x01, //
        0x1B,// 12: index row*12 + columnn
        0x01, // 13: color style 0x0 const 0x1 blink 0x2 pulse
        0x05, // 14: color
        (byte) 0xF7
    };
    
    // F0 00 21 50 00 01 00 02 01 01 02 0F [target_id] [value] F7
    private static final byte[] SLIDER_UPDATE = {
        (byte) 0xF0, 0x00, 0x21, 0x50, 0x00, 0x01, 0x00, 0x02, 0x01, 0x01, 0x02, 0x0F,  //
        0x01, // 12: target Id
        0x05, // 13: value
        (byte) 0xF7
    };
    
    private final MidiIn midiInDaw;
    private final MidiOut midiOutDaw;
    private final Erae2ControllerExtension controller;
    
    public EraeMidiProcessor(final ControllerHost host, final Erae2ControllerExtension controller) {
        this.controller = controller;
        midiInDaw = host.getMidiInPort(0);
        midiOutDaw = host.getMidiOutPort(0);
        midiInDaw.createNoteInput("Erae MIDI Port", "??????");
        midiInDaw.setSysexCallback(this::handleSysEx);
        
        final MidiIn noteMidiIn = host.getMidiInPort(1);
        final NoteInput noteInputMpe = noteMidiIn.createNoteInput("Erae MPE Port", "??????");
        noteInputMpe.setUseExpressiveMidi(true, 0, 48);
    }
    
    public void init() {
        midiOutDaw.sendSysex(IDENT_REQUEST);
    }
    
    
    private void handleSysEx(final String fullData) {
        if (fullData.startsWith(IN_ERAE_SYSEX_HEADER)) {
            final String data = fullData.substring(IN_ERAE_SYSEX_HEADER.length() + 1, fullData.length() - 2);
            
            if ("2c".equals(data)) {
                midiOutDaw.sendSysex(REQUEST_STATE);
            } else if (data.startsWith("26")) {
                final int size = Integer.parseInt(data.substring(2, 4), 16);
                final int mode = Integer.parseInt(data.substring(4, 6), 16);
                controller.handleMode(size, mode);
            } else if (data.startsWith("01")) {
                final int buttonIndex = Integer.parseInt(data.substring(2, 4), 16);
                final int value = Integer.parseInt(data.substring(4, 6), 16);
                controller.handleIndexButton(buttonIndex, value > 0);
            } else if (data.startsWith("02")) {
                final int buttonIndex = Integer.parseInt(data.substring(2, 4), 16);
                final int value = Integer.parseInt(data.substring(4, 6), 16);
                final int row = buttonIndex / 12;
                final int col = buttonIndex % 12;
                controller.handleGridButton(row, col, value > 0);
            } else if (data.startsWith("0f")) {
                final int sliderIndex = Integer.parseInt(data.substring(2, 4), 16);
                final int value = Integer.parseInt(data.substring(4, 6), 16);
                controller.handleSlider(sliderIndex, value);
            } else {
                Erae2ControllerExtension.println(" DATA = %s", data);
            }
        } else if (fullData.equals(IDENT_REPLY)) {
            Erae2ControllerExtension.println(" IDENT REPLY");
            midiOutDaw.sendSysex(REQUEST_STATE);
        } else {
            Erae2ControllerExtension.println(" SysEx = %s", fullData);
        }
    }
    
    public void sendGridButtonUpdate(final int row, final int column, final int style, final int color) {
        BUTTON_UPDATE[12] = (byte) ((row * 12 + column) & 0x7F);
        BUTTON_UPDATE[13] = (byte) (style & 0x7F);
        BUTTON_UPDATE[14] = (byte) (color & 0x7F);
        midiOutDaw.sendSysex(BUTTON_UPDATE);
    }
    
    public void sendActionButtonUpdate(final int index, final int style, final int color) {
        ACTION_BUTTON_UPDATE[12] = (byte) (index & 0x7F);
        ACTION_BUTTON_UPDATE[13] = (byte) (style & 0x7F);
        ACTION_BUTTON_UPDATE[14] = (byte) (color & 0x7F);
        midiOutDaw.sendSysex(ACTION_BUTTON_UPDATE);
    }
    
    public void sendSliderUpdate(final int targetId, final int value) {
        SLIDER_UPDATE[12] = (byte) (targetId & 0x7F);
        SLIDER_UPDATE[13] = (byte) (value & 0x7F);
        midiOutDaw.sendSysex(SLIDER_UPDATE);
    }
    
    
}
