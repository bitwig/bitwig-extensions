package com.bitwig.extensions.controllers.arturia.minilab3;

import com.bitwig.extension.controller.ControllerExtensionDefinition;

public abstract class MiniLabExtensionDefinition extends ControllerExtensionDefinition {
    
    public MiniLabExtensionDefinition() {
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Arturia/Arturia MiniLab 3.pdf";
    }
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "1.10";
    }
    
    @Override
    public String getHardwareVendor() {
        return "Arturia";
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
}
