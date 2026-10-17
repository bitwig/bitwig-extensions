package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.bitwig.extensions.controllers.akai.mpkmk4.display.StringUtil;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.Midi;

@Component
public class MpkMidiProcessor {
    
    private static final byte[] DEVICE_INQUIRY = {(byte) 0xF0, 0x7E, 0x7F, 0x06, 0x01, (byte) 0xF7};
    // Product ID 03 hard coded, payload is ASCII "120.00"
    private static final byte[] HELLO_MESSAGE = {
        (byte) 0xF0, 0x47, 0x00, 0x03, 0x49, 0x00, 0x07, 0x31, 0x32, 0x30, 0x2E, 0x30, 0x30, 0x00, (byte) 0xF7
    };
    
    private static final int CMD_SCREEN_STATUS_X19 = 0x19;
    private static final int CMD_PAD_MODE_X2A = 0x2A;
    private static final int CMD_PLUGIN_DAW_X2D = 0x2D;
    
    private final ControllerHost host;
    private final MidiIn dawMidiIn;
    private final MidiOut dawMidiOut;
    private final MidiIn playMidiIn;
    private final GlobalStates globalStates;
    private final MpkSysExCommands cmds;
    private LineDisplay mainDisplay;
    private final NoteInput noteInput;
    private final List<Runnable> updateListeners = new ArrayList<>();
    private final List<IntConsumer> modeChangeListeners = new ArrayList<>();
    private long lastScreenRequest = System.currentTimeMillis();
    private final List<NoteListener> noteListeners = new ArrayList<>();
    private final LineState[] lineStates = new LineState[4];
    protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    
    public MpkMidiProcessor(final ControllerHost host, final GlobalStates globalStates) {
        this.host = host;
        this.globalStates = globalStates;
        this.cmds = globalStates.getVariant().buildCommands();  // ← single clean line
        for (int i = 0; i < lineStates.length; i++) {
            lineStates[i] = new LineState(i);
        }
        this.dawMidiIn = host.getMidiInPort(0);
        noteInput = dawMidiIn.createNoteInput("MIDI", "89????", "99????", "A9????");
        noteInput.setShouldConsumeEvents(false);
        this.dawMidiOut = host.getMidiOutPort(0);
        this.playMidiIn = host.getMidiInPort(1);
        
        playMidiIn.createNoteInput("IN", "??????");
        this.dawMidiIn.setSysexCallback(this::handleSysEx);
        this.dawMidiIn.setMidiCallback(this::handleMidiIn);
    }
    
    private class LineState {
        private String value;
        private String tempValue;
        private int tempColor;
        
        private MpkDisplayFont font;
        private final int index;
        public int color;
        public long overrideTime;
        
        public LineState(int index) {
            this.index = index;
        }
        
        public void restore() {
            sendText(index, font, value);
            setDisplayColor(index, color);
            resetState();
        }
        
        
        public void resetState() {
            overrideTime = -1L;
            this.tempValue = "";
            this.tempColor = -1;
        }
        
        public int getTempColor() {
            return tempColor;
        }
        
        public void setTempColor(final int tempColor) {
            this.tempColor = tempColor;
        }
        
        public String getTempValue() {
            return tempValue;
        }
        
        public void setTempValue(String value) {
            this.tempValue = value;
        }
        
        public boolean tempOverride() {
            boolean isOverridden = overrideTime != -1;
            overrideTime = System.currentTimeMillis();
            return isOverridden;
        }
        
        public boolean needsRestore(long timeNow) {
            return overrideTime != -1 && (timeNow - overrideTime) > 1000;
        }
        
    }
    
    @FunctionalInterface
    public interface NoteListener {
        void playNote(int note, int vel);
    }
    
    private byte[] prepareFixedData(final int commandId, final int fixedPayLoad) {
        final byte[] data = new byte[8 + fixedPayLoad];
        data[0] = (byte) 0xF0;
        data[1] = (byte) 0x47;
        data[2] = (byte) 0x7F;
        data[3] = this.cmds.productId;
        data[4] = (byte) commandId;
        data[5] = (byte) 0x00;
        data[6] = (byte) fixedPayLoad;
        data[data.length - 1] = (byte) 0xF7;
        return data;
    }
    
    public byte getProductId() {
        return cmds.productId;
    }
    
    public void init() {
        sendSysEx(DEVICE_INQUIRY);
    }
    
    /**
     * Single exit point for all SysEx sent to the device. Block or log here.
     */
    public void sendSysEx(final byte[] data) {
        dawMidiOut.sendSysex(data);
        //        if (globalStates.getMpkBaseMode().get() == MpkBaseMode.DAW) {
        //        }
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
        if (diff > 5800 && globalStates.getMpkBaseMode().get() == MpkBaseMode.DAW) {
            sendSysEx(cmds.setScreenOwner);
            lastScreenRequest = now;
        }
        if (lineStates[0].needsRestore(now)) {
            lineStates[0].restore();
            lineStates[1].restore();
            lineStates[2].restore();
        }
        host.scheduleTask(this::handlePing, 50);
    }
    
    public NoteInput getNoteInput() {
        return noteInput;
    }
    
