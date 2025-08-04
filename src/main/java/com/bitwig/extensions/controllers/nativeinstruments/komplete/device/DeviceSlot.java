package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceLayerBank;

public class DeviceSlot implements DeviceSelectionTab {
    private String name;
    private boolean exists;
    private final int index;
    private boolean hasLayers;
    private boolean hasDrumPads;
    
    private boolean isExpanded;
    private final Device device;
    private boolean selected;
    private final List<LayerSlot> layerSlots = new ArrayList<>();
    
    public DeviceSlot(final int index, final Device device) {
        this.index = index;
        this.device = device;
        if (this.device != null) {
            final DeviceLayerBank layerBank = device.createLayerBank(64);
            for (int i = 0; i < layerBank.getSizeOfBank(); i++) {
                final LayerSlot layerSlot = new LayerSlot(i, layerBank.getItemAt(i));
                layerSlots.add(layerSlot);
            }
        }
    }
    
    public DeviceSlot(final int index, final String name) {
        this.index = index;
        this.name = name;
        this.device = null;
    }
    
    public List<String> getLayerList() {
        return layerSlots.stream().filter(LayerSlot::isExists).map(LayerSlot::getName).toList();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    public boolean isExists() {
        return exists;
    }
    
    public void setExists(final boolean exists) {
        this.exists = exists;
    }
    
    public int getIndex() {
        return index;
    }
    
    public void setHasLayers(final boolean hasLayers) {
        this.hasLayers = hasLayers;
    }
    
    public boolean hasLayers() {
        return hasLayers;
    }
    
    public boolean hasDrumPads() {return hasDrumPads;}
    
    public void setHasDrumPads(final boolean hasDrumPads) {
        this.hasDrumPads = hasDrumPads;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(final boolean selected) {
        this.selected = selected;
    }
    
    @Override
    public String getLayerCode() {
        if (!hasLayers && !hasDrumPads) {
            return name;
        }
        if (isExpanded) {
            return "{\"n\":\"%s\",\"c\":[%s]}".formatted(
                name,
                getLayerList().stream().map("\"%s\""::formatted).collect(Collectors.joining(",")));
        } else {
            return "{\"n\":\"%s\",\"c\":[]}".formatted(name);
        }
    }
    
    public void toggleExpanded() {
        this.isExpanded = !this.isExpanded;
    }
    
    public void select() {
        if (device != null) {
            device.selectInEditor();
        }
    }
    
    public Device selectLayer(final int slotIndex) {
        final LayerSlot layer = layerSlots.get(slotIndex);
        layer.select();
        return layer.getDevice();
    }
}
