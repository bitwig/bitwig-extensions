package com.bitwig.extensions.controllers.novation.launchkey_mk4.definition;

import com.bitwig.extension.controller.ControllerExtensionDefinition;

public abstract class AbstractLaunchkeyMk4ExtensionDefinition extends ControllerExtensionDefinition {
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "1.1";
    }
    
    @Override
    public String getHardwareVendor() {
        return "Novation";
    }
    
    @Override
    public int getRequiredAPIVersion() {
        return 18;
    }
    
    @Override
    public int getNumMidiInPorts() {
        return 2;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 2;
    }
    
}