    private void handleSysEx(final String data) {
        final SysExMessage msg = SysExMessage.parse(data);
        if (msg.isDeviceInquiryResponse(cmds.productId)) {
            MpkMk4ControllerExtension.println(" Connected !");
            startConnection();
            return;
        }
        if (!msg.isAkai(cmds.productId)) {
            MpkMk4ControllerExtension.println("  Non Akai SYSEX = %s", msg);
            return;
        }
        
        switch (msg.command()) {
            case CMD_SCREEN_STATUS_X19 -> handleScreenMessage(msg);
            
            // TODO Restore MINI iv 0x2B command
            case CMD_PAD_MODE_X2A -> {
                final int padMode = msg.payload(0);  // TODO We need to Enum this
                modeChangeListeners.forEach(l -> l.accept(padMode));
            }
            case CMD_PLUGIN_DAW_X2D -> {
                final MpkBaseMode value = msg.payload(0) == 0 ? MpkBaseMode.DAW : MpkBaseMode.PLUGIN;
                globalStates.getMpkBaseMode().set(value);
                lastScreenRequest = 0;
            }
            default -> {
                MpkMk4ControllerExtension.println("  Not Handled SYSEX cmd=%02X %s", msg.command(), msg);
            }
        }
    }
    
    private void handleScreenMessage(final SysExMessage msg) {
        if (msg.payloadLength() == 0) {
            if (globalStates.getMpkBaseMode().get() == MpkBaseMode.DAW) {
                sendSysEx(cmds.setScreenOwner);
            }
        }
    }
    
    public void setPadMode(int mode) {
        sendSysEx(cmds.setPadMode(mode));
    }
    
    public void addModeChangeListener(final IntConsumer listener) {
        this.modeChangeListeners.add(listener);
    }
    
    public void registerMainDisplay(final LineDisplay mainLineDisplay) {
        this.mainDisplay = mainLineDisplay;
    }
    
    private void startConnection() {
        sendSysEx(cmds.setScreenOwner);
        sendSysEx(HELLO_MESSAGE);
        setPadMode(01);
        sendSysEx(cmds.command(MpkSysExCommands.CMD_PADS_DISABLED));
        // Request MODE
        sendSysEx(cmds.command(MpkSysExCommands.CMD_PLUGIN_DAW));
        sendSysEx(cmds.command(0x3A));
        setPadNotesEnabled(false);
        mainDisplay.updateCurrent();
        host.scheduleTask(this::handlePing, 50);
    }
    
    private void handleMidiIn(final int status, final int data1, final int data2) {
        MpkMk4ControllerExtension.println(" MIDI in %02X %02X %02X", status, data1, data2);
        if (status == (Midi.NOTE_ON | 9) || status == (Midi.NOTE_OFF | 9)) {
            noteListeners.forEach(l -> l.playNote(data1, data2));
        }
    }
    
    public void sendMidi(final int status, final int val1, final int val2) {
        dawMidiOut.sendMidi(status, val1, val2);
    }
    
    public void configureLine(final MpkDisplayFont font, final int lineIndex, final int justification,
        final Color foreGround, final Color background) {
        sendSysEx(cmds.configureLine(font, lineIndex, justification, foreGround, background));
    }
    
    public void setText(final int line, final MpkDisplayFont font, final String text) {
        lineStates[line].font = font;
        lineStates[line].value = text;
        lineStates[line].resetState();
        sendText(line, font, text);
    }
    
    private void sendText(final int line, final MpkDisplayFont font, final String text) {
        sendSysEx(cmds.setDisplayString(line, font, StringUtil.toAsciiDisplay(text, 31)));
    }
    
    private static byte[] hexToBytes(final String hex) {
        final int len = hex.length();
        final byte[] buffer = new byte[len / 2 + 1];
        int count = 0;
        int i = 0;
        while (i + 1 < len) {
            final int hi = Character.digit(hex.charAt(i), 16);
            if (hi < 0) { // skip spaces
                i++;
                continue;
            }
            final int lo = Character.digit(hex.charAt(i + 1), 16);
            buffer[count++] = (byte) (hi << 4 | lo);
            i += 2;
        }
        return Arrays.copyOf(buffer, count);
    }
    
    public void setDisplayColor(final int line, final int color) {
        lineStates[line].color = color;
        sendSysEx(cmds.setDisplayColor(line, color));
    }
    
    public void temporaryUpdate(final int lineIndex, final String value, final int colorIndex) {
        LineState lineState = lineStates[lineIndex];
        final boolean isOverridden = lineState.tempOverride();
        if (!lineState.getTempValue().equals(value) || !isOverridden) {
            sendText(lineIndex, MpkDisplayFont.PT24, value);
            lineState.setTempValue(value);
        }
        if (lineState.getTempColor() != colorIndex || !isOverridden) {
            sendSysEx(cmds.setDisplayColor(lineIndex, colorIndex));
            lineState.setTempColor(colorIndex);
        }
    }
    
    public void resetTemps(int index) {
        for (int i = 0; i < lineStates.length; i++) {
            if (i != index) {
                lineStates[i].resetState();
            }
        }
    }
    
    public void exit() {
        sendSysEx(cmds.setScreenFw);
        setPadNotesEnabled(true);
    }
    
    public void setPadNotesEnabled(final boolean enabled) {
        sendSysEx(cmds.setPadNotesEnabled(enabled));
    }
    
    private static int getPayload(final String data) {
        final int high = getValue(data, 5);
        final int low = getValue(data, 6);
        return high << 7 | low;
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
    
}
