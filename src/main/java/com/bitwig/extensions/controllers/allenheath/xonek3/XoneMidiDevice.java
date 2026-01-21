package com.bitwig.extensions.controllers.allenheath.xonek3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.ColorIndexCell;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.XoneRgbColor;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneAssignButton;
import com.bitwig.extensions.framework.values.MidiStatus;

public class XoneMidiDevice {
    public static final String IN_SYSEX_SETUP_HEADER = "f000001a5015";
    
    private final int deviceIndex;
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final List<ColorIndexCell> colorIndexCells = new ArrayList<>();
    private final XoneMidiProcessor.InternalRgbColor[] colorStore = new XoneMidiProcessor.InternalRgbColor[34];
    private final int layerMode = 0;
    private final List<XoneAssignButton> assignButtons = new ArrayList<>();
    
    private final byte[] ledUpdateData = new byte[] {
        (byte) 0xF0, 0x00, 0x00, 0x1A, 0x50, 0x15, 0x04, 0x7F, 0x7F, 0x7F, 0x10, 0x00, 0x7F, 0x7F, 0x00, 0x00, 0x00,
        0x7F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF7
    };
    
    private final byte[] globalLedUpdateData = new byte[] {
        (byte) 0xF0, 0x00, 0x00, 0x1A, 0x50, 0x15, 0x00, 0x0E, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, (byte) 0xF7
    };
    
    private final byte[] globalLedUpdateDataOverride = new byte[] {
        (byte) 0xF0, 0x00, 0x00, 0x1A, 0x50, 0x15, 0x00, 0x0E, 0x7F, (byte) 0xF7
    };
    
    private final byte[] switchUpdateData = new byte[] {
        (byte) 0xF0, 0x00, 0x00, 0x1A, 0x50, 0x15, 0x01,  //
        0x7F, // 07 - type = index
        0x7F, // 08 - layer
        0x10, // 09 - channel
        0x00, // 10 - Mode always Gate
        0x01, // 11 - Command = 0x1 Chromatic Note
        0x7F, // 12 - MIDI/CC Nr
        0x00, 0x00, // 14 - Min
        0x00, 0x7F, // 16 - Max
        0x00, 0x00, // 18 - Step
        0x00, 0x00, 0x00, (byte) 0xF7
    };
    
    private final byte[] analogUpdateData = new byte[] {
        (byte) 0xF0, 0x00, 0x00, 0x1A, 0x50, 0x15, 0x02,  //
        0x7F, // 07 - type = index
        0x7F, // 08 - layer
        0x10, // 09 - channel
        0x00, // 10 - Curve -> Complicated
        0x05, // 11 - Command = 0x5 CC Value
        0x7F, // 12 - CC Nr
        0x00, 0x00, // 14 - Min
        0x00, 0x7F, // 16 - Max
        0x00, 0x00, // 18 - Step
        0x00, 0x00, 0x00, (byte) 0xF7
    };
    
    private final byte[] encoderUpdateData = new byte[] {
        (byte) 0xF0, 0x00, 0x00, 0x1A, 0x50, 0x15, 0x03,  //
        0x7F, // 07 - type = index
        0x7F, // 08 - layer
        0x10, // 09 - channel
        0x00, // 10 - Mode -> 0x00 always Relative
        0x05, // 11 - Command = 0x5 CC Value
        0x7F, // 12 - CC Nr
        0x00, 0x00, // 14 - Min
        0x00, 0x7F, // 16 - Max
        0x00, 0x00, // 18 - Step
        0x00, 0x00, 0x00, (byte) 0xF7
    };
    
    
    public XoneMidiDevice(final int deviceIndex, final MidiIn midiIn, final MidiOut midiOut) {
        this.midiIn = midiIn;
        this.midiOut = midiOut;
        this.deviceIndex = deviceIndex;
        midiIn.setSysexCallback(this::handleSysEx);
        for (int i = 0; i < colorStore.length; i++) {
            colorStore[i] = new XoneMidiProcessor.InternalRgbColor();
        }
        for (int layer = 0; layer < 2; layer++) {
            for (int i = 0; i < 34; i++) {
                colorIndexCells.add(new ColorIndexCell(layer + 1, i));
            }
        }
    }
    
    public int getDeviceIndex() {
        return deviceIndex;
    }
    
    public void init() {
        midiOut.sendSysex("F0 00 00 1A 50 15 00 0D 00 F7"); // Turn Latching Layers off
        midiOut.sendSysex("F0 00 00 1A 50 15 00 05 0E F7"); // Global Channel to 15
    }
    
    public void setAssignButtons(final List<XoneAssignButton> assignButtons) {
        this.assignButtons.clear();
        this.assignButtons.addAll(assignButtons);
        for (final XoneAssignButton button : assignButtons) {
            getColorCell(button.getLayerIndex(), button.getLedIndex()).ifPresent(
                cell -> cell.setNoteCcValue(button.getMidiNr()));
        }
    }
    
