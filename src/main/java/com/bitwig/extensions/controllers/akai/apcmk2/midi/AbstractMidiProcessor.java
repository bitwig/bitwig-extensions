package com.bitwig.extensions.controllers.akai.apcmk2.midi;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.controllers.akai.apc.common.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apcmk2.AbstractAkaiApcExtension;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;

public abstract class AbstractMidiProcessor implements MidiProcessor {
    protected static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    protected static final String MINI_MODE_CHANGE_HEAD = "f0477f4f620001";
    protected static final String MINI_DEVICE_RESPONSE = "f07e000602474f0019052100007f0";
    protected static final String KEYS_DEVICE_RESPONSE = "f07e000602474e0019052200007f0";
    protected static final String MINI_SESSION_MODE = "f0 47 7f 4f 62 00 01 00 f7";

    protected final MidiIn midiIn;
    protected final MidiOut midiOut;
    protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    protected final ControllerHost host;
    protected IntConsumer modeChangeListener;

    public AbstractMidiProcessor(final ControllerHost host, final MidiIn midiIn, final MidiOut midiOut) {
        this.host = host;
        this.midiIn = midiIn;
        this.midiOut = midiOut;
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        midiIn.setSysexCallback(this::handleSysEx);
    }

    @Override
    public NoteInput createNoteInput(String name, String... mask) {
        return midiIn.createNoteInput(name, mask);
    }

    public void queueEvent(final TimedEvent event) {
        timedEvents.add(event);
    }

    public MidiIn getMidiIn() {
        return midiIn;
    }

    public void setModeChangeListener(final IntConsumer modeChangeListener) {
        this.modeChangeListener = modeChangeListener;
    }

    protected void onMidi0(final ShortMidiMessage msg) {
        AbstractAkaiApcExtension.println("Incoming %02X %02X %02X", msg.getStatusByte(), msg.getData1(),
                msg.getData2());
    }

    protected abstract void handleSysEx(final String sysExString);
}
