package com.bitwig.extensions.controllers.mcu;

import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorDeviceLayer;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;

public class CursorDeviceControl {
    private final DeviceBank deviceBank;
    private final PinnableCursorDevice cursorDevice;
    
    private final CursorRemoteControlsPage remotes;
    private final CursorTrack cursorTrack;
    private final PinnableCursorDevice primaryDevice;
    
    private final DrumPadBank drumPadBank;
    private final CursorDeviceLayer drumCursor;
    private final DeviceBank drumDeviceBank;
    private final CursorDeviceLayer cursorLayer;
    private final Device cursorLayerItem;
    private final DeviceBank trackDeviceBank;
    
    public CursorDeviceControl(final CursorTrack cursorTrack, final int size, final int totalChannelsAvailable) {
        this.cursorTrack = cursorTrack;
        cursorTrack.trackType().markInterested();
        cursorDevice = cursorTrack.createCursorDevice("main", "mmain", 1, CursorDeviceFollowMode.FOLLOW_SELECTION);
        primaryDevice =
            cursorTrack.createCursorDevice("drumdetection", "Pad Device", 8, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        primaryDevice.hasDrumPads().markInterested();
        primaryDevice.exists().markInterested();
        
        drumPadBank = primaryDevice.createDrumPadBank(totalChannelsAvailable);
        
        drumPadBank.setSkipDisabledItems(false);
        drumCursor = primaryDevice.createCursorLayer();
        
        drumDeviceBank = drumCursor.createDeviceBank(8);
        
        cursorLayer = cursorDevice.createCursorLayer();
        final DeviceBank layerBank = cursorLayer.createDeviceBank(1);
        cursorLayerItem = layerBank.getItemAt(0);
        cursorLayerItem.name().markInterested();
        
        markCursorDevice();
        
        deviceBank = cursorDevice.deviceChain().createDeviceBank(8);
        markDeviceBank(deviceBank);
        trackDeviceBank = cursorTrack.createDeviceBank(8);
        markDeviceBank(trackDeviceBank);
        
        remotes = cursorDevice.createCursorRemoteControlsPage(8);
        remotes.pageCount().markInterested();
        
        for (int i = 0; i < deviceBank.getSizeOfBank(); i++) {
            final Device device = deviceBank.getDevice(i);
            device.deviceType().markInterested();
            device.name().markInterested();
            device.position().markInterested();
        }
        
        cursorDevice.position().addValueObserver(cp -> {
            if (cp >= 0) {
                deviceBank.scrollPosition().set(cp - 1);
            }
        });
        
    }
    
    private void markCursorDevice() {
        cursorDevice.name().markInterested();
        cursorDevice.deviceType().markInterested();
        cursorDevice.isPinned().markInterested();
        cursorDevice.hasDrumPads().markInterested();
        cursorDevice.hasLayers().markInterested();
        cursorDevice.hasSlots().markInterested();
        cursorDevice.slotNames().markInterested();
    }
    
    private void markDeviceBank(final DeviceBank bank) {
        bank.canScrollBackwards().markInterested();
        bank.canScrollForwards().markInterested();
        bank.itemCount().markInterested();
        bank.scrollPosition().markInterested();
    }
    
    public void moveDeviceLeft() {
        final Device previousDevice = deviceBank.getDevice(0);
        previousDevice.beforeDeviceInsertionPoint().moveDevices(cursorDevice);
        cursorDevice.selectPrevious();
        cursorDevice.selectNext();
    }
    
    public void moveDeviceRight() {
        final Device nextDevice = deviceBank.getDevice(2);
        nextDevice.afterDeviceInsertionPoint().moveDevices(cursorDevice);
        cursorDevice.selectPrevious();
        cursorDevice.selectNext();
    }
    
    public CursorDeviceLayer getDrumCursor() {
        return drumCursor;
    }
    
    public DrumPadBank getDrumPadBank() {
        return drumPadBank;
    }
    
    public DeviceBank getDrumDeviceBank() {
        return drumDeviceBank;
    }
    
    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }
    
    public DeviceBank getDeviceBank() {
        return deviceBank;
    }
    
    public DeviceBank getTrackDeviceBank() {
    	return trackDeviceBank;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public CursorRemoteControlsPage getRemotes() {
        return remotes;
    }
    
    public Parameter getParameter(final int index) {
        return remotes.getParameter(index);
    }
    
    public void selectDevice(final Device device) {
        cursorDevice.selectDevice(device);
        cursorDevice.selectInEditor();
    }
    
    public void focusOnDrumDevice() {
        cursorDevice.selectDevice(drumDeviceBank.getDevice(0));
    }
    
    public void focusOnPrimary() {
        cursorDevice.selectDevice(primaryDevice);
    }
    
    public void handleLayerSelection() {
        cursorDevice.selectDevice(cursorLayerItem);
    }
    
    public void navigateNextInLayer() {
        cursorLayer.selectNext();
    }
    
    public void navigatePreviousInLayer() {
        cursorLayer.selectPrevious();
    }
    
    public Device getCursorLayerItem() {
        return cursorLayerItem;
    }
    
    public String getLayerDeviceInfo() {
        if (cursorDevice.hasLayers().get()) {
            return "SEL=" + cursorLayerItem.name().get();
        }
        return "";
    }
    
    public boolean cursorHasDrumPads() {
        if (cursorTrack.trackType().get().equals("Instrument")) {
            return primaryDevice.exists().get() && primaryDevice.hasDrumPads().get();
        }
        return false;
    }
    
    public boolean hasDrumPads() {
        return cursorDevice.hasDrumPads().get();
    }
    
    public void navigateToPage(final int index) {
        if (index < remotes.pageCount().get()) {
            remotes.selectedPageIndex().set(index);
        }
    }
    
    public void navigateToDeviceInChain(final int index) {
        if (index < trackDeviceBank.itemCount().get()) {
            cursorDevice.selectDevice(trackDeviceBank.getDevice(index));
        }
    }
    
    public void insertVst3Device(final String id) {
        cursorDevice.afterDeviceInsertionPoint().insertVST3Device(id);
    }
}
