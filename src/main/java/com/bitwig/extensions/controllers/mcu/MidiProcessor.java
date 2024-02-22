package com.bitwig.extensions.controllers.mcu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.RelativeHardwareValueMatcher;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.display.ControllerDisplay;
import com.bitwig.extensions.controllers.mcu.display.DisplayPart;
import com.bitwig.extensions.controllers.mcu.display.LcdDisplay;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.Midi;

public class MidiProcessor implements ControllerDisplay {
    private final ControllerHost host;
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final int portIndex;
    private final boolean has2ClickResolution;
    private final int[] lightStatusMap = new int[127];
    private final LcdDisplay upperDisplay;
    private final LcdDisplay lowerDisplay;
    private final BooleanValueObject slidersTouched = new BooleanValueObject();
    private int touchCount = 0;
    private boolean segmentedDisplay = false;
    private long touchReleaseTime = -1;
    private final TimedProcessor timedProcessor;
    
    public MidiProcessor(final Context context, final int portIndex) {
        this.portIndex = portIndex;
        this.host = context.getService(ControllerHost.class);
        this.timedProcessor = context.getService(TimedProcessor.class);
        final ControllerConfig controllerConfig = context.getService(ControllerConfig.class);
        this.has2ClickResolution = controllerConfig.isHas2ClickResolution();
        this.midiIn = host.getMidiInPort(portIndex);
        this.midiOut = host.getMidiOutPort(portIndex);
        this.segmentedDisplay = controllerConfig.isDisplaySegmented();
        
        midiIn.setMidiCallback(this::handleMidiIn);
        midiIn.setSysexCallback(this::handleSysEx);
        
        final SectionType sectionType =
            portIndex == 0 || !controllerConfig.isSingleMainUnit() ? SectionType.MAIN : SectionType.XTENDER;
        
        this.upperDisplay =
            new LcdDisplay(context, portIndex, midiOut, sectionType, DisplayPart.UPPER, controllerConfig);
        this.lowerDisplay =
            controllerConfig.hasLowerDisplay() ? new LcdDisplay(context, portIndex, midiOut, sectionType,
                DisplayPart.LOWER, controllerConfig) : null;
        Arrays.fill(lightStatusMap, -1);
        timedProcessor.addActionListener(() -> {
            if (touchReleaseTime != -1 && (System.currentTimeMillis() - touchReleaseTime) > 400) {
                slidersTouched.set(false);
                touchReleaseTime = -1;
            }
        });
    }
    
    private void handleMidiIn(final int status, final int data1, final int data2) {
        McuExtension.println("MIDI(%d) => %02X %02X %02X   %03d %03d", portIndex, status, data1, data2, data1, data2);
    }
    
    protected void handleSysEx(final String sysExString) {
        McuExtension.println("SysEx = %s", sysExString);
    }
    
    public int getPortIndex() {
        return portIndex;
    }
    
    public void handleTouch(final boolean touch) {
        if (touch) {
            if (touchCount == 0 && !slidersTouched.get()) {
                slidersTouched.set(true);
            }
            touchReleaseTime = -1;
            touchCount++;
        } else {
            touchCount = Math.max(0, touchCount - 1);
            if (touchCount == 0) {
                touchReleaseTime = System.currentTimeMillis();
            }
        }
    }
    
    public void exit() {
        upperDisplay.exitMessage();
        if (lowerDisplay != null) {
            lowerDisplay.clearAll();
        }
    }
    
    public void sendMidi(final int status, final int data1, final int data2) {
        midiOut.sendMidi(status, data1, data2);
    }
    
    public void sendLedLightStatus(final int noteNr, final int value) {
        lightStatusMap[noteNr] = value; // TODO Consider Midi Channels
        midiOut.sendMidi(Midi.NOTE_ON, noteNr, value);
    }
    
