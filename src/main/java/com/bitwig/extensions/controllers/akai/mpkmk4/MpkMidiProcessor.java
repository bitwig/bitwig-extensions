package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkDisplayFont;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.ScreenRowState;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.StringUtil;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.Midi;

@Component
public class MpkMidiProcessor {
    
    private static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    private static String DEVICE_RESPONSE_HEADER;
    
    private static String IN_MODE_SYSEX;
    private static String IN_SCREEN_OWNER_SHIP;
    private static String IN_DAW_MODE_SYSEX;
    private static String IN_SCREEN_STATUS;
    
    private static byte productId;
    private static String akaiHeader;
    private static String set_preset_daw;
    private static String set_screen_owner;
    private static String set_screen_fw;
    private static String set_mode_clip;
    private static String set_display_string;
    private static String set_display_color;
    private static String set_display_row;
    private static String set_pad_notes_enabled;
    private static String IN_SYSEX_HEADER;
    private static String disable_pads;
    private static final byte[] ROW_DISPLAY_COLOR = prepareFixedData(0x14, 9);
    private static final byte[] CLEAR_LINE_TEXT = prepareFixedData(0x15, 4);
    
    private final ControllerHost host;
    private final MidiIn dawMidiIn;
    private final MidiOut dawMidiOut;
    private final MidiIn playMidiIn;
    private final GlobalStates globalStates;
    private LineDisplay mainDisplay;
    private final NoteInput noteInput;
    protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    private final List<Runnable> updateListeners = new ArrayList<>();
    private final List<IntConsumer> modeChangeListeners = new ArrayList<>();
    private long lastScreenRequest = System.currentTimeMillis();
    private final List<NoteListener> noteListeners = new ArrayList<>();
    
    @FunctionalInterface
    public interface NoteListener {
        void playNote(int note, int vel);
    }
    
    public void setHeader(final byte productId) {
        MpkMidiProcessor.productId = productId;
        akaiHeader = "F0 47 7F %02X ".formatted(productId);
        set_preset_daw = akaiHeader + "2D 00 00 F7";
        set_screen_owner = akaiHeader + "1C 00 01 02 F7";
        set_screen_fw = akaiHeader + "1C 00 01 00 F7";
        set_mode_clip = akaiHeader + "2A 00 01 01 F7";
        set_display_string = akaiHeader + "10 ";
        set_display_color = akaiHeader + "11 00 02 %02X %02X F7";
        set_display_row = akaiHeader + "14 ";
        set_pad_notes_enabled = akaiHeader + "2E 00 01 %s F7";
        disable_pads = akaiHeader + "2B 00 01 01 F7";
        DEVICE_RESPONSE_HEADER = "f07e7f060247%02x0019".formatted(productId);
        IN_SYSEX_HEADER = "f0477f%02x".formatted(productId);
        IN_MODE_SYSEX = IN_SYSEX_HEADER + "2a00";
        IN_SCREEN_OWNER_SHIP = IN_SYSEX_HEADER + "19000100f7";
        IN_DAW_MODE_SYSEX = IN_SYSEX_HEADER + "190000f7";
        IN_SCREEN_STATUS = IN_SYSEX_HEADER + "190011";
    }
    
    public MpkMidiProcessor(final ControllerHost host, final GlobalStates globalStates) {
        this.host = host;
        this.globalStates = globalStates;
        this.dawMidiIn = host.getMidiInPort(0);
        noteInput = dawMidiIn.createNoteInput("MIDI", "89????", "99????", "A9????");
        noteInput.setShouldConsumeEvents(false);
        this.dawMidiOut = host.getMidiOutPort(0);
        this.playMidiIn = host.getMidiInPort(1);
        switch (globalStates.getVariant()) {
            case MINI -> setHeader((byte) 0x5D);
            case MINI_PLUS -> setHeader((byte) 0x03);
        }
        
        playMidiIn.createNoteInput("IN", "??????");
        this.dawMidiIn.setSysexCallback(this::handleSysEx);
        this.dawMidiIn.setMidiCallback(this::handleMidiIn);
    }
    
    private static byte[] prepareFixedData(final int commandId, final int fixedPayLoad) {
        final byte[] data = new byte[8 + fixedPayLoad];
        data[0] = (byte) 0xF0;
        data[1] = (byte) 0x47;
        data[2] = (byte) 0x7F;
        data[3] = productId;
        data[4] = (byte) commandId;
        data[5] = (byte) 0x00;
        data[6] = (byte) fixedPayLoad;
        data[data.length - 1] = (byte) 0xF7;
        return data;
    }
    
