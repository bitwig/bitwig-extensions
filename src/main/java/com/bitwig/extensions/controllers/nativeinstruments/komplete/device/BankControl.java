package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.values.ValueObject;

public class BankControl {
    
    private final MidiProcessor midiProcessor;
    private final List<DeviceSlot> devices = new ArrayList<>();
    
    private final DeviceControl deviceControl;
    private final PinnableCursorDevice cursorDevice;
    private boolean nested;
    private int currentDeviceIndex = 0;
    private final ParentTab parentNavTab;
    
    private final DeviceSlot trackDevice;
    private final DeviceSlot projectDevice;
    
    private boolean usesTrackRemotes = false;
    private final ValueObject<Focus> currentFocus = new ValueObject<>(Focus.DEVICE);
    private boolean trackRemotesPresent;
    private boolean projectRemotesPresent;
    
    private static class ParentTab implements DeviceSelectionTab {
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
        final DeviceBank deviceBank = cursorDevice.deviceChain().createDeviceBank(64);
        this.midiProcessor = midiProcessor;
        this.deviceControl = deviceControl;
        this.cursorDevice = cursorDevice;
        this.parentNavTab = new ParentTab();
        cursorDevice.position().addValueObserver(this::handleCursorDevicePosition);
        this.trackDevice = new DeviceSlot(-1, "Track");
        this.projectDevice = new DeviceSlot(-2, "Project");
        for (int i = 0; i < deviceBank.getSizeOfBank(); i++) {
            final Device device = deviceBank.getDevice(i);
            final DeviceSlot slot = new DeviceSlot(i, device);
            devices.add(slot);
            device.name().addValueObserver(name -> handleNameChange(slot, name));
            device.exists().addValueObserver(exists -> handleExistChange(slot, exists));
            device.hasLayers().addValueObserver(hasLayers -> handleHasLayers(slot, hasLayers));
            device.hasDrumPads().addValueObserver(hasPads -> handleHasDrumPads(slot, hasPads));
        }
        cursorDevice.exists().markInterested();
        cursorDevice.isNested().addValueObserver(this::handleNested);
    }
    
    protected void setTrackRemotesPresent(final boolean present) {
        this.trackRemotesPresent = present;
        if (usesTrackRemotes) {
            if (!present && currentFocus.get() == Focus.TRACK) {
                currentFocus.set(Focus.DEVICE);
            }
            if (!nested) {
                select(Math.max(0, currentDeviceIndex + (present ? 1 : -1)));
            } else {
                deviceControl.triggerUpdateAction();
            }
        }
    }
    
    protected void setProjectRemotesPresent(final boolean present) {
        this.projectRemotesPresent = present;
        if (usesTrackRemotes) {
            if (!present && currentFocus.get() == Focus.PROJECT) {
                currentFocus.set(Focus.DEVICE);
            }
            if (!nested) {
                select(Math.max(0, currentDeviceIndex + (present ? 1 : -1)));
            } else {
                deviceControl.triggerUpdateAction();
            }
        }
    }
    
    public ValueObject<Focus> getCurrentFocus() {
        return currentFocus;
    }
    
    private void handleNested(final boolean nested) {
        this.nested = nested;
        deviceControl.triggerUpdateAction();
    }
    
    public void setUsesTrackRemotes(final boolean usesTrackRemotes) {
        this.usesTrackRemotes = usesTrackRemotes;
        if (!usesTrackRemotes) {
            currentFocus.set(Focus.DEVICE);
        }
        deviceControl.triggerUpdateAction();
    }
    
    public int getSelectionIndex() {
        return switch (currentFocus.get()) {
            case DEVICE -> currentDeviceIndex + getIndexOffset();
            case TRACK -> 1;
            case PROJECT -> 0;
        };
    }
    
    public List<? extends DeviceSelectionTab> getBankConfig() {
        final List<DeviceSelectionTab> elements = new ArrayList<>();
        
        if (nested) {
            elements.add(parentNavTab);
        } else if (usesTrackRemotes) {
            if (projectRemotesPresent) {
                elements.add(projectDevice);
            }
            if (trackRemotesPresent) {
                elements.add(trackDevice);
            }
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
        
        if (currentFocus.get() == Focus.DEVICE) {
            final int index = slot.getIndex() + getIndexOffset();
            midiProcessor.sendSelectionIndex(index, new int[0]);
            slot.setSelected(true);
        } else if (currentFocus.get() == Focus.TRACK) {
            midiProcessor.sendSelectionIndex(projectRemotesPresent ? 1 : 0, new int[0]);
        } else {
            midiProcessor.sendSelectionIndex(0, new int[0]);
        }
    }
    
    private int getIndexOffset() {
        if (nested) {
            return 1;
        }
        if (usesTrackRemotes) {
            return (projectRemotesPresent ? 1 : 0) + (trackRemotesPresent ? 1 : 0);
        }
        return 0;
    }
    
    public void select(final int... selectionPath) {
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
                if (projectRemotesPresent) {
                    currentFocus.set(Focus.PROJECT);
                    midiProcessor.sendSelectionIndex(0, new int[0]);
                    return;
                } else if (trackRemotesPresent) {
                    currentFocus.set(Focus.TRACK);
                    midiProcessor.sendSelectionIndex(0, new int[0]);
                    return;
                }
            }
            if (index == 1 && projectRemotesPresent && trackRemotesPresent) {
                currentFocus.set(Focus.TRACK);
                midiProcessor.sendSelectionIndex(1, new int[0]);
                return;
            }
            index -= getIndexOffset();
        }
        currentFocus.set(Focus.DEVICE);
        
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
