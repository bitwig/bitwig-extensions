package com.bitwig.extensions.controllers.novation.slmk3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.AbsoluteHardwareValueMatcher;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareValueMatcher;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenLayout;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.SysExScreenBuilder;
import com.bitwig.extensions.controllers.novation.slmk3.value.NoteHandler;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.Midi;

public class MidiProcessor {
    private static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    private final static String NOVATION_HEADER = "F0 00 20 29 02 0A 01 ";
    private final MidiOut midiKeyOut;
    public final static String PROPERTY_SET_HEADER = NOVATION_HEADER + "02 ";
    private final static String NOTIFICATION_HEADER = NOVATION_HEADER + "04 ";
    private static final String SET_LAYOUT_HEADER = NOVATION_HEADER + "01 %02X F7";
    private static final byte[] RGB_COMMAND =
        {(byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x0A, 0x01, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF7};
    
    private final ControllerHost host;
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final NoteInput drumNoteInput;
    private final NoteInput noteInput;
    private final Queue<TimedEvent> timedEvents = new LinkedList<>();
    private final List<NoteHandler> noteHandlers = new ArrayList<>();
    private final List<NoteHandler> padNoteHandlers = new ArrayList<>();
    private int blinkCounter = 0;
    private final List<Runnable> tickActions = new ArrayList<>();
    
    public MidiProcessor(final ControllerHost host) {
        this.host = host;
        this.midiIn = host.getMidiInPort(0);
        this.midiOut = host.getMidiOutPort(0);
        this.midiKeyOut = host.getMidiOutPort(1);
        drumNoteInput = midiIn.createNoteInput("MIDI", "8F????", "9F????", "AF????");
        drumNoteInput.setShouldConsumeEvents(false);
        
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
    
    public void addPadNoteHandler(final NoteHandler handler) {this.padNoteHandlers.add(handler);}
    
    private void handleMidiIn(final int statusByte, final int data1, final int data2) {
        //SlMk3Extension.println("MIDI-1 %02X %02X %02X", statusByte, data1, data2);
        final int code = 0xF0 & statusByte;
        if (code == 0x80 || code == 0x90) {
            this.padNoteHandlers.forEach(noteHandler -> noteHandler.handleNoteAction(data1, data2));
        }
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
    
    private void handleSysEx(final String data) {
        if (data.startsWith("f07e0006020020")) {
            setScreenLayout(ScreenLayout.KNOB);
        } else {
            SlMk3Extension.println("SYS EX = %s", data);
        }
    }
    
    public void init() {
        midiOut.sendSysex(DEVICE_INQUIRY);
        host.scheduleTask(this::processMidi, 50);
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
        for (final Runnable action : tickActions) {
            action.run();
        }
        host.scheduleTask(this::processMidi, 50);
    }
    
    public RelativeHardwareValueMatcher createAcceleratedMatcher(final int ccNr) {
        return midiIn.createRelative2sComplementCCValueMatcher(0xF, ccNr, 200);
    }
    
    public void setNoteMatcher(final HardwareButton hwButton, final int noteNr) {
        hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0xF, noteNr));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0xF, noteNr));
    }
    
    public void setCcMatcher(final HardwareButton hwButton, final int ccNr) {
        hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, ccNr, 127));
        hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, ccNr, 0));
    }
    
    public RelativeHardwarControlBindable createIncAction(final IntConsumer changeAction) {
        final HardwareActionBindable incAction = host.createAction(() -> changeAction.accept(1), () -> "+");
        final HardwareActionBindable decAction = host.createAction(() -> changeAction.accept(-1), () -> "-");
        return host.createRelativeHardwareControlStepTarget(incAction, decAction);
    }
    
    public RelativeHardwareValueMatcher createEncoderMatcher(final int ccNr) {
        final String matchExpr = String.format("(status==%d && data1==%d && data2>0)", Midi.CC, ccNr);
        return midiIn.createRelative2sComplementValueMatcher(matchExpr, "data2", 7, 200);
    }
    
    public AbsoluteHardwareValueMatcher createAbsoluteHardwareMatcher(final int ccNr) {
        return midiIn.createAbsoluteCCValueMatcher(0xF, ccNr);
    }
    
    public RelativeHardwareValueMatcher createNonAcceleratedMatcher(final int ccNr) {
        final RelativeHardwareValueMatcher stepDownMatcher =
            midiIn.createRelativeValueMatcher("(status == 191 && data1 == %d && data2 > 64)".formatted(ccNr), -1);
        final RelativeHardwareValueMatcher stepUpMatcher =
            midiIn.createRelativeValueMatcher("(status == 191 && data1 == %d && data2 < 65)".formatted(ccNr), 1);
        return host.createOrRelativeHardwareValueMatcher(stepDownMatcher, stepUpMatcher);
    }
    
    public void setScreenLayout(final ScreenLayout layout) {
        final String msg = SET_LAYOUT_HEADER.formatted(layout.getId());
        midiOut.sendSysex(msg);
    }
    
    public void send(final SysExScreenBuilder sb) {
        sb.complete();
        midiOut.sendSysex(sb.getString());
    }
    
    public void sendNotification(final String text1, final String text2) {
        final StringBuilder sb = new StringBuilder(NOTIFICATION_HEADER);
        appendText(sb, text1, 18);
        appendText(sb, text2, 18);
        sb.append("F7");
        midiOut.sendSysex(sb.toString());
    }
    
    public NoteInput getDrumNoteInput() {
        return drumNoteInput;
    }
    
    private static void appendText(final StringBuilder sb, final String text, final int maxLen) {
        final String validText = StringUtil.toAsciiDisplay(text, maxLen);
        for (int i = 0; i < validText.length(); i++) {
            sb.append("%02X ".formatted((int) validText.charAt(i)));
        }
        sb.append("00 ");
    }
    
    public void updateRgbState(final int ledIndex, final SlRgbState rgbState) {
        if (rgbState.getBehavior() == 2) {
            sendRgbState(ledIndex, rgbState.getOtherColor() != null ? rgbState.getOtherColor() : SlRgbState.OFF);
        }
        sendRgbState(ledIndex, rgbState);
    }
    
    private void sendRgbState(final int ledIndex, final SlRgbState rgbState) {
        final byte[] msg = new byte[RGB_COMMAND.length];
        System.arraycopy(RGB_COMMAND, 0, msg, 0, RGB_COMMAND.length);
        msg[8] = (byte) (ledIndex & 0x7F);
        msg[9] = (byte) (rgbState.getBehavior() & 0x7F);
        msg[10] = (byte) (rgbState.getRed() & 0x7F);
        msg[11] = (byte) (rgbState.getGreen() & 0x7F);
        msg[12] = (byte) (rgbState.getBlue() & 0x7F);
        midiOut.sendSysex(msg);
    }
    
    public void updateLightState(final int ledIndex, final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof SlRgbState rgbState) {
            updateRgbState(ledIndex, rgbState);
        }
    }
    
    public void sendNoteToKey(final int note, final int vel) {
        midiOut.sendMidi(Midi.NOTE_ON | 0x0, note, vel);
    }
    
    public void queueTimedEvent(final TimedEvent timedEvent) {
        timedEvents.add(timedEvent);
    }
    
    public void delay(final Runnable delayedAction, final int delayTime) {
        host.scheduleTask(delayedAction, delayTime);
    }
}
