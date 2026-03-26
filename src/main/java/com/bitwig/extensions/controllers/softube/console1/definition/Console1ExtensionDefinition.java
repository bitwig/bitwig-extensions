package com.bitwig.extensions.controllers.softube.console1.definition;

import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.softube.console1.Console1Extension;

public abstract class Console1ExtensionDefinition extends ControllerExtensionDefinition {
    
    
    public Console1ExtensionDefinition() {
    }
    
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
        return "Softube";
    }
    
    @Override
    public int getRequiredAPIVersion() {
        return 25;
    }
    
    @Override
    public int getNumMidiInPorts() {
        return 1;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 1;
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Softube/Direct DAW Control.pdf";
    }
    
    @Override
    public Console1Extension createInstance(final ControllerHost host) {
        return new Console1Extension(this, host);
    }
    
    
}
