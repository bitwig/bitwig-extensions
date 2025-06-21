package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;

public class PadSlot {
    private final DrumPad pad;
    private final int index;
    private final Device device;
    private RgbState color;
    private RgbState colorOff;
    private boolean selected;
    
    public PadSlot(final int index, final DrumPad pad) {
        this.index = index;
        this.pad = pad;
        final DeviceBank deviceBank = pad.createDeviceBank(1);
        device = deviceBank.getDevice(0);
    }
    
    public void setColor(final RgbState color) {
        this.color = color;
        this.colorOff = color.dim();
    }
    
    public RgbState getColor() {
        return color;
    }
    
    public RgbState getColorOff() {
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