    public void attachNoteOnOffMatcher(final HardwareButton button, final int channel, final int note) {
        button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(channel, note));
        button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, note));
    }
    
    public void attachPitchBendSliderValue(final HardwareSlider slider, final int channel) {
        slider.setAdjustValueMatcher(midiIn.createAbsolutePitchBendValueMatcher(channel));
    }
    
    public RelativeHardwareValueMatcher createAcceleratedMatcher(final int ccNr) {
        return midiIn.createRelativeSignedBitCCValueMatcher(0x0, ccNr, 200);
    }
    
    public RelativeHardwareValueMatcher createNonAcceleratedMatcher(final int ccNr) {
        final RelativeHardwareValueMatcher stepDownMatcher =
            midiIn.createRelativeValueMatcher("(status == 176 && data1 == %d && data2 > 64)".formatted(ccNr), -1);
        final RelativeHardwareValueMatcher stepUpMatcher =
            midiIn.createRelativeValueMatcher("(status == 176 && data1 == %d && data2 < 65)".formatted(ccNr), 1);
        return host.createOrRelativeHardwareValueMatcher(stepDownMatcher, stepUpMatcher);
    }
    
    public void updateIconColors(final int[] colors) {
        final StringBuilder sysEx = new StringBuilder("F0 00 00 68 16 14 ");
        for (int i = 0; i < colors.length; i++) {
            final int red = colors[i] >> 16;
            final int green = colors[i] >> 8 & 0x7F;
            final int blue = colors[i] & 0x7F;
            sysEx.append(String.format("%02x %02x %02x ", red, green, blue));
        }
        sysEx.append("F7");
        midiOut.sendSysex(sysEx.toString());
    }
    
    @Override
    public void sendVuUpdate(final int index, final int value) {
        midiOut.sendMidi(Midi.CHANNEL_AT, index << 4 | value, 0);
    }
    
    @Override
    public void sendMasterVuUpdateL(final int value) {
        midiOut.sendMidi(Midi.CHANNEL_AT | 0x1, value, 0);
    }
    
    @Override
    public void sendMasterVuUpdateR(final int value) {
        midiOut.sendMidi(Midi.CHANNEL_AT | 0x1, 0x10 | value, 0);
    }
    
    public HardwareActionBindable createAction(final Runnable action) {
        return host.createAction(action, null);
    }
    
    public MidiIn getMidiIn() {
        return midiIn;
    }
    
    public MidiOut getMidiOut() {
        return midiOut;
    }
    
    public boolean isHas2ClickResolution() {
        return has2ClickResolution;
    }
    
    public void showText(final DisplayPart part, final int row, final int cell, final String text) {
        if (part == DisplayPart.UPPER) {
            upperDisplay.sendToRow(row, cell, text);
        } else if (part == DisplayPart.LOWER && lowerDisplay != null) {
            lowerDisplay.sendToRow(row, cell, text);
        }
    }
    
    public void showText(final DisplayPart part, final int row, final List<String> texts) {
        if (part == DisplayPart.UPPER) {
            upperDisplay.sendSegmented(row, texts);
        } else if (part == DisplayPart.LOWER && lowerDisplay != null) {
            lowerDisplay.sendSegmented(row, texts);
        }
    }
    
    public void showText(final DisplayPart part, final int row, final String text) {
        if (part == DisplayPart.UPPER) {
            if (segmentedDisplay) {
                final List<String> segments = splitInSegments(text, 6);
                for (int i = 0; i < 8; i++) {
                    final String value = i < segments.size() ? segments.get(i) : "";
                    upperDisplay.sendToRow(row, i, value);
                }
            } else {
                upperDisplay.sendToDisplay(row, text);
            }
        } else if (part == DisplayPart.LOWER && lowerDisplay != null) {
            if (segmentedDisplay) {
                final List<String> segments = splitInSegments(text, 5);
                for (int i = 0; i < 8; i++) {
                    final String value = i < segments.size() ? segments.get(i) : "";
                    lowerDisplay.sendToRow(row, i, value);
                }
            } else {
                lowerDisplay.sendToDisplay(row, text);
            }
        }
    }
    
    private List<String> splitInSegments(final String text, final int maxSegLen) {
        final List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        final String[] split = text.split(" ");
        
        for (final String txt : split) {
            if (txt.length() > 0) {
                if (current.length() == 0) {
                    current.append(txt);
                } else if (current.length() + txt.length() + 1 <= maxSegLen) {
                    current.append(" ");
                    current.append(txt);
                } else {
                    if (current.length() > maxSegLen) {
                        result.add(current.substring(0, maxSegLen));
                        final String restPart = current.substring(maxSegLen, current.length());
                        if (restPart.length() + txt.length() > maxSegLen) {
                            result.add(restPart);
                            current = new StringBuilder(txt);
                        } else {
                            current = new StringBuilder(restPart + " " + txt);
                        }
                    } else {
                        result.add(current.toString());
                        current = new StringBuilder(txt);
                    }
                }
            }
        }
        if (current.length() > 0) {
            if (current.length() > maxSegLen) {
                result.add(current.substring(0, maxSegLen));
                result.add(current.substring(maxSegLen, current.length()));
            } else {
                result.add(current.toString());
            }
        }
        
        return result;
    }
    
    @Override
    public void refresh() {
        if (lowerDisplay != null) {
            lowerDisplay.refreshDisplay();
        }
        if (upperDisplay != null) {
            upperDisplay.refreshDisplay();
        }
    }
    
    @Override
    public void blockUpdate(final DisplayPart part, final int row) {
    }
    
    @Override
    public void enableUpdate(final DisplayPart part, final int row) {
    }
    
    @Override
    public boolean hasLower() {
        return lowerDisplay != null;
    }
    
    public BooleanValueObject getSlidersTouched() {
        return slidersTouched;
    }
}