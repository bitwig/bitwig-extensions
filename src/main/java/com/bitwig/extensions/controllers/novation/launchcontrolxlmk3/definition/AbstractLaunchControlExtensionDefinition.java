package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.definition;

import com.bitwig.extension.controller.ControllerExtensionDefinition;

public abstract class AbstractLaunchControlExtensionDefinition extends ControllerExtensionDefinition {
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "1.0";
    }
    
    @Override
    public String getHardwareVendor() {
        return "Novation";
    }
    
    @Override
    public int getRequiredAPIVersion() {
        return 24;
    }
    
    @Override
    public int getNumMidiInPorts() {
        return 1;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 1;
    }
    
    public abstract boolean isXlVersion();
}
