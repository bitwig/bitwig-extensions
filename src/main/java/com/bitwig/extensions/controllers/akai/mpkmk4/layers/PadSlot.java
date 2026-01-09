package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColor;

public class PadSlot {
    private final DrumPad pad;
    private final int index;
    private final Device deviceItem;
    private MpkColor color = MpkColor.OFF;
    private boolean isSelected;
    private boolean isBaseNote;
    private boolean playing;
    
    public PadSlot(final int index, final DrumPad pad) {
        this.pad = pad;
        this.index = index;
        pad.color().addValueObserver((r, g, b) -> color = MpkColor.getColor(r, g, b));
        pad.name().markInterested();
        final DeviceBank deviceBank = pad.createDeviceBank(1);
        deviceItem = deviceBank.getDevice(0);
    }
    
    public MpkColor getColor() {
        return color;
    }
    
    public Device getDeviceItem() {
        return deviceItem;
    }
    
    public DrumPad getPad() {
        return pad;
    }
    
    public String getName() {
        return pad.name().get();
    }
    
    public boolean isPlaying() {
        return playing;
    }
    
    public void setColor(final MpkColor color) {
        this.color = color;
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public void setSelected(final boolean selected) {
        isSelected = selected;
    }
    
    public boolean isBaseNote() {
        return isBaseNote;
    }
    
    public void setBaseNote(final boolean baseNote) {
        isBaseNote = baseNote;
    }
    
    public void setPlaying(final boolean playing) {
        this.playing = playing;
    }
    
    public int getIndex() {
        return index;
    }
}
