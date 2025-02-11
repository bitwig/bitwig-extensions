package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareValueMatcher;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.ModeListener;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.NoteHandler;
import com.bitwig.extensions.framework.time.TimedEvent;

public class MidiProcessor {
    private static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    private static final String NOVATION_HEADER = "F0 00 20 29 02 %02X ";
    private static final byte[] TEXT_CONFIG_COMMAND =
        {(byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x14, 0x04, 0x00, 0x00, (byte) 0xF7};
    private final ControllerHost host;
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final NoteInput noteInput;
    private int blinkCounter = 0;
    private final boolean miniVersion;
    private final Queue<TimedEvent> timedEvents = new LinkedList<>();
    private final MidiOut midiOut2;
    private final List<ModeListener> modeListeners = new ArrayList<>();
    private final List<NoteHandler> noteHandlers = new ArrayList<>();
    private final String header;
    private final int modelIdCode;
    private final NoteInput padNoteInput;
    
    public MidiProcessor(final ControllerHost host, final boolean mini) {
        this.host = host;
        this.miniVersion = mini;
        this.modelIdCode = mini ? 0x13 : 0x14;
        this.header = NOVATION_HEADER.formatted(modelIdCode);
        if (mini) {
            TEXT_CONFIG_COMMAND[5] = 0x13;
        }
        this.midiIn = host.getMidiInPort(0);
        this.midiOut = host.getMidiOutPort(0);
        this.midiOut2 = host.getMidiOutPort(1);
        
        padNoteInput = midiIn.createNoteInput("DRUM", "89????", "99????", "A?????");
        
        final MidiIn midiIn2 = host.getMidiInPort(1);
        noteInput = midiIn2.createNoteInput("MIDI", "8?????", "9?????", "A?????", "B?????", "D?????", "E?????");
        noteInput.setShouldConsumeEvents(false);
        midiIn.setMidiCallback(this::handleMidiIn);
        midiIn2.setMidiCallback(this::handleMidiIn2);
        midiIn.setSysexCallback(this::handleSysEx);
    }
    
    public void addNoteHandler(final NoteHandler handler) {
        this.noteHandlers.add(handler);
    }
    
    public String getSysexHeader() {
        return this.header;
    }
    
    public boolean isMiniVersion() {
        return miniVersion;
    }
    
    public void addModeListener(final ModeListener listener) {
        this.modeListeners.add(listener);
    }
    
    private void handleMidiIn(final int statusByte, final int data1, final int data2) {
        final int cmd = statusByte & 0xF0;
        if (statusByte == 0xB6) {
            if (data1 == 0x1E) {
                fireMode(ModeType.ENCODER, data2);
                setEncodersToRelativeMode();
            } else if (data1 == 0x1D) {
                fireMode(ModeType.PAD, data2);
            } else if (data1 == 0x1F) {
                fireMode(ModeType.FADER, data2);
            }
        }
    }
    
    private void setEncodersToRelativeMode() {
        midiOut.sendMidi(0xB6, 0x45, 0x7F);
    }
    
    private void fireMode(final ModeType type, final int id) {
        this.modeListeners.forEach(mode -> mode.handleModeChange(type, id));
    }
    
    private void handleMidiIn2(final int statusByte, final int data1, final int data2) {
        final int code = 0xF0 & statusByte;
        if (code == 0x80 || code == 0x90) {
            this.noteHandlers.forEach(noteHandler -> noteHandler.handleNoteAction(data1, data2));
        }
    }
    
    public NoteInput getNoteInput() {
        return noteInput;
    }
    
    public NoteInput getPadNoteInput() {
        return padNoteInput;
    }
    
    private void handleSysEx(final String data) {
        LaunchkeyMk4Extension.println(" Incoming Sysex = %s", data);
    }
    
    private void dawConnect(final boolean connect) {
        midiOut.sendSysex(this.header + "02 %02X F7".formatted(connect ? 0x7F : 0x0));
    }
    
    public void init() {
        dawConnect(false);
        dawConnect(true);
        midiOut.sendMidi(0xB6, 0x54, 0x01);
        setEncodersToRelativeMode();
        host.scheduleTask(this::processMidi, 50);
    }
    
    public void exitDawMode() {
        midiOut.sendSysex(this.header + "04 20 00 F7");
        midiOut.sendMidi(0xB6, 0x54, 0x00);
        dawConnect(false);
    }
    
    public MidiIn getMidiIn() {
        return midiIn;
    }
    
    public RelativeHardwareValueMatcher createNonAcceleratedMatcher(final int ccNr) {
        final RelativeHardwareValueMatcher stepDownMatcher =
            midiIn.createRelativeValueMatcher("(status == 191 && data1 == %d && data2 > 64)".formatted(ccNr), -1);
        final RelativeHardwareValueMatcher stepUpMatcher =
            midiIn.createRelativeValueMatcher("(status == 191 && data1 == %d && data2 < 65)".formatted(ccNr), 1);
        return host.createOrRelativeHardwareValueMatcher(stepDownMatcher, stepUpMatcher);
    }
    
    public RelativeHardwareValueMatcher createAcceleratedMatcher(final int ccNr) {
        return midiIn.createRelativeBinOffsetCCValueMatcher(0xF, ccNr, 200);
    }
    
    public RelativeHardwarControlBindable createIncAction(final IntConsumer changeAction) {
        final HardwareActionBindable incAction = host.createAction(() -> changeAction.accept(1), () -> "+");
        final HardwareActionBindable decAction = host.createAction(() -> changeAction.accept(-1), () -> "-");
        return host.createRelativeHardwareControlStepTarget(incAction, decAction);
    }
    
    
    public void setNoteMatcher(final HardwareButton hwButton, final int noteNr, final int channel) {
        hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(channel, noteNr));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, noteNr));
    }
    
    public void setCcMatcher(final HardwareButton hwButton, final int ccNr, final int channel) {
        hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 127));
        hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 0));
    }
    
    public void setAbsoluteCcMatcher(final AbsoluteHardwareKnob control, final int ccNr, final int channel) {
        control.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(channel, ccNr));
    }
    
    private void processMidi() {
        blinkCounter = (blinkCounter + 1) % 8;
        if (!timedEvents.isEmpty()) {
            final Iterator<TimedEvent> it = timedEvents.iterator();
            while (it.hasNext()) {
                final TimedEvent event = it.next();
                event.process();
                if (event.isCompleted()) {
                    it.remove();
                }
            }
        }
        //        for (final Runnable action : tickActions) {
        //            action.run();
        //        }
        host.scheduleTask(this::processMidi, 50);
    }
    
    public void queueTimedEvent(final TimedEvent timedEvent) {
        timedEvents.add(timedEvent);
    }
    
    public void sendMidi(final int status, final int data1, final int data2) {
        midiOut.sendMidi(status, data1, data2);
    }
    
    public void sendSysExBytes(final byte[] data) {
        midiOut.sendSysex(data);
    }
    
    public void sendSysExString(final String data) {
        midiOut.sendSysex(data);
    }
    
}
