package com.bitwig.extensions.controllers.neuzeitinstruments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.Midi;

@Component
public class DropMidiProcessor {
    
    private final ControllerHost host;
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final List<IntConsumer> dropRequestListeners = new ArrayList<>();
    protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    
    public DropMidiProcessor(final ControllerHost host) {
        this.host = host;
        this.midiIn = host.getMidiInPort(0);
        this.midiOut = host.getMidiOutPort(0);
        midiIn.setMidiCallback(this::handleMidiIn);
        midiIn.setSysexCallback(this::handleSysEx);
        
        setUpKeyboardChannel(host);
        host.scheduleTask(this::processMidi, 200);
    }
    
    private void setUpKeyboardChannel(final ControllerHost host) {
        final String[] filters = Stream.of("1") //
            .map(index -> getKeyboardFilter(index, "1"))//
            .flatMap(Optional::stream) //
            .distinct() //
            .toArray(String[]::new);
        if (filters.length > 0) {
            midiIn.createNoteInput("KEYBOARD", filters);
        }
    }
    
    private Optional<String> getKeyboardFilter(final String index, final String defaultValue) {
        final SettableEnumValue preferenceValue = this.host.getPreferences().getEnumSetting(
            "Keyboard Channel", "Notes", Stream.concat(
                    Stream.of("Off"), IntStream.rangeClosed(1, 15) //
                        .filter(v -> v != 2) //
                        .mapToObj(String::valueOf)) // convert int → String
                .toArray(String[]::new), defaultValue);
        preferenceValue.markInterested();
        final String value = preferenceValue.get();
        if (value.equals("Off")) {
            return Optional.empty();
        }
        final int channel = Integer.parseInt(preferenceValue.get());
        final String filter = "9%X????".formatted(channel - 1);
        return Optional.of(filter);
    }
    
    
    public void assignNoteAction(final HardwareButton hwButton, final int midiNote) {
        hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0xF, midiNote));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0xF, midiNote));
    }
    
    public void assignCcAction(final HardwareButton hwButton, final int channel, final int ccNr) {
        hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 0x7F));
        hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 0x0));
    }
    
    public void assignCcMatcher(final AbsoluteHardwareControl control, final int channel, final int ccValue) {
        control.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(channel, ccValue));
    }
    
    public void queueEvent(final TimedEvent event) {
        timedEvents.add(event);
    }
    
    private void processMidi() {
        if (!timedEvents.isEmpty()) {
            for (final TimedEvent event : timedEvents) {
                event.process();
                if (event.isCompleted()) {
                    timedEvents.remove(event);
                }
            }
        }
        host.scheduleTask(this::processMidi, 50);
    }
    
    
    public void addDropRequestListener(final IntConsumer dropRequestListener) {
        dropRequestListeners.add(dropRequestListener);
    }
    
    private void handleSysEx(final String data) {
        DropExtension.println(" SYS_EX = %s", data);
    }
    
    private void handleMidiIn(final int status, final int data1, final int data2) {
        if (status == 0x9F && data1 == 0x53) {
            dropRequestListeners.forEach(listener -> listener.accept(data2));
        } else {
            DropExtension.println("MIDI => %02X %02X %02X  %d", status, data1, data2, data1);
        }
    }
    
    public MidiIn getMidiIn() {
        return midiIn;
    }
    
    public void sendMidi(final int status, final int val1, final int val2) {
        midiOut.sendMidi(status, val1, val2);
    }
    
    public void sendMidiDelayed(final int status, final int val1, final int val2) {
        host.scheduleTask(() -> midiOut.sendMidi(status, val1, val2), 500);
    }
    
    public void setLayoutMode(final int mode) {
        midiOut.sendMidi(Midi.NOTE_ON | 0xF, 83, mode);
    }
}
