package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceLayer;

class LayerSlot {
    private final DeviceLayer layer;
    private final DeviceBank bank;
    private final Device device;
    private final int index;
    private boolean exists;
    private String name;
    private boolean deviceExists;
    private final int layerCount = 0;
    
    public LayerSlot(final int index, final DeviceLayer layer) {
        this.layer = layer;
        this.index = index;
        this.bank = layer.createDeviceBank(1);
        this.device = bank.getDevice(0);
        this.device.exists().addValueObserver(exists -> this.deviceExists = exists);
        this.layer.exists().addValueObserver(this::handleExists);
        this.layer.name().addValueObserver(this::handleName);
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
    
    public boolean isDeviceExists() {
        return deviceExists;
    }
    
    public void setDeviceExists(final boolean deviceExists) {
        this.deviceExists = deviceExists;
    }
}
