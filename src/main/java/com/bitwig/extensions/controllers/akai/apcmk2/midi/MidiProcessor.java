package com.bitwig.extensions.controllers.akai.apcmk2.midi;

import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.function.IntConsumer;

public interface MidiProcessor {

    NoteInput createNoteInput(String name, String... mask);

    void sendMidi(final int status, final int val1, final int val2);

    void start();

    void queueEvent(TimedEvent currentTimer);

    void setModeChangeListener(final IntConsumer modeChangeListener);

    MidiIn getMidiIn();
}