    private void handleMidiIn(final int status, final int data1, final int data2) {
        XoneK3ControllerExtension.println(" MIDI in %02X %02X %02X", status, data1, data2);
    }
    
    private void handleSysEx(final String data) {
        if (data.startsWith(IN_SYSEX_SETUP_HEADER)) {
            readLedConfiguration(data);
        } else if (data.contains(IN_SYSEX_SETUP_HEADER)) {
            final String ext = data.substring(data.indexOf(IN_SYSEX_SETUP_HEADER));
            readLedConfiguration(ext);
        } else {
            XoneK3ControllerExtension.println(" CODE = %s", data);
        }
    }
    
    private void readLedConfiguration(final String data) {
        final int component = extract(data, 6);
        final int elementIndex = extract(data, 7);
        final int layerIndex = extract(data, 8);
        if (component == 4 && layerIndex > 0 && layerIndex < 3 && elementIndex < 34) {
            getColorCell(layerIndex - 1, elementIndex).ifPresent(cell -> {
                final int colorPaletteIndex = extract(data, 9);
                cell.setColorValue(colorPaletteIndex);
                cell.setColor(XoneRgbColor.getPaletteColor(colorPaletteIndex));
            });
        }
    }
    
    private Optional<ColorIndexCell> getColorCell(final int layer, final int ledIndex) {
        final int index = layer * 34 + ledIndex;
        if (index < colorIndexCells.size()) {
            return Optional.of(colorIndexCells.get(index));
        }
        return Optional.empty();
    }
    
    
    private int extract(final String data, final int byteIndex) {
        if (byteIndex * 2 + 2 < data.length()) {
            return Integer.parseInt(data.substring(byteIndex * 2, byteIndex * 2 + 2), 16);
        }
        return -1;
    }
    
    public void configureLed(final int index, final int layer, final int color, final MidiStatus midiStatus,
        final int noteCcNr) {
        ledUpdateData[7] = (byte) (index & 0x7F);
        ledUpdateData[8] = (byte) (layer & 0x7F);
        ledUpdateData[9] = (byte) (color & 0x7F);
        ledUpdateData[12] = (byte) (midiStatus == MidiStatus.NOTE_ON ? 0x01 : 0x05);
        ledUpdateData[13] = (byte) (noteCcNr & 0x7F);
        midiOut.sendSysex(ledUpdateData);
        pause(3);
    }
    
    public void updateLed(final int index, final int red, final int green, final int blue, final int brightness) {
        colorStore[index].set(red, green, blue, brightness);
        if (layerMode == 0) {
            sendColor(index, colorStore[index]);
        }
    }
    
    public void sendColor(final int index, final XoneRgbColor color, final int brightness) {
        globalLedUpdateData[8] = (byte) (index & 0x7F);
        globalLedUpdateData[9] = (byte) (color.getRed() & 0x7F);
        globalLedUpdateData[10] = (byte) (color.getGreen() & 0x7F);
        globalLedUpdateData[11] = (byte) (color.getBlue() & 0x7F);
        globalLedUpdateData[12] = (byte) (brightness & 0x7F);
        midiOut.sendSysex(globalLedUpdateData);
        pause(3);
    }
    
    public void sendColor(final int index, final XoneMidiProcessor.InternalRgbColor color) {
        globalLedUpdateData[8] = (byte) (index & 0x7F);
        globalLedUpdateData[9] = (byte) (color.red & 0x7F);
        globalLedUpdateData[10] = (byte) (color.green & 0x7F);
        globalLedUpdateData[11] = (byte) (color.blue & 0x7F);
        globalLedUpdateData[12] = (byte) (color.brightness & 0x7F);
        midiOut.sendSysex(globalLedUpdateData);
        pause(3);
    }
    
    public void clearLedOverride(final int index) {
        globalLedUpdateDataOverride[8] = (byte) (index & 0x7F);
        midiOut.sendSysex(globalLedUpdateDataOverride);
    }
    
    public MidiIn getMidiIn() {
        return midiIn;
    }
    
    public void sendMidi(final int status, final int val1, final int val2) {
        midiOut.sendMidi(status, val1, val2);
    }
    
    public void updateAssignLed(final int midiStatus, final int midiNr, final int layerIndex, final int ledIndex,
        final boolean isOn) {
        sendMidi(midiStatus, midiNr, isOn ? 0x7F : 0x00);
        getColorCell(layerIndex, ledIndex).ifPresent(cell -> {
            cell.setOn(isOn);
            if (cell.getLayer() == layerMode) {
                sendColor(cell.getIndex(), isOn ? cell.getColor() : XoneRgbColor.OFF, 0x20);
            }
        });
    }
    
    
    private static void pause(final int mstime) {
        try {
            Thread.sleep(mstime);
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
}
