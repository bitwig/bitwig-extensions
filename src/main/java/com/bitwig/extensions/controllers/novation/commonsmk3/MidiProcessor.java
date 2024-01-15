package com.bitwig.extensions.controllers.novation.commonsmk3;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.Midi;

public class MidiProcessor {
    protected static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    private static final String DAW_MODE = "F0 00 20 29 02 %02X 10 %02X F7";
    private static final String LAYOUT_COMMAND = "F0 00 20 29 02 %02X 00 %02X F7";
    private static final String NOTE_LAYOUT_COMMAND = "F0 00 20 29 02 %02X 0F %02X F7";
    private static final String BUTTON_ID_SYSEX = "F0 00 20 29 02 %02X %02X %02X 01 F7";
    private static final String FADER_SET = "%02X %02X %02X %02X ";
    public static final String FADER_CONFIG = "F0 00 20 29 02 %02X 01 00 %02X ";
    
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    private final ControllerHost host;
    private final int deviceSysExCode;
    private final int sliderValueStatus;
    
    public MidiProcessor(final ControllerHost host, final MidiIn midiIn, final MidiOut midiOut,
        final LaunchpadDeviceConfig config) {
        this.host = host;
        this.midiIn = midiIn;
        this.midiOut = midiOut;
        deviceSysExCode = config.getSysExId();
        sliderValueStatus = config.getSliderValueStatus();
        //DebugMini.println(" MIDI PROC %02X", sliderValueStatus);
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
        host.scheduleTask(this::handlePing, 100);
    }
    
    public void sendMidi(final int status, final int val1, final int val2) {
        midiOut.sendMidi(status, val1, val2);
    }
    
    public void sendToSlider(final int ccNr, final int value) {
        midiOut.sendMidi(sliderValueStatus, ccNr, value);
    }
    
    public void setButtonLed(final int buttonId, final int color) {
        midiOut.sendSysex(String.format(BUTTON_ID_SYSEX, deviceSysExCode, buttonId, color));
    }
    
    public void sendDeviceInquiry() {
        midiOut.sendSysex(DEVICE_INQUIRY);
    }
    
    public void enableDawMode(final boolean active) {
        midiOut.sendSysex(String.format(DAW_MODE, deviceSysExCode, active ? 1 : 0));
    }
    
    public void toLayout(final int layoutId) {
        midiOut.sendSysex(String.format(LAYOUT_COMMAND, deviceSysExCode, layoutId));
    }
    
    public void setNoteModeLayoutX(final int layoutId) {
        final String format = String.format(NOTE_LAYOUT_COMMAND, deviceSysExCode, layoutId);
        midiOut.sendSysex(format);
    }
    
    public void setFaderBank(final int orient, final int[] colorIndex, final boolean uniPolar, final int ccNrOffset) {
        final StringBuilder sysEx = new StringBuilder(String.format(FADER_CONFIG, deviceSysExCode, orient));
        for (int i = 0; i < 8; i++) {
            sysEx.append(String.format(FADER_SET, i, uniPolar ? 0 : 1, ccNrOffset + i, colorIndex[i]));
        }
        sysEx.append("F7");
        midiOut.sendSysex(sysEx.toString());
        toLayout(0x0D);
    }
    
    public void updatePadLed(final InternalHardwareLightState state, final int ccValue) {
        if (state instanceof final RgbState rgbState) {
            switch (rgbState.getState()) {
                case NORMAL:
                    sendMidi(Midi.CC, ccValue, rgbState.getColorIndex());
                    break;
                case FLASHING:
                    sendMidi(Midi.CC, ccValue, rgbState.getAltColor());
                    sendMidi(Midi.CC + 1, ccValue, rgbState.getColorIndex());
                    break;
                case PULSING:
                    sendMidi(Midi.CC + 2, ccValue, rgbState.getColorIndex());
                    break;
            }
        } else {
            sendMidi(Midi.NOTE_ON, ccValue, 0);
        }
    }
    
    public void delayAction(final int delayTime, final Runnable action) {
        this.host.scheduleTask(action, delayTime);
    }
}
