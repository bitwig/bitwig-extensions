package com.bitwig.extensions.controllers.nativeinstruments.komplete.midi;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.KompleteKontrolExtension;

public class MidiProcessor {
    final NhiaSyexLevelsCommand trackLevelMeterCommand = new NhiaSyexLevelsCommand(0x49);

    private static final int MIDI_KK_DAW_COMMAND = 0xBF;
    private static final int MIDI_GOODBYE_COMMAND = 0x2;

    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final ControllerHost host;
    private boolean dawModeConfirmed;
    private String lastReportedKKInstance = null;
    private final HardwareSurface surface;

    public MidiProcessor(ControllerHost host, HardwareSurface surface) {
        this.midiIn = host.getMidiInPort(0);
        this.midiOut = host.getMidiOutPort(0);
        this.surface = surface;
        this.host = host;
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
    }

    public void exit() {
        midiOut.sendMidi(MIDI_KK_DAW_COMMAND, MIDI_GOODBYE_COMMAND, 0);
    }

    protected void onMidi0(final ShortMidiMessage msg) {
        if (msg.getStatusByte() == 0xBF) {
            if (msg.getData1() == 1) {
                dawModeConfirmed = true;
            }
        }
    }

    public HardwareButton createButton(String name, final int ccNr, int index) {
        HardwareButton hwButton = surface.createHardwareButton(name + "_" + index);
        hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, ccNr, index));
        return hwButton;
    }

    public void updateKompleteKontrolInstance(final String instanceParamName) {
        if (lastReportedKKInstance == null || !lastReportedKKInstance.equals(instanceParamName)) {
            KompleteKontrolExtension.println(" KP=%s", instanceParamName);
            sendTextCommand(TextCommand.SELECTED_TRACK, instanceParamName);
            host.scheduleTask(() -> sendTextCommand(TextCommand.SELECTED_TRACK, instanceParamName), 100);
            lastReportedKKInstance = instanceParamName;
        }
    }

    public void sendValueCommand(ValueCommand command, int index, boolean value) {
        command.send(midiOut, index, value);
    }

    public void sendValueCommand(ValueCommand command, int index, int value) {
        command.send(midiOut, index, value);
    }

    public void sendTextCommand(TextCommand command, String text) {
        command.send(midiOut, text);
    }

    public void sendTextCommand(TextCommand command, int index, String text) {
        command.send(midiOut, index, text);
    }

    public void intoDawMode() {
        midiOut.sendMidi(0xBF, 0x1, 0x3);
    }

    public void sendLedUpdate(final CcAssignment assignment, final int value) {
        midiOut.sendMidi(0xBF, assignment.getStateId(), value);
    }

    public void sendLedUpdate(final int code, final int value) {
        midiOut.sendMidi(0xBF, code, value);
    }

    public void sendVolumeValue(int index, byte value) {
        midiOut.sendMidi(0xBF, 0x50 + index, value);
    }

    public void sendPanValue(int index, int v) {
        midiOut.sendMidi(0xBF, 0x58 + index, v);
    }

    public void updateVuLeft(int index, byte value) {
        trackLevelMeterCommand.updateLeft(index, value);
    }

    public void updateVuRight(int index, byte value) {
        trackLevelMeterCommand.updateRight(index, value);
        trackLevelMeterCommand.update(midiOut);
    }

    public void doFlush() {
        if (dawModeConfirmed) {
            surface.updateHardware();
        }
    }

    public void resetAllLEDs() {
        for (final CcAssignment cc : CcAssignment.values()) {
            sendLedUpdate(cc, 0);
        }
    }

    public MidiIn getMidiIn() {
        return midiIn;
    }

    public HardwareSurface getSurface() {
        return surface;
    }
}
