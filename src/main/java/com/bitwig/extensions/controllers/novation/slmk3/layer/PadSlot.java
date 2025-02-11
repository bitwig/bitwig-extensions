package com.bitwig.extensions.controllers.novation.slmk3.layer;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;

public class PadSlot {
    private final DrumPad pad;
    private final int index;
    private final Device device;
    private SlRgbState color;
    private SlRgbState colorOff;
    private boolean selected;
    
    public PadSlot(final int index, final DrumPad pad) {
        this.index = index;
        this.pad = pad;
        final DeviceBank deviceBank = pad.createDeviceBank(1);
        device = deviceBank.getDevice(0);
    }
    
    public void setColor(final SlRgbState color) {
        this.color = color;
        this.colorOff = color.reduced(40);
    }
    
    public SlRgbState getColor() {
        return color;
    }
    
    public SlRgbState getColorOff() {
        return colorOff;
    }
    
    public void select(final PinnableCursorDevice cursorDevice) {
        pad.selectInEditor();
        cursorDevice.selectDevice(device);
    }
    
    public void setSelected(final boolean selected) {
        this.selected = selected;
    }
    
    public boolean isSelected() {
        return selected;
    }
}
