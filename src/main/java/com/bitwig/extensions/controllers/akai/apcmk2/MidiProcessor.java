package com.bitwig.extensions.controllers.akai.apcmk2;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;

public class MidiProcessor {
    protected static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    protected static final String MINI_MODE_CHANGE_HEAD = "f0477f4f620001";
    protected static final String MINI_DEVICE_RESPONSE = "f07e000602474f0019052100007f0";
    protected static final String KEYS_DEVICE_RESPONSE = "f07e000602474e0019052200007f0";
    protected static final String MINI_SESSION_MODE = "f0 47 7f 4f 62 00 01 00 f7";

    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    private final ControllerHost host;
    private IntConsumer modeChangeListener;

    public MidiProcessor(final ControllerHost host, final MidiIn midiIn, final MidiOut midiOut) {
        this.host = host;
        this.midiIn = midiIn;
        this.midiOut = midiOut;
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        midiIn.setSysexCallback(this::handleSysEx);
    }

    public void queueEvent(final TimedEvent event) {
        timedEvents.add(event);
    }

    public MidiIn getMidiIn() {
        return midiIn;
    }

    public MidiOut getMidiOut() {
        return midiOut;
    }

    public void start() {
        midiOut.sendSysex(DEVICE_INQUIRY);
        host.scheduleTask(this::handlePing, 50);
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

    public void setModeChangeListener(final IntConsumer modeChangeListener) {
        this.modeChangeListener = modeChangeListener;
    }

    public void sendMidi(final int status, final int val1, final int val2) {
        midiOut.sendMidi(status, val1, val2);
    }

    private void handleSysEx(final String sysExString) {
        if (sysExString.startsWith(KEYS_DEVICE_RESPONSE)) {
            DebugApc.println(" Response KEYS");
            //midiOut.sendSysex(INIT_REQUEST);
        } else if (sysExString.startsWith(MINI_DEVICE_RESPONSE)) {
            midiOut.sendSysex(MINI_SESSION_MODE);
        } else if (sysExString.startsWith(MINI_MODE_CHANGE_HEAD)) {
            int mode = Integer.parseInt(sysExString.substring(MINI_MODE_CHANGE_HEAD.length(), sysExString.length() - 2),
               16);
            midiOut.sendSysex(sysExString);
            if (modeChangeListener != null) {
                modeChangeListener.accept(mode);
            }
        } else {
            DebugApc.println("Sysex %s", sysExString);
        }
    }

    private void onMidi0(final ShortMidiMessage msg) {
        DebugApc.println("Incoming %02X %02X %02X", msg.getStatusByte(), msg.getData1(), msg.getData2());
    }

}