    public byte getProductId() {
        return productId;
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
    
    public void addNoteListener(final NoteListener listener) {
        noteListeners.add(listener);
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
        final long diff = System.currentTimeMillis() - lastScreenRequest;
        if (diff > 5800) {
            dawMidiOut.sendSysex(set_screen_owner);
            lastScreenRequest = now;
        }
        host.scheduleTask(this::handlePing, 50);
    }
    
    public NoteInput getNoteInput() {
        return noteInput;
    }
    
    private void handleSysEx(final String data) {
        if (data.startsWith(DEVICE_RESPONSE_HEADER)) {
            MpkMk4ControllerExtension.println(" Connected !");
            startConnection();
        } else if (data.startsWith(IN_SCREEN_OWNER_SHIP)) {
            //MpkMk4ControllerExtension.println(" IN OWNERSHIP ");
            //dawMidiOut.sendSysex(SET_SCREEN_OWNER);
        } else if (data.startsWith(IN_SCREEN_STATUS)) {
            updateListeners.forEach(l -> l.run());
        } else if (data.startsWith(IN_MODE_SYSEX)) {
            final int padMode = getValue(data, 7);
            modeChangeListeners.forEach(l -> l.accept(padMode));
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
        dawMidiOut.sendSysex(set_pad_notes_enabled.formatted(enabled ? "00" : "01"));
    }
    
    public void addModeChangeListener(final IntConsumer listener) {
        this.modeChangeListeners.add(listener);
    }
    
    public void registerMainDisplay(final LineDisplay mainLineDisplay) {
        this.mainDisplay = mainLineDisplay;
    }
    
    private void startConnection() {
        dawMidiOut.sendSysex(set_preset_daw);
        dawMidiOut.sendSysex(set_mode_clip);
        dawMidiOut.sendSysex(set_screen_owner);
        dawMidiOut.sendSysex(akaiHeader + "2B 00 00 F7");
        dawMidiOut.sendSysex(akaiHeader + "3A 00 00 F7");
        setPadNotesEnabled(false);
        mainDisplay.updateCurrent();
        host.scheduleTask(this::handlePing, 50);
    }
    
    private void handleMidiIn(final int status, final int data1, final int data2) {
        //MpkMk4ControllerExtension.println(" MIDI in %02X %02X %02X", status, data1, data2);
        if (status == (Midi.NOTE_ON | 9) || status == (Midi.NOTE_OFF | 9)) {
            noteListeners.forEach(l -> l.playNote(data1, data2));
        }
    }
    
    public void sendMidi(final int status, final int val1, final int val2) {
        dawMidiOut.sendMidi(status, val1, val2);
    }
    
    public void configureLine(final MpkDisplayFont font, final int lineIndex, final int justification,
        final Color foreGround, final Color background) {
        final String sb = set_display_row + "00 09 " //
            + "%02X ".formatted(font.getValue()) //
            + "%02X ".formatted(lineIndex) //
            + "%02X ".formatted(justification) //
            + "%02X ".formatted(foreGround.getRed255() >> 3) //
            + "%02X ".formatted(foreGround.getGreen255() >> 2) //
            + "%02X ".formatted(foreGround.getBlue255() >> 3) //
            + "%02X ".formatted(background.getRed255() >> 3) //
            + "%02X ".formatted(background.getGreen255() >> 2) //
            + "%02X ".formatted(background.getBlue255() >> 3) //
            + "F7";
        dawMidiOut.sendSysex(sb);
    }
    
    public void setText(final int line, final MpkDisplayFont font, final String text) {
        final StringBuilder sb = new StringBuilder(set_display_string);
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
    }
    
    public void clearLineText(final int lineIndex, final int red, final int green, final int blue) {
    }
    
    public void sendSysEx(final byte[] data) {
        dawMidiOut.sendSysex(data);
    }
    
    public void setDisplayColor(final int line, final int color) {
        dawMidiOut.sendSysex(set_display_color.formatted(line, color));
    }
    
    public void exit() {
        dawMidiOut.sendSysex(set_screen_fw);
        setPadNotesEnabled(true);
    }
    
    private static int getValue(final String data, final int byteOffset) {
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
        final int high = getValue(data, 5);
        final int low = getValue(data, 6);
        return high << 7 | low;
    }
    
}
