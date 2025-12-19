package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.controllers.akai.apc64.StringUtil;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkDisplayFont;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.ParameterValues;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.ScreenRowState;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.time.TimedEvent;

@Component
public class MpkMidiProcessor {
    
    private static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    private static final String DEVICE_RESPONSE_HEADER = "f07e7f0602475d0019";
    
    private static final String IN_SYSEX_HEADER = "f0477f5d";
    
    private static final String IN_MODE_SYSEX = IN_SYSEX_HEADER + "2a00";
    private static final String IN_SCREEN_OWNER_SHIP = IN_SYSEX_HEADER + "19000100f7";
    private static final String IN_DAW_MODE_SYSEX = IN_SYSEX_HEADER + "190000f7";
    private static final String IN_SCREEN_STATUS = IN_SYSEX_HEADER + "190011";
    // SYSEX = f0477f5d2d000100f7
    // SYSEX = f0477f5d2b000101f7
    
    
    private static final String AKAI_HEADER = "F0 47 7F 5D ";
    private static final String SET_PRESET_DAW = AKAI_HEADER + "2D 00 00 F7";
    private static final String SET_SCREEN_OWNER = AKAI_HEADER + "1C 00 01 02 F7";
    private static final String SET_SCREEN_FW = AKAI_HEADER + "1C 00 01 00 F7";
    private static final String SET_MODE_CLIP = AKAI_HEADER + "2A 00 01 01 F7";
    private static final String SET_DISPLAY_STRING = AKAI_HEADER + "10 ";
    private static final String SET_DISPLAY_COLOR = AKAI_HEADER + "11 00 02 %02X %02X F7";
    private static final String SET_PAD_NOTES_ENABLED = AKAI_HEADER + "2E 00 01 %s F7";
    private static final String DISABLE_PADS = AKAI_HEADER + "2B 00 01 01 F7";
    private static final byte[] ROW_DISPLAY_COLOR = prepareFixedData(0x14, 9);
    private static final byte[] CLEAR_LINE_TEXT = prepareFixedData(0x15, 4);
    // Message ID
    // Payload Len High
    // Payload Len Low
    // .... Payload
    // F7
    
    private final ControllerHost host;
    private final MidiIn dawMidiIn;
    private final MidiOut dawMidiOut;
    private final MidiIn playMidiIn;
    private final GlobalStates globalStates;
    private final NoteInput playNoteInput;
    private final MidiOut playMidiOut;
    protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    private final List<Runnable> updateListeners = new ArrayList<>();
    private long lastScreenRequest = System.currentTimeMillis();
    
    public MpkMidiProcessor(final ControllerHost host, final GlobalStates globalStates) {
        this.host = host;
        this.globalStates = globalStates;
        this.dawMidiIn = host.getMidiInPort(0);
        this.dawMidiOut = host.getMidiOutPort(0);
        this.playMidiIn = host.getMidiInPort(1);
        
        playNoteInput = playMidiIn.createNoteInput("IN", "??????");
        playMidiOut = host.getMidiOutPort(1);
        this.dawMidiIn.setSysexCallback(this::handleSysEx);
        this.dawMidiIn.setMidiCallback(this::handleMidiIn);
    }
    
    private static byte[] prepareFixedData(final int commandId, final int fixedPayLoad) {
        final byte[] data = new byte[8 + fixedPayLoad];
        data[0] = (byte) 0xF0;
        data[1] = (byte) 0x47;
        data[2] = (byte) 0x7F;
        data[3] = (byte) 0x5D;
        data[4] = (byte) commandId;
        data[5] = (byte) 0x00;
        data[6] = (byte) fixedPayLoad;
        data[data.length - 1] = (byte) 0xF7;
        return data;
    }
    
    
    public void init() {
        dawMidiOut.sendSysex(DEVICE_INQUIRY);
    }
    
    public MidiIn getDawMidiIn() {
        return dawMidiIn;
    }
    
    public void queueEvent(final TimedEvent event) {
        timedEvents.add(event);
    }
    
    public void addUpdateListeners(final Runnable updateListener) {
        this.updateListeners.add(updateListener);
    }
    
    private void handlePing() {
        final long now = System.currentTimeMillis();
        if (!timedEvents.isEmpty()) {
            for (final TimedEvent event : timedEvents) {
                event.process();
                if (event.isCompleted()) {
                    timedEvents.remove(event);
                }
            }
        }
        final long diff = System.currentTimeMillis();
        if (diff > 1800) {
            dawMidiOut.sendSysex(SET_SCREEN_OWNER);
            lastScreenRequest = now;
        }
        host.scheduleTask(this::handlePing, 50);
    }
    
