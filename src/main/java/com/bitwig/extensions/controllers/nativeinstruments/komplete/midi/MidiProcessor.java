package com.bitwig.extensions.controllers.nativeinstruments.komplete.midi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.DataStringUtil;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.KompleteKontrolExtension;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.LayoutType;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.device.DeviceSelectionTab;

public class MidiProcessor {
    final NhiaSyexLevelsCommand trackLevelMeterCommand = new NhiaSyexLevelsCommand(0x49);
    private final static String INCOMING_HEADER = "f0002109000044430100";
    private final static String INCOMING_INDEX = INCOMING_HEADER + "7000";
    private final static String INCOMING_TEMPO = INCOMING_HEADER + "190000";
    private static final int MIDI_KK_DAW_COMMAND = 0xBF;
    private static final int MIDI_GOODBYE_COMMAND = 0x2;
    
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final ControllerHost host;
    private boolean dawModeConfirmed;
    private String lastReportedKKInstance = null;
    private int expectedProtocol;
    private final HardwareSurface surface;
    private final List<DeviceMidiListener> deviceListeners = new ArrayList<>();
    private final List<IntConsumer> modeListener = new ArrayList<>();
    private final List<DoubleConsumer> tempoListener = new ArrayList<>();
    
    public MidiProcessor(final ControllerHost host, final HardwareSurface surface) {
        this.midiIn = host.getMidiInPort(0);
        this.midiOut = host.getMidiOutPort(0);
        this.surface = surface;
        this.host = host;
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        midiIn.setSysexCallback(this::handleSysex);
    }
    
    public void addDeviceMidiListener(final DeviceMidiListener listener) {
        this.deviceListeners.add(listener);
    }
    
    public void addModeListener(final IntConsumer listener) {
        this.modeListener.add(listener);
    }
    
    public void addTempoListener(final DoubleConsumer listener) {
        this.tempoListener.add(listener);
    }
    
    private void handleSysex(final String sysExData) {
        if (sysExData.startsWith(INCOMING_INDEX)) {
            final String data = sysExData.substring(INCOMING_INDEX.length(), sysExData.length() - 2);
            final int[] selectionPath = getIndexMap(data);
            deviceListeners.forEach(listener -> listener.handleDeviceSelect(selectionPath));
        } else if (sysExData.startsWith(INCOMING_TEMPO)) {
            final String data = sysExData.substring(INCOMING_TEMPO.length(), sysExData.length() - 2);
            final int[] tempoData = getIndexMap(data);
            final double tempo = DataStringUtil.toTempo(tempoData);
            tempoListener.forEach(l -> l.accept(tempo));
        } else {
            KompleteKontrolExtension.println(" SYSEX = %s", sysExData);
        }
    }
    
    public void intoDawMode(final int protocol) {
        this.expectedProtocol = protocol;
        midiOut.sendMidi(0xBF, 0x1, protocol);
    }
    
    public void delay(final Runnable action, final int delay) {
        host.scheduleTask(action, delay);
    }
    
    public static int[] getIndexMap(final String data) {
        final int[] result = new int[data.length() / 2];
        for (int i = 0; i < data.length(); i += 2) {
            result[i / 2] = Integer.parseInt(data.substring(i, i + 2), 16);
        }
        return result;
    }
    
    public void exit() {
        midiOut.sendMidi(MIDI_KK_DAW_COMMAND, MIDI_GOODBYE_COMMAND, 0);
    }
    
    protected void onMidi0(final ShortMidiMessage msg) {
        if (msg.getStatusByte() == 0xBF) {
            if (msg.getData1() == 1) {
                handleHandshakeComplete(msg);
            } else if (msg.getData1() == 5) {
                modeListener.forEach(listener -> listener.accept(msg.getData2()));
            } else {
                KompleteKontrolExtension.println(
                    "MIDI => %02X %02X %02X", msg.getStatusByte(), msg.getData1(), msg.getData2());
            }
        } else {
            KompleteKontrolExtension.println(
                "MIDI => %02X %02X %02X", msg.getStatusByte(), msg.getData1(), msg.getData2());
        }
    }
    
    private void handleHandshakeComplete(final ShortMidiMessage msg) {
        KompleteKontrolExtension.println(" DAW MODE = %d expected = %d", msg.getData2(), expectedProtocol);
        dawModeConfirmed = true;
        if (msg.getData2() != expectedProtocol && expectedProtocol == 4) {
            if (expectedProtocol == 4) {
                host.showPopupNotification("Please update Kontrol S Mk3 firmware to version 2.0 or higher");
            } else {
                host.showPopupNotification("Please update firmware to higher version");
            }
        }
        sendDawInfo(5, 3);
    }
    
