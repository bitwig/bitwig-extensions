package com.bitwig.extensions.controllers.mcu.devices;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mcu.CursorDeviceControl;
import com.bitwig.extensions.controllers.mcu.VPotMode;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class DeviceTypeFollower {
    private final DeviceBank deviceBank;
    private final VPotMode potMode;
    private final Device focusDevice;
    private final CursorDeviceControl cursorDeviceControl;
    private final BooleanValue cursorOnDevice;
    private int chainIndex = -1;

    public DeviceTypeFollower(final CursorDeviceControl cursorDeviceControl, final DeviceMatcher matcher,
                              final VPotMode potMode) {
        this.cursorDeviceControl = cursorDeviceControl;
        final CursorTrack cursorTrack = cursorDeviceControl.getCursorTrack();
        deviceBank = cursorTrack.createDeviceBank(1);
        this.potMode = potMode;
        deviceBank.setDeviceMatcher(matcher);

        focusDevice = deviceBank.getItemAt(0);
        focusDevice.exists().markInterested();
        focusDevice.name().markInterested();

        final PinnableCursorDevice cursorDevice = cursorDeviceControl.getCursorDevice();
        if (potMode.getAssign() == VPotMode.BitwigType.DEVICE) {
            final BooleanValueObject matchesTyp = new BooleanValueObject();
            cursorOnDevice = matchesTyp;
            cursorDevice.deviceType().addValueObserver(type -> {
                matchesTyp.set(type.equals(potMode.getTypeName()));
                if (matchesTyp.get()) {
                    chainIndex = cursorDevice.position().get();
                }
            });
        } else {
            cursorOnDevice = focusDevice.createEqualsValue(cursorDevice);
            cursorOnDevice.addValueObserver(equalsCursor -> {
                if (equalsCursor) {
                    chainIndex = cursorDevice.position().get();
                }
            });
        }
        cursorDevice.position().addValueObserver(position -> {
            if (cursorOnDevice.get()) {
                chainIndex = position;
            }
        });
    }

    public void addOnCursorListener(final BooleanValueChangedCallback booleanValueChangedCallback) {
        cursorOnDevice.addValueObserver(booleanValueChangedCallback);
    }

    public Device getFocusDevice() {
        return focusDevice;
    }

    public Device getCurrentDevice() {
        return cursorDeviceControl.getCursorDevice();
    }

    public void addNewDeviceAfter() {
        if (focusDevice.exists().get()) {
            focusDevice.afterDeviceInsertionPoint().browse();
        } else {
            deviceBank.browseToInsertDevice(0);
        }
    }

    public void addNewDeviceBefore() {
        if (focusDevice.exists().get()) {
            focusDevice.beforeDeviceInsertionPoint().browse();
        } else {
            deviceBank.browseToInsertDevice(0);
        }
    }

    public void initiateBrowsing() {
        if (focusDevice.exists().get()) {
            focusDevice.replaceDeviceInsertionPoint().browse();
        } else {
            deviceBank.browseToInsertDevice(0);
        }
    }

    public void ensurePosition() {
        final int currentDiveChainLocation = cursorDeviceControl.getCursorDevice().position().get();
        if (!cursorOnDevice.get()) {
            cursorDeviceControl.getCursorDevice().selectDevice(focusDevice);
        }
    }

}
