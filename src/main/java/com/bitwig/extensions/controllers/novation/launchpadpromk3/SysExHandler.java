package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.ControlMode;

public class SysExHandler {
    private static final String SYSEX_HEADER = "F0 00 20 29 02 0E ";
    private static final String MODE_CHANGE = SYSEX_HEADER + "00 %02X %02X 00 F7";
    private static final String DAW_MODE_CMD = SYSEX_HEADER + "10 %02X F7";
    private static final String LAYOUT_REQUEST = SYSEX_HEADER + "00 F7";
    private static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    private static final String PRINT_TO_CLIP_ENABLE = "F0 00 20 29 02 0E 18 %02X F7";
    private static final String FADER_SET = "%02X %02X %02X %02X ";
    private static final String DAW_FADER = "F0 00 20 29 02 0E 00 %02X %02X 00 F7";
    
    private static final String DAW_MODE = SYSEX_HEADER + "0E %02X F7";
    private static final String NOTE_MODE = SYSEX_HEADER + "00 %02X F7";
    
    private static final String DEVICE_REPLY = "f07e00060200202923010000000";
    
    private static final String MODE_CHANGE_REPLY = "f0002029020e00";
    private static final String PTC_HEAD = "f0002029020e03";
    
    private final MidiOut midiOut;
    
    private LpBaseMode mode = LpBaseMode.SESSION;
    private int page = 0;
    private PrintToClipData printToClip = null;
    public List<Consumer<PrintToClipData>> printDataListeners = new ArrayList<>();
    public List<ModeChangeListener> modeChangeListeners = new ArrayList<>();
    
    public interface ModeChangeListener {
        void handleModeChange(LpBaseMode mode, int page);
    }
    
    public SysExHandler(final MidiProcessor midiProcessor) {
        midiOut = midiProcessor.getMidiOut();
    }
    
    public void changeMode(final LpBaseMode newMode, final int page) {
        if (mode == newMode) {
            return;
        }
        mode = newMode;
        setLayout(mode, page);
    }
    
    public void setFaderBank(final int orient, final ControlMode mode, final int[] colorIndex) {
        if (mode == ControlMode.NONE) {
            return;
        }
        final StringBuilder sysEx = new StringBuilder(SYSEX_HEADER);
        sysEx.append(String.format("01 %02X %02X ", mode.getBankId(), orient));
        for (int i = 0; i < 8; i++) {
            sysEx.append(String.format(FADER_SET, i, mode.getType(), mode.getCcNr() + i, colorIndex[i]));
        }
        sysEx.append("F7");
        midiOut.sendSysex(sysEx.toString());
        midiOut.sendSysex("F0 00 20 29 02 0E 00 0D F7");
    }
    
    public void changeNoteMode(final boolean drumMode) {
        midiOut.sendSysex(String.format(NOTE_MODE, drumMode ? 2 : 1));
    }
    
    public void enableFaderMode(final ControlMode mode, final boolean active) {
        if (mode.hasFaders()) {
            midiOut.sendSysex(String.format(DAW_FADER, active ? 1 : 0, mode.getBankId()));
        }
    }
    
    public LpBaseMode getMode() {
        return mode;
    }
    
    public void setLayout(final LpBaseMode mode, final int page) {
        midiOut.sendSysex(String.format(MODE_CHANGE, mode.getSysExId(), page));
    }
    
    public void enableClipPrint(final boolean enabled) {
        midiOut.sendSysex(String.format(PRINT_TO_CLIP_ENABLE, enabled ? 1 : 0));
    }
    
    public void setDawMode(final boolean on) {
        midiOut.sendSysex(String.format(DAW_MODE_CMD, on ? 1 : 0));
    }
    
    public void requestLayout() {
        midiOut.sendSysex(LAYOUT_REQUEST);
    }
    
    public void deviceInquiry() {
        midiOut.sendSysex(DEVICE_INQUIRY);
    }
    
    public void handleSysEx(final String sysEx) {
        if (sysEx.startsWith(DEVICE_REPLY)) {
            LaunchpadProMk3ControllerExtension.println(" >> Device Reply ");
            setDawMode(true);
            setLayout(LpBaseMode.SESSION, 0);
            //            int v1 = getValue(sysEx, 12);
        } else if (sysEx.startsWith(MODE_CHANGE_REPLY)) {
            final int page = getValue(sysEx, 8);
            final LpBaseMode detectMode = LpBaseMode.toMode(getValue(sysEx, 7));
            mode = detectMode;
            this.page = page;
            modeChangeListeners.forEach(listener -> listener.handleModeChange(detectMode, page));
        } else if (sysEx.startsWith(PTC_HEAD)) {
            final String which = sysEx.substring(PTC_HEAD.length(), PTC_HEAD.length() + 2);
            final int type = Integer.parseInt(which, 16);
            if (type == 1) {
                printToClip = new PrintToClipData();
            }
            if (printToClip != null) {
                printToClip.setData(type, sysEx.substring(PTC_HEAD.length() + 2, sysEx.length() - 2));
            }
            if (type == 3) {
                printDataListeners.forEach(listener -> listener.accept(printToClip));
            }
        } else {
            LaunchpadProMk3ControllerExtension.println("Unknown = " + sysEx);
        }
    }
    
    public void addPrintToClipDataListener(final Consumer<PrintToClipData> listener) {
        printDataListeners.add(listener);
    }
    
    public void addModeChangeListener(final ModeChangeListener listener) {
        modeChangeListeners.add(listener);
    }
    
    private int getValue(final String sysExString, final int byteLocation) {
        final int index = byteLocation * 2;
        if (index >= sysExString.length()) {
            return -1;
        }
        final String valueStr = sysExString.substring(index, index + 2);
        return Integer.parseInt(valueStr, 16);
    }
    
}
