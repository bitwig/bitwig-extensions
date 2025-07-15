package com.bitwig.extensions.controllers.nativeinstruments.komplete.definition;

import com.bitwig.extension.controller.ControllerExtensionDefinition;

public abstract class AbstractKompleteKontrolExtensionDefinition extends ControllerExtensionDefinition {
    
    private final String modelName;
    
    protected AbstractKompleteKontrolExtensionDefinition(final String modelName) {
        this.modelName = "Komplete Kontrol %s".formatted(modelName);
    }
    
    @Override
    public String getName() {
        return modelName;
    }
    
    @Override
    public String getHardwareModel() {
        return modelName;
    }
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "1.5";
    }
    
    @Override
    public int getRequiredAPIVersion() {
        return 22;
    }
    
    @Override
    public int getNumMidiInPorts() {
        return 2;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 2;
    }
    
    @Override
    public String getHardwareVendor() {
        return "Native Instruments";
    }
    
    
}
