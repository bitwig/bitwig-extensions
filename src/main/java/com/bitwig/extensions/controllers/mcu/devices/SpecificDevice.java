package com.bitwig.extensions.controllers.mcu.devices;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extensions.controllers.mcu.CursorDeviceControl;

/**
 * Fully Customized control of the Bitwig EQ+ Device Missing Parameters:
 * <p>
 * OUTPUT_GAIN GLOBAL_SHIFT BAND SOLO
 * <p>
 * ADAPTIVE_Q DECIBEL_RANGE
 */
public abstract class SpecificDevice implements DeviceManager {
    
    private final SpecialDevices deviceType;
    private final List<Runnable> updateListeners = new ArrayList<>();
    protected final SpecificBitwigDevice bitwigDevice;
    protected int pageIndex = 0;
    protected final DeviceTypeFollower deviceFollower;
    protected final CursorDeviceControl cursorDeviceControl;
    protected final List<ParamPageSlot> pageSlots;
    
    public SpecificDevice(final SpecialDevices type, final CursorDeviceControl cursorDeviceControl,
        final DeviceTypeFollower deviceFollower) {
        this.deviceType = type;
        this.cursorDeviceControl = cursorDeviceControl;
        final PinnableCursorDevice cursorDevice = cursorDeviceControl.getCursorDevice();
        bitwigDevice = cursorDevice.createSpecificBitwigDevice(type.getUuid());
        this.deviceFollower = deviceFollower;
        this.pageSlots = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            this.pageSlots.add(new ParamPageSlot(i, this));
        }
    }
    
    @Override
    public void disableInfo() {
        //infoSource = null;
    }
    
    public void navigateDeviceParameters(final int direction) {
        navigateToDeviceParameters(pageIndex + direction);
    }
    
    public void navigateToDeviceParameters(final int index) {
        if (index >= 0 && index < getPageCount()) {
            pageIndex = index;
            this.applyPageValues(pageIndex);
            this.pageSlots.forEach(ParamPageSlot::update);
            updateListeners.forEach(Runnable::run);
        }
    }
    
    public abstract void applyPageValues(int page);
    
    public ParamPageSlot getParamPageSlot(final int index) {
        return pageSlots.get(index);
    }
    
    public abstract String getDeviceInfo();
    
    public abstract String getPageInfo();
    
    public int getCurrentPage() {
        return pageIndex;
    }
    
    @Override
    public boolean isSpecificDevicePresent() {
        return deviceFollower.getFocusDevice().exists().get();
    }
    
    public BooleanValue getExists() {
        return deviceFollower.getFocusDevice().exists();
    }
    
    @Override
    public DeviceTypeFollower getDeviceFollower() {
        return deviceFollower;
    }
    
    public void addUpdateListeners(final Runnable callback) {
        updateListeners.add(callback);
    }
    
    public void addExistChangeListener(final BooleanValueChangedCallback callback) {
        deviceFollower.getFocusDevice().exists().addValueObserver(callback);
    }
    
    public void insertDevice() {
        deviceFollower.getCurrentDevice()//
            .afterDeviceInsertionPoint() //
            .insertBitwigDevice(deviceType.getUuid());
    }
    
}
