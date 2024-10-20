package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.ModeListener;
import com.bitwig.extensions.framework.time.TimedEvent;

public class MidiProcessor {
    private static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    private static String NOVATION_HEADER = "F0 00 20 29 02 %02X ";
    private static final byte[] TEXT_CONFIG_COMMAND =
        {(byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x14, 0x04, 0x00, 0x00, (byte) 0xF7};
    private static String TEXT_COMMAND;
    private final ControllerHost host;
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final NoteInput noteInput;
    private int blinkCounter = 0;
    private final boolean miniVersion;
    private final Queue<TimedEvent> timedEvents = new LinkedList<>();
    private final MidiOut midiOut2;
    private final List<ModeListener> modeListeners = new ArrayList<>();
    private Runnable connectionCallback;
    private final String header;

    public MidiProcessor(final ControllerHost host, final boolean mini) {
        this.host = host;
        this.miniVersion = mini;
        this.header = NOVATION_HEADER.formatted(mini ? 0x13 : 0x14);
        TEXT_COMMAND = this.header + "06 ";
        if (mini) {
            TEXT_CONFIG_COMMAND[5] = 0x13;
        }
        this.midiIn = host.getMidiInPort(0);
        this.midiOut = host.getMidiOutPort(0);
        this.midiOut2 = host.getMidiOutPort(1);

        final MidiIn midiIn2 = host.getMidiInPort(1);
        noteInput = midiIn2.createNoteInput("MIDI", "8?????", "9?????", "A?????", "B?????", "D?????", "E?????");
        noteInput.setShouldConsumeEvents(false);
        midiIn.setMidiCallback(this::handleMidiIn);
        midiIn2.setMidiCallback(this::handleMidiIn2);
        midiIn.setSysexCallback(this::handleSysEx);
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
            } else if (data1 == 0x1D) {
                fireMode(ModeType.PAD, data2);
            } else if (data1 == 0x1F) {
                fireMode(ModeType.FADER, data2);
            }
        }
        //             9F 0C 7F
        else if (cmd == 0x90 || cmd == 0xB0) {
            LaunchkeyMk4Extension.println("MIDI-1 %02X %02X %02X", statusByte, data1, data2);
        }
    }

    private void fireMode(final ModeType type, final int id) {
        this.modeListeners.forEach(mode -> mode.handleModeChange(type, id));
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
        LaunchkeyMk4Extension.println(" Incoming Sysex = %s", data);
    }

    private void handleSysEx_(final String data) {
        if (data.startsWith("f000202902140200f7")) {
            LaunchkeyMk4Extension.println(" == INIT now Device inquire");
            midiOut.sendSysex(DEVICE_INQUIRY);
        } else if (data.startsWith("f07e000602")) {
            midiOut.sendSysex("F0 00 20 29 02 14 02 7F F7");
            //midiOut.sendMidi(0x9f, 0xC, 0x7f);
        } else if (data.startsWith("f00020290214027ff7")) {
            connectionCallback.run();
        } else {
            LaunchkeyMk4Extension.println(" SYSEX = %s", data);
        }
    }

    public void init(Runnable connectionCallback) {
        this.connectionCallback = connectionCallback;
        midiOut.sendSysex("F0 00 20 29 02 14 02 00 F7");
        midiOut.sendSysex("F0 00 20 29 02 14 02 7F F7");
        host.scheduleTask(this::processMidi, 50);
    }

    public void init_(Runnable connectionCallback) {
        this.connectionCallback = connectionCallback;
        midiOut.sendSysex("F0 00 20 29 02 14 04 20 00 F7");
        midiOut.sendSysex("F0 00 20 29 02 14 02 00 F7");
        host.scheduleTask(this::processMidi, 50);
    }

    public void exitDawMode() {
        midiOut.sendSysex("F0 00 20 29 02 14 04 20 00 F7");
        midiOut.sendSysex("F0 00 20 29 02 14 02 00 F7");
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

    public void sendSysExBytes(byte[] data) {
        midiOut.sendSysex(data);
    }

    public void sendSysExString(String data) {
        midiOut.sendSysex(data);
    }

}
