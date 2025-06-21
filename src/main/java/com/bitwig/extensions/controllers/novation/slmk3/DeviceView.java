package com.bitwig.extensions.controllers.novation.slmk3;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceSlot;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.ValueObject;

public class DeviceView {
    private final StringValue name;
    private final BasicStringValue pageName = new BasicStringValue("");
    private String[] pages;
    private String[] slotNames;
    private int pageIndex = -1;
    private final int index;
    private DeviceType deviceType = DeviceType.NOTE_EFFECT;
    private boolean exists;
    private final ValueObject<SlRgbState> deviceColor = new ValueObject<>(SlRgbState.OFF);
    private final BooleanValue selected;
    private final Device device;
    private final DeviceSlot cursorSlot;
    private final DeviceBank deviceBank;
    private final PinnableCursorDevice cursorDevice;
    
    public DeviceView(final int index, final DeviceBank deviceBank, final PinnableCursorDevice cursorDevice) {
        this.device = deviceBank.getItemAt(index);
        this.deviceBank = deviceBank;
        this.name = device.name();
        device.slotNames().addValueObserver(slotNames -> this.slotNames = slotNames);
        device.hasLayers().markInterested();
        device.hasDrumPads().markInterested();
        device.hasSlots().markInterested();
        device.exists().markInterested();
        this.index = index;
        this.cursorDevice = cursorDevice;
        selected = cursorDevice.createEqualsValue(device);
        this.cursorSlot = device.getCursorSlot();
        final CursorRemoteControlsPage remotes = device.createCursorRemoteControlsPage(8);
        device.name().markInterested();
        remotes.pageNames().addValueObserver(pages -> {
            this.pages = pages;
            updatePagesName();
        });
        remotes.selectedPageIndex().addValueObserver(pageIndex -> {
            this.pageIndex = pageIndex;
            updatePagesName();
        });
        device.deviceType().addValueObserver(deviceType -> {
            this.deviceType = DeviceType.toType(deviceType);
            updateDeviceColor();
        });
        device.exists().addValueObserver(exists -> {
            this.exists = exists;
            updatePagesName();
            updateDeviceColor();
        });
        // Enabling devices
    }
    
    private void updatePagesName() {
        if (exists && this.pages != null && pageIndex >= 0 && pageIndex < this.pages.length) {
            pageName.set(this.pages[pageIndex]);
        } else {
            pageName.set("");
        }
    }
    
    private void updateDeviceColor() {
        if (exists && deviceType != null) {
            deviceColor.set(deviceType.getColor());
        } else {
            deviceColor.set(SlRgbState.OFF);
        }
    }
    
    public BasicStringValue getPageName() {
        return pageName;
    }
    
    public StringValue getName() {
        return name;
    }
    
    public ValueObject<SlRgbState> getDeviceColor() {
        return deviceColor;
    }
    
    public BooleanValue getSelected() {
        return selected;
    }
    
    public void select() {
        device.selectInEditor();
    }
    
    public void delete() {
        device.deleteObject();
    }
    
    public void duplicate() {
        device.afterDeviceInsertionPoint().copyDevices(device);
    }
    
    public void selectNested() {
        if (device.hasDrumPads().get()) {
        }
    }
    
    public boolean isDrumDevice() {
        return device.hasDrumPads().get() && device.exists().get();
    }
    
    public void selectKeyPad(final int note) {
        cursorDevice.selectDevice(device);
        cursorDevice.selectFirstInKeyPad(note);
    }
    
    public boolean isSelected() {
        return selected.get();
    }
}
