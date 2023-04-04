package com.bitwig.extensions.controllers.akai.apcmk2.midi;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.akai.apcmk2.DebugApc;
import com.bitwig.extensions.framework.time.TimedEvent;

public class MidiProcessorDirect extends AbstractMidiProcessor {

    public MidiProcessorDirect(final ControllerHost host, final MidiIn midiIn, final MidiOut midiOut) {
        super(host, midiIn, midiOut);
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


    protected void handleSysEx(final String sysExString) {
        if (sysExString.startsWith(KEYS_DEVICE_RESPONSE)) {
            DebugApc.println(" Response KEYS");
        }
    }

    public void sendMidi(final int status, final int val1, final int val2) {
        midiOut.sendMidi(status, val1, val2);
    }


}
