package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.time.TimedEvent;

public class MidiProcessor {
    private static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    private static String NOVATION_HEADER = "F0 00 20 29 02 "; //TODO 13 for minis
    private final ControllerHost host;
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    //private final NoteInput drumNoteInput;
    private final NoteInput noteInput;
    private int blinkCounter = 0;
    private final Queue<TimedEvent> timedEvents = new LinkedList<>();
    private final MidiOut midiOut2;
    
    public MidiProcessor(final ControllerHost host, final boolean mini) {
        this.host = host;
        NOVATION_HEADER += mini ? "13 " : "14 ";
        this.midiIn = host.getMidiInPort(0);
        this.midiOut = host.getMidiOutPort(0);
        this.midiOut2 = host.getMidiOutPort(1);
        //drumNoteInput = midiIn.createNoteInput("MIDI", "8F????", "9F????", "AF????");
        //drumNoteInput.setShouldConsumeEvents(false);
        
        final MidiIn midiIn2 = host.getMidiInPort(1);
        noteInput = midiIn2.createNoteInput("MIDI", "8?????", "9?????", "A?????", "B?????", "D?????", "E?????");
        noteInput.setShouldConsumeEvents(false);
        midiIn.setMidiCallback(this::handleMidiIn);
        midiIn2.setMidiCallback(this::handleMidiIn2);
        midiIn.setSysexCallback(this::handleSysEx);
    }
    
    private void handleMidiIn(final int statusByte, final int data1, final int data2) {
        final int cmd = statusByte & 0xF0;
        if (statusByte == 0xB6) {
            if (data1 == 0x1E) {
                LaunchkeyMk4Extension.println(" ENCODER MODE = %d", data2);
            } else if (data1 == 0x1D) {
                LaunchkeyMk4Extension.println(" PAD MODE = %d", data2);
            } else if (data1 == 0x1F) {
                LaunchkeyMk4Extension.println(" SLIDER MODE = %d", data2);
            }
        } else if (cmd == 0x90 || cmd == 0xB0) {
            LaunchkeyMk4Extension.println("MIDI-1 %02X %02X %02X", statusByte, data1, data2);
        }
    }
    
    private int idNrPmMsb = 0;
    private int idNrPmLsb = 0;
    private int valNrPmMsb = 0;
    private int valNrPmLsb = 0;
    
    private void handleMidiIn2(final int statusByte, final int data1, final int data2) {
        if (statusByte == 0xBA) {
            //LaunchkeyMk4Extension.println("MIDI-2 %02X %02X %02X", statusByte, data1, data2);
            if (data1 == 0x63 && data2 != 0x7F) {
                idNrPmMsb = data2;
            } else if (data1 == 0x62) {
                if (data2 == 0x7F) {
                    final int id = idNrPmMsb << 7 | idNrPmLsb;
                    final int val = valNrPmMsb << 7 | valNrPmLsb;
                    LaunchkeyMk4Extension.println(" Received NRPM ID=%d VAL=%d", id, val);
                } else {
                    idNrPmLsb = data2;
                }
            } else if (data1 == 0x6) {
                valNrPmMsb = data2;
            } else if (data1 == 0x26) {
                valNrPmLsb = data2;
            }
        }
    }
    
    public NoteInput getNoteInput() {
        return noteInput;
    }
    
    private void handleSysEx(final String data) {
        LaunchkeyMk4Extension.println("SYSEX = %s", data);
        if (data.startsWith("f07e000602")) {
            LaunchkeyMk4Extension.println(" --> connect to Launch key");
            midiOut.sendMidi(0x9f, 0xC, 0x7f);
        }
    }
    
    public void exitDawMode() {
        midiOut.sendMidi(0x9f, 0xC, 0x0);
    }
    
    public MidiIn getMidiIn() {
        return midiIn;
    }
    
    public void setNoteMatcher(final HardwareButton hwButton, final int noteNr, final int channel) {
        hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(channel, noteNr));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, noteNr));
    }
    
    public void setCcMatcher(final HardwareButton hwButton, final int ccNr, final int channel) {
        hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 127));
        hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 0));
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
        //        for (final Runnable action : tickActions) {
        //            action.run();
        //        }
        host.scheduleTask(this::processMidi, 50);
    }
    
    private String[] getMask() {
        final List<String> masks = new ArrayList<>();
        masks.add("8?????"); // Note On
        masks.add("9?????"); // Note Off
        masks.add("A?????"); // Poly Aftertouch
        masks.add("D?????"); // Channel Aftertouch
        masks.add("B?????"); // CCss
        masks.add("E?????"); // Pitchbend
        return masks.toArray(String[]::new);
    }
    
    public void queueTimedEvent(final TimedEvent timedEvent) {
        timedEvents.add(timedEvent);
    }
    
    public void sendMidi(final int status, final int data1, final int data2) {
        midiOut.sendMidi(status, data1, data2);
    }
}
