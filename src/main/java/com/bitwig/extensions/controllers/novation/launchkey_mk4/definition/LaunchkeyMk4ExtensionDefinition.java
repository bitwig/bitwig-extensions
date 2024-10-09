package com.bitwig.extensions.controllers.novation.launchkey_mk4.definition;

import com.bitwig.extension.controller.ControllerExtensionDefinition;

public abstract class LaunchkeyMk4ExtensionDefinition extends ControllerExtensionDefinition {
    
    
    public abstract int numberOfKeys();
    
    @Override
    public String getName() {
        return String.format("Launchkey %d Mk4", numberOfKeys());
    }
    
    @Override
    public String getHardwareModel() {
        return String.format("Launchkey %d Mk4", numberOfKeys());
    }
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "0.1";
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
    public String getHelpFilePath() {
        return "Controllers/Novation/LaunchKey Mk3.pdf";
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