    private void handleSysEx(final String data) {
        if (data.startsWith(DEVICE_RESPONSE_HEADER)) {
            MpkMk4ControllerExtension.println(" Connected !");
            startConnection();
        } else if (data.startsWith(IN_SCREEN_OWNER_SHIP)) {
            MpkMk4ControllerExtension.println(" IN OWNERSHIP ");
            dawMidiOut.sendSysex(SET_SCREEN_OWNER);
        } else if (data.startsWith(IN_SCREEN_STATUS)) {
            // MpkMk4ControllerExtension.println(" IN: Confirm Screen Ownership " + data);
            updateListeners.forEach(l -> l.run());
        } else if (data.startsWith(IN_MODE_SYSEX)) {
            final int padMode = getValue(data, 7);
            MpkMk4ControllerExtension.println(" MODE = %d", padMode);
        } else if (data.startsWith(IN_DAW_MODE_SYSEX)) {
            //            final int modeValue = getValue(data, 6);
            //            if (modeValue == 0) {
            //                updateListeners.forEach(l -> l.run());
            //            }
        } else if (data.startsWith(IN_SYSEX_HEADER)) {
            final int command = getValue(data, 4);
            final int payload = getPayload(data);
            final int value = getValue(data, 7);
            MpkMk4ControllerExtension.println(" IN Command = %02X PL=%d v=%d", command, payload, value);
        } else {
            MpkMk4ControllerExtension.println(" SYSEX = %s", data);
        }
    }
    
    public void setPadNotesEnabled(final boolean enabled) {
        dawMidiOut.sendSysex(SET_PAD_NOTES_ENABLED.formatted(enabled ? "00" : "01"));
    }
    
    private void startConnection() {
        dawMidiOut.sendSysex(SET_PRESET_DAW);
        dawMidiOut.sendSysex(SET_MODE_CLIP);
        dawMidiOut.sendSysex(SET_SCREEN_OWNER);
        dawMidiOut.sendSysex(AKAI_HEADER + "2B 00 00 F7");
        dawMidiOut.sendSysex(AKAI_HEADER + "3A 00 00 F7");
        setPadNotesEnabled(false);
        host.scheduleTask(this::handlePing, 50);
    }
    
    private void handleMidiIn(final int status, final int data1, final int data2) {
        MpkMk4ControllerExtension.println(" MIDI in %02X %02X %02X", status, data1, data2);
    }
    
    public void sendMidi(final int status, final int val1, final int val2) {
        dawMidiOut.sendMidi(status, val1, val2);
    }
    
    public void setText(final MpkDisplayFont font, final int line, final String text) {
        final StringBuilder sb = new StringBuilder(SET_DISPLAY_STRING);
        final String asciiText = StringUtil.toAsciiDisplay(text, 31);
        sb.append("%02X ".formatted(0));
        final int length = asciiText.length();
        sb.append("%02X ".formatted(length + 3));
        sb.append("%02X ".formatted(font.getValue()));
        sb.append("%02X ".formatted(line));
        for (int i = 0; i < length; i++) {
            sb.append("%02X ".formatted((int) asciiText.charAt(i)));
        }
        sb.append("00 ");
        sb.append("F7");
        dawMidiOut.sendSysex(sb.toString());
    }
    
    public void setRowDisplayColor(final ScreenRowState rowState) {
        // ROW_DISPLAY_COLOR
        // Screen State
    }
    
    public void clearLineText(final int lineIndex, final int red, final int green, final int blue) {
    
    }
    
    public void updateParameterValues(final ParameterValues parameterValues) {
        dawMidiOut.sendSysex(parameterValues.getData());
    }
    
    public void setDisplayColor(final int line, final int color) {
        dawMidiOut.sendSysex(SET_DISPLAY_COLOR.formatted(line, color));
    }
    
    public void exit() {
        dawMidiOut.sendSysex(SET_SCREEN_FW);
        setPadNotesEnabled(true);
    }
    
    private static int getValue(final String data, final int byteOffset) {
        //MpkMk4ControllerExtension.println(" ==> = %s", data);
        final int stringOffset = byteOffset * 2;
        if (stringOffset < data.length()) {
            final String stringValue = data.substring(stringOffset, stringOffset + 2);
            if ("F7".equals(stringValue)) {
                return -1;
            }
            return Integer.parseInt(stringValue, 16);
        }
        return -1;
    }
    
    private static int getPayload(final String data) {
        //MpkMk4ControllerExtension.println(" ==> = %s", data);
        final int high = getValue(data, 5);
        final int low = getValue(data, 6);
        return high << 7 | low;
    }
}
