package com.bitwig.extensions.controllers.allenheath.xonek3.color;

import com.bitwig.extensions.framework.values.MidiStatus;

public class ColorIndexCell {
    private final byte[] ledUpdateData = new byte[] {
        (byte) 0xF0, 0x00, 0x00, 0x1A, 0x50, 0x15, 0x04, 0x7F, 0x7F, 0x7F, 0x10, 0x00, 0x7F, 0x7F, 0x00, 0x00, 0x00,
        0x7F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF7
    };

    private final int index;
    private final int layer;
    private int colorValue;
    private boolean on;
    private final MidiStatus midiStatus;
    private int noteCcValue = -1;
    private XoneRgbColor color = XoneRgbColor.WHITE_DIM;

    public ColorIndexCell(final int layer, final int index) {
        this.index = index;
        this.layer = layer;
        this.midiStatus = MidiStatus.NOTE_ON;
        ledUpdateData[7] = (byte) (index & 0x7F);
        ledUpdateData[8] = (byte) (layer & 0x7F);
    }

    public MidiStatus getMidiStatus() {
        return midiStatus;
    }

    public int getNoteCcValue() {
        return noteCcValue;
    }

    public void setNoteCcValue(final int noteCcValue) {
        this.noteCcValue = noteCcValue;
    }

    public int getIndex() {
        return index;
    }

    public int getLayer() {
        return layer;
    }

    public int getColorValue() {
        return colorValue;
    }

    public void setColorValue(final int colorValue) {
        this.colorValue = colorValue;
    }

    public boolean isDefined() {
        return noteCcValue != -1;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(final boolean on) {
        this.on = on;
    }

    public XoneRgbColor getColor() {
        return color;
    }

    public void setColor(final XoneRgbColor color) {
        this.color = color;
    }

    public byte[] getLedUpdateData() {
        ledUpdateData[9] = (byte) (colorValue & 0x7F);
        ledUpdateData[12] = (byte) (midiStatus == MidiStatus.NOTE_ON ? 0x01 : 0x05);
        ledUpdateData[13] = (byte) (noteCcValue & 0x7F);
        return ledUpdateData;
    }

}
