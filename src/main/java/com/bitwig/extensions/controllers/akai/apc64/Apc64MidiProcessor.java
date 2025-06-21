package com.bitwig.extensions.controllers.akai.apc64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.controllers.akai.apc.common.MidiProcessor;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class Apc64MidiProcessor implements MidiProcessor {
    private static final String MODE_CHANGE_MSG = "f0470053190001";
    
    private static final String DEVICE_VALUE = "f07e00060247530019010";
    
    //2F0 47 00 53 19 00 01 02 F7
    public static final String PRINT_TO_CLIP_HEAD = "f0470053200002";
    public static final String PRINT_TO_CLIP_TAIL = "f0470053220000f7";
    public static final String PRINT_TO_CLIP_BODY = "f047005321";
    private static final String TEXT_PREFIX = "F0 47 00 53 10 00 ";
    protected final MidiIn midiIn;
    protected final MidiOut midiOut;
    protected final NoteInput noteInput;
    protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    protected final ControllerHost host;
    protected List<Consumer<PadMode>> modeChangeListeners = new ArrayList<>();
    private final int[] noteState = new int[128];
    private final int[] noteValueState = new int[128];
    private HardwareElements hwElements;
    private final BooleanValueObject shiftMode;
    private final BooleanValueObject clearMode;
    private Consumer<PrintToClipSeq> printToClipSeqConsumer;
    private PrintToClipSeq currentPrintToClip;
    private boolean sessionModeState = false;
    private boolean initState = true;
    private PadMode currentMode = PadMode.SESSION;
    
    public Apc64MidiProcessor(final ControllerHost host, final MidiIn midiIn, final MidiOut midiOut,
        final ModifierStates modifierStates) {
        this.host = host;
        this.midiIn = midiIn;
        this.midiOut = midiOut;
        noteInput = midiIn.createNoteInput("MIDI", "86????", "96????", "A?????", "D?????");
        setupNoteInput();
        Arrays.fill(noteState, 0);
        Arrays.fill(noteValueState, 0);
        this.shiftMode = modifierStates.getShiftActive();
        this.clearMode = modifierStates.getClearActive();
        midiIn.setMidiCallback(this::handleMidiIn);
        midiIn.setSysexCallback(this::handleSysEx);
    }
    
    private void setupNoteInput() {
        noteInput.setShouldConsumeEvents(true);
        final Integer[] noAssignTable = new Integer[128];
        Arrays.fill(noAssignTable, Integer.valueOf(-1));
        noteInput.setKeyTranslationTable(noAssignTable);
    }
    
    @Override
    public NoteInput createNoteInput(final String name, final String... mask) {
        return midiIn.createNoteInput(name, mask);
    }
    
    @Override
    public void sendMidi(final int status, final int val1, final int val2) {
        midiOut.sendMidi(status, val1, val2);
        noteState[val1] = status & 0xF;
        noteValueState[val1] = val2;
    }
    
    public void setHwElements(final HardwareElements elements) {
        this.hwElements = elements;
    }
    
    public void restoreState() {
        if (hwElements == null) {
            return;
        }
        hwElements.invokeRefresh();
        //        for (int i = 0; i < noteState.length; i++) {
        //            if (noteState[i] != -1) {
        //                midiOut.sendMidi(0x90 | noteState[i], i, noteValueState[i]);
        //            }
        //        }
    }
    
    @Override
    public void start() {
        midiOut.sendSysex("F0 47 00 53 1B 00 01 00 F7");
        midiOut.sendSysex("F0 47 00 53 19 00 01 00 F7");
        midiOut.sendSysex("F0 7E 7F 06 01 F7");
        host.scheduleTask(this::handlePing, 50);
    }
    
    public NoteInput getNoteInput() {
        return noteInput;
    }
    
    public void setDrumMode(final boolean drumMode) {
        if (drumMode) {
            enterSessionMode();
            midiOut.sendSysex("F0 47 00 53 1B 00 01 01 F7");
            activateDawMode(true);
        } else {
            midiOut.sendSysex("F0 47 00 53 1B 00 01 00 F7");
            midiOut.sendSysex("F0 47 00 53 19 00 01 02 F7");
            exitSessionMode();
        }
    }
    
    
    public boolean isSessionModeState() {
        return sessionModeState;
    }
    
    public boolean modeHasTextControl() {
        return currentMode.hasLocalControl();
    }
    
    public void exitSessionMode() {
        if (sessionModeState) {
            activateDawMode(false);
            sessionModeState = false;
        }
    }
    
    public void enterSessionMode() {
        if (!sessionModeState) {
            activateDawMode(true);
            sessionModeState = true;
        }
    }
    
    public void activateDawMode(final boolean active) {
        midiOut.sendSysex("F0 47 00 53 1C 00 01 %02X F7".formatted(active ? 1 : 0));
    }
    
    private void handlePing() {
        if (!timedEvents.isEmpty()) {
            for (final TimedEvent event : timedEvents) {
                event.process();
                if (event.isCompleted()) {
                    timedEvents.remove(event);
                }
            }
        }
        host.scheduleTask(this::handlePing, 50);
    }
    
    public void queueEvent(final TimedEvent event) {
        timedEvents.add(event);
    }
    
    public MidiIn getMidiIn() {
        return midiIn;
    }
    
    public void setPrintToClipSeqConsumer(final Consumer<PrintToClipSeq> printToClipSeqConsumer) {
        this.printToClipSeqConsumer = printToClipSeqConsumer;
    }
    
    public void addModeChangeListener(final Consumer<PadMode> modeChangeListener) {
        this.modeChangeListeners.add(modeChangeListener);
    }
    
    @Override
    public void setModeChangeListener(final IntConsumer modeChangeListener) {
        // nothing to do
    }
    
    private void handleMidiIn(final int status, final int data1, final int data2) {
        //Apc64Extension.println("MIDI => %02X %02X %02X", status, data1, data2);
    }
    
    public BooleanValueObject getShiftMode() {
        return shiftMode;
    }
    
    public BooleanValueObject getClearMode() {
        return clearMode;
    }
    
    // Text  F0 47 00 53 10 00 0A 00 20 31 2D 4D 49 44 49 20 00 F7
    // 1-MIDI
    // Text  F0 47 00 53 10 00 0A 00 41 42 43 44 61 31 32 33 00 F7
    // ABCDa123
    // Confirmation F0 7E 00 06 02 47 53 00 19 01 01 00 0E 00 00 00 00 00 41 34 32 33 30 37 32 35 37 34 30 32 37 31
    // 31 00 F7
    
    protected void handleSysEx(final String sysExString) {
        //Apc64Extension.println("SysEx = %s  mode=%s", sysExString, sysExString.startsWith(MODE_CHANGE_MSG));
        if (sysExString.startsWith(DEVICE_VALUE)) {
            Apc64Extension.println("#### Connect to APC #### ");
            initState = false;
            enterSessionMode();
        } else if (sysExString.startsWith(MODE_CHANGE_MSG)) {
            final int mode =
                Integer.parseInt(sysExString.substring(MODE_CHANGE_MSG.length(), MODE_CHANGE_MSG.length() + 2), 16);
            handleModeChange(mode);
        } else if (sysExString.startsWith(PRINT_TO_CLIP_HEAD)) {
            final String value = sysExString.substring(PRINT_TO_CLIP_HEAD.length(), sysExString.length() - 2);
            final int length = fromHexValue(value);
            currentPrintToClip = new PrintToClipSeq(length);
        } else if (sysExString.startsWith(PRINT_TO_CLIP_BODY)) {
            final String data = sysExString.substring(PRINT_TO_CLIP_BODY.length() + 2, sysExString.length() - 4);
            final int headValue =
                fromHexValue(sysExString.substring(PRINT_TO_CLIP_BODY.length(), PRINT_TO_CLIP_BODY.length() + 2));
            currentPrintToClip.addNoteData(data);
            currentPrintToClip.setHeadValue(headValue);
        } else if (sysExString.startsWith(PRINT_TO_CLIP_TAIL)) {
            if (printToClipSeqConsumer != null) {
                printToClipSeqConsumer.accept(currentPrintToClip);
            }
        } else {
            //Apc64Extension.println("Unknown SysEx = %s", sysExString);
        }
    }
    
    private void handleModeChange(final int mode) {
        if (initState) {
            return;
        }
        currentMode = PadMode.fromId(mode);
        //Apc64Extension.println(" MODE =%d ==> %s", mode, currentMode);
        
        if (currentMode.hasLocalControl()) {
            //Apc64Extension.println(" DAW MODE IN %s", sessionModeState);
            activateDawMode(true);
            sessionModeState = true;
            restoreState();
        } else {
            exitSessionMode();
        }
        modeChangeListeners.forEach(listener -> listener.accept(currentMode));
    }
    
    private int fromHexValue(final String hex) {
        if (hex.length() == 4) {
            final int v1 = Integer.parseInt(hex.substring(0, 2), 16);
            final int v2 = Integer.parseInt(hex.substring(2, 4), 16);
            return (v1 << 7) | v2;
        }
        if (hex.length() < 3) {
            return Integer.parseInt(hex, 16);
        }
        return 0;
    }
    
    public void sendText(final int row, final String text) {
        final StringBuilder sb = new StringBuilder(TEXT_PREFIX);
        final int len = Math.min(14, Math.max(3, text.length()));
        sb.append("%02X ".formatted(len + 2));
        sb.append("%02X ".formatted(row));
        final String asciiText = StringUtil.toAsciiDisplay(text, len);
        for (int i = 0; i < len; i++) {
            if (i < asciiText.length()) {
                sb.append("%02X ".formatted((int) asciiText.charAt(i)));
            } else {
                sb.append("20 ");
            }
        }
        sb.append("00 ");
        sb.append("F7");
        //Apc64Extension.println(" SEND TEXT %d => %s", row, text);
        midiOut.sendSysex(sb.toString());
    }
    
    
}
