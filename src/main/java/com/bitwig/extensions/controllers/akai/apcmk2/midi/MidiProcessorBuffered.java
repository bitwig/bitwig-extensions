package com.bitwig.extensions.controllers.akai.apcmk2.midi;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.akai.apcmk2.AbstractAkaiApcExtension;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.LinkedList;
import java.util.Queue;

public class MidiProcessorBuffered extends AbstractMidiProcessor {
    private final Queue<Runnable> messageQueue = new LinkedList<>();

    public MidiProcessorBuffered(final ControllerHost host, final MidiIn midiIn, final MidiOut midiOut) {
        super(host, midiIn, midiOut);
    }

    public void start() {
        midiOut.sendSysex(DEVICE_INQUIRY);
        host.scheduleTask(this::handlePing, 50);
    }

    private void handlePing() {
        if (!messageQueue.isEmpty()) {
            int count = 0;
            while (!messageQueue.isEmpty()) {
                messageQueue.poll().run();
                count++;
                if (!messageQueue.isEmpty() && count % 20 == 0) {
                    pause();
                }
            }
        }
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


    protected void handleSysEx(final String sysExString) {
        if (sysExString.startsWith(MINI_DEVICE_RESPONSE)) {
            midiOut.sendSysex(MINI_SESSION_MODE);
        } else if (sysExString.startsWith(MINI_MODE_CHANGE_HEAD)) {
            int mode = Integer.parseInt(sysExString.substring(MINI_MODE_CHANGE_HEAD.length(), sysExString.length() - 2),
                    16);
            midiOut.sendSysex(sysExString);
            if (modeChangeListener != null) {
                modeChangeListener.accept(mode);
            }
        } else {
            AbstractAkaiApcExtension.println("Sysex %s", sysExString);
        }
    }

    public void sendMidi(final int status, final int val1, final int val2) {
        messageQueue.add(() -> midiOut.sendMidi(status, val1, val2));
    }

    void pause() {
        try {
            Thread.sleep(0, 2);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
