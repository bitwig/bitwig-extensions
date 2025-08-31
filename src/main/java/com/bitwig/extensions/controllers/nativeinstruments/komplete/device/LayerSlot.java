package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceLayer;

class LayerSlot {
    private final DeviceLayer layer;
    private final Device device;
    private final int index;
    private boolean exists;
    private String name;
    private boolean active;
    
    public LayerSlot(final int index, final DeviceLayer layer) {
        this.layer = layer;
        this.index = index;
        final DeviceBank bank = layer.createDeviceBank(1);
        this.device = bank.getDevice(0);
        this.layer.exists().addValueObserver(this::handleExists);
        this.layer.name().addValueObserver(this::handleName);
        this.layer.isActivated().addValueObserver(this::handleActivated);
    }
    
    private void handleActivated(final boolean activated) {
        this.active = activated;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void toggleActive() {
        this.layer.isActivated().toggle();
    }
    
    public void select() {
        this.layer.selectInEditor();
    }
    
    private void handleName(final String name) {
        this.name = name;
    }
    
    private void handleExists(final boolean exists) {
        this.exists = exists;
    }
    
    public Device getDevice() {
        return device;
    }
    
    public int getIndex() {
        return index;
    }
    
    public boolean isExists() {
        return exists;
    }
    
    public void setExists(final boolean exists) {
        this.exists = exists;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    public String displayName() {
        return active ? name : "[x] %s".formatted(name);
    }
    
}
