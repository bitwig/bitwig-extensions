package com.bitwig.extensions.controllers.akai.mpkmk4;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.di.Component;

@Component
public class MpkMidiProcessor {
    
    private static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    private static final String DEVICE_RESPONSE_HEADER = "f07e7f0602475d0019";
    private static final String AKAI_HEADER = "F0 47 7F 5D ";
    private static final String SET_SCREEN_OWNER = AKAI_HEADER + "1C ";
    // Message ID
    // Payload Len High
    // Payload Len Low
    // .... Payload
    // F7
    
    private final ControllerHost host;
    private final MidiIn dawMidiIn;
    private final MidiOut dawMidiOut;
    private final MidiIn playMidiIn;
    
    public MpkMidiProcessor(final ControllerHost host) {
        this.host = host;
        this.dawMidiIn = host.getMidiInPort(0);
        this.dawMidiOut = host.getMidiOutPort(0);
        this.playMidiIn = host.getMidiInPort(1);
        
        playMidiIn.createNoteInput("IN", "8?????", "9?????", "A?????", "B?????", "D?????", "E?????");
        this.dawMidiIn.setSysexCallback(this::handleSysEx);
        this.dawMidiIn.setMidiCallback(this::handleMidiIn);
    }
    
    
    public void init() {
        dawMidiOut.sendSysex(DEVICE_INQUIRY);
    }
    
    private void handleSysEx(final String data) {
        if (data.startsWith(DEVICE_RESPONSE_HEADER)) {
            MpkMk4ControllerExtension.println(" Connected !");
        } else {
            MpkMk4ControllerExtension.println(" SYSEX = %s", data);
        }
    }
    
    private void handleMidiIn(final int status, final int data1, final int data2) {
        MpkMk4ControllerExtension.println(" MIDI in %02X %02X %02X", status, data1, data2);
    }
    
    
    public void exit() {}
}