    public RelativeHardwarControlBindable createIncDoubleAction(final DoubleConsumer changeAction) {
        return host.createRelativeHardwareControlAdjustmentTarget(changeAction);
    }
    
    public RelativeHardwarControlBindable createIncAction(final IntConsumer changeAction) {
        final HardwareActionBindable incAction = host.createAction(() -> changeAction.accept(1), () -> "+");
        final HardwareActionBindable decAction = host.createAction(() -> changeAction.accept(-1), () -> "-");
        return host.createRelativeHardwareControlStepTarget(incAction, decAction);
    }
    
    public HardwareButton createButton(final String name, final int ccNr, final int index) {
        final HardwareButton hwButton = surface.createHardwareButton(name + "_" + index);
        hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, ccNr, index));
        return hwButton;
    }
    
    public void updateKompleteKontrolInstance(final String instanceParamName) {
        if (lastReportedKKInstance == null || !lastReportedKKInstance.equals(instanceParamName)) {
            sendTextCommand(TextCommand.SELECTED_TRACK, instanceParamName);
            host.scheduleTask(() -> sendTextCommand(TextCommand.SELECTED_TRACK, instanceParamName), 100);
            lastReportedKKInstance = instanceParamName;
        }
    }
    
    public void sendValueCommand(final ValueCommand command, final int index, final boolean value) {
        command.send(midiOut, index, value);
    }
    
    public void sendValueCommand(final ValueCommand command, final int index, final int value) {
        command.send(midiOut, index, value);
    }
    
    public void sendTextCommand(final TextCommand command, final String text) {
        command.send(midiOut, text);
    }
    
    public void sendTextCommand(final TextCommand command, final int index, final String text) {
        command.send(midiOut, index, text);
    }
    
    public void sendLayoutCommand(final LayoutType layoutType) {
        TextCommand.CONFIG.send(midiOut, layoutType == LayoutType.LAUNCHER ? 0 : 1, 0, "track_orientation");
    }
    
    public void sendColor(final int index, final String color) {
        TextCommand.COLOR_UPDATE.send(midiOut, index, color);
    }
    
    public void sendDawInfo(final int major, final int minor) {
        TextCommand.DAW_IDENT.send(midiOut, major, minor, "Bitwig");
    }
    
    public void sendTempo(final double tempo) {
        final byte[] data = DataStringUtil.toTempo10nsData(tempo);
        TextCommand.TEMPO_UPDATE.sendData(midiOut, 0, 0, data);
    }
    
    public void sendBankUpdate(final List<? extends DeviceSelectionTab> bankNames) {
        final String expr = bankNames.stream().map(DeviceSelectionTab::getLayerCode).collect(Collectors.joining("\0"));
        TextCommand.BANK_UPDATE.send(midiOut, expr);
    }
    
    public void sendPresetName(final String name) {
        TextCommand.PRESET_NAME.send(midiOut, name);
    }
    
    public void sendSection(final int index, final String sectionName) {
        TextCommand.TRACK_SECTION.send(midiOut, index, sectionName);
    }
    
    public void sendRemoteState(final int index, final int type, final String name) {
        TextCommand.PARAMETER_UPDATE.send(midiOut, type, index, name);
    }
    
    public void sendPageCount(final int pageCount, final int pageIndex) {
        ValueCommand.PAGE_COUNT_INDEX.send(midiOut, pageIndex, pageCount);
    }
    
    public void sendParamValue(final int index, final String value) {
        TextCommand.PARAMETER_VALUE.send(midiOut, index, value);
    }
    
    public void sendSelectionIndex(final int index, final int[] subindexes) {
        ValueCommand.SELECTION_INDEX.send(midiOut, index, subindexes);
    }
    
    public void sendLedUpdate(final CcAssignment assignment, final int value) {
        midiOut.sendMidi(0xBF, assignment.getStateId(), value);
    }
    
    public void sendLedUpdate(final int code, final int value) {
        midiOut.sendMidi(0xBF, code, value);
    }
    
    public void sendVolumeValue(final int index, final byte value) {
        midiOut.sendMidi(0xBF, 0x50 + index, value);
    }
    
    public void sendPanValue(final int index, final int v) {
        midiOut.sendMidi(0xBF, 0x58 + index, v);
    }
    
    public void updateVuLeft(final int index, final byte value) {
        trackLevelMeterCommand.updateLeft(index, value);
    }
    
    public void updateVuRight(final int index, final byte value) {
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
    
    
    public void updateParameterValue(final int index, final int value) {
        midiOut.sendMidi(0xBF, 0x70 + index, value);
    }
    
}
