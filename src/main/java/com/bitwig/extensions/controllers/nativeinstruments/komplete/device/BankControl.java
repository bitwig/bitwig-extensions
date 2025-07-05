package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.KompleteKontrolExtension;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;

public class BankControl {
    
    private final DeviceBank deviceBank;
    private final MidiProcessor midiProcessor;
    private final List<DeviceSlot> devices = new ArrayList<>();
    
    private final DeviceControl deviceControl;
    private final PinnableCursorDevice cursorDevice;
    private boolean nested;
    private int currentDeviceIndex = 0;
    private final ParentTab parentNavTab;
    private final DeviceSlot trackDevice;
    
    private final boolean usesTrackRemotes = true;
    private final DeviceSlot projectDevice;
    
    private class ParentTab implements DeviceSelectionTab {
        private String name;
        
        public void setName(final String name) {
            this.name = name;
        }
        
        @Override
        public String getLayerCode() {
            return "[<< Parent]";
        }
    }
    
    public enum Focus {
        PROJECT,
        TRACK,
        DEVICE
    }
    
    public BankControl(final PinnableCursorDevice cursorDevice, final MidiProcessor midiProcessor,
        final DeviceControl deviceControl) {
        this.deviceBank = cursorDevice.deviceChain().createDeviceBank(64);
        this.midiProcessor = midiProcessor;
        this.deviceControl = deviceControl;
        this.cursorDevice = cursorDevice;
        this.parentNavTab = new ParentTab();
        cursorDevice.position().addValueObserver(this::handleCursorDevicePosition);
        this.trackDevice = new DeviceSlot(-1, "Track-Remotes", this);
        this.projectDevice = new DeviceSlot(-2, "Proj-Remotes", this);
        for (int i = 0; i < deviceBank.getSizeOfBank(); i++) {
            final Device device = deviceBank.getDevice(i);
            final DeviceSlot slot = new DeviceSlot(i, device, this);
            devices.add(slot);
            device.name().addValueObserver(name -> handleNameChange(slot, name));
            device.exists().addValueObserver(exists -> handleExistChange(slot, exists));
            device.hasLayers().addValueObserver(hasLayers -> handleHasLayers(slot, hasLayers));
            device.hasDrumPads().addValueObserver(hasPads -> handleHasDrumPads(slot, hasPads));
        }
        cursorDevice.exists().markInterested();
        cursorDevice.isNested().addValueObserver(this::handleNested);
    }
    
    private void handleNested(final boolean nested) {
        this.nested = nested;
        deviceControl.triggerUpdateAction();
    }
    
    public int getSelectionIndex() {
        return currentDeviceIndex + getIndexOffset(); //  (nested ? 1 : 0);
    }
    
    public List<? extends DeviceSelectionTab> getBankConfig() {
        final List<DeviceSelectionTab> elements = new ArrayList<>();
        if (nested) {
            elements.add(parentNavTab);
        } else if (usesTrackRemotes) {
            elements.add(projectDevice);
            elements.add(trackDevice);
        }
        devices.stream().filter(DeviceSlot::isExists).forEach(elements::add);
        return elements;
    }
    
    private void handleHasDrumPads(final DeviceSlot slot, final boolean hasPads) {
        slot.setHasDrumPads(hasPads);
        deviceControl.triggerUpdateAction();
    }
    
    private void handleHasLayers(final DeviceSlot slot, final boolean hasLayers) {
        slot.setHasLayers(hasLayers);
        deviceControl.triggerUpdateAction();
    }
    
    private void handleNameChange(final DeviceSlot slot, final String name) {
        slot.setName(name);
        deviceControl.triggerUpdateAction();
    }
    
    private void handleExistChange(final DeviceSlot slot, final boolean exists) {
        slot.setExists(exists);
        deviceControl.triggerUpdateAction();
    }
    
    private void handleCursorDevicePosition(final int position) {
        if (position < 0 || position > 64) {
            return;
        }
        currentDeviceIndex = position;
        final DeviceSlot slot = devices.get(position);
        final int index = slot.getIndex() + getIndexOffset();
        midiProcessor.sendSelectionIndex(index, new int[0]);
        
        slot.setSelected(true);
    }
    
    private int getIndexOffset() {
        return nested ? 1 : (usesTrackRemotes ? 2 : 1);
    }
    
    public void select(final int[] selectionPath) {
        if (selectionPath.length == 0) {
            return;
        }
        int index = selectionPath[0];
        if (nested && index == 0) {
            cursorDevice.selectParent();
            return;
        } else if (nested) {
            index--;
        } else if (usesTrackRemotes) {
            if (index == 0) {
                KompleteKontrolExtension.println(" Select project remotes");
                return;
            }
            if (index == 1) {
                KompleteKontrolExtension.println(" Select track remotes");
                return;
            } else {
                index -= 2;
            }
        }
        
        
        final DeviceSlot slot = devices.get(index);
        if (selectionPath.length == 1) {
            slot.select();
            if (slot.isSelected()) {
                if (slot.hasLayers()) {
                    slot.toggleExpanded();
                }
                deviceControl.triggerUpdateAction();
            } else {
                devices.forEach(d -> d.setSelected(false));
                //devices.forEach(d -> d.setExpanded(false));
                slot.setSelected(true);
                deviceControl.triggerUpdateAction();
            }
        } else {
            final Device device = slot.selectLayer(selectionPath[1]);
            cursorDevice.selectDevice(device);
        }
    }
}
