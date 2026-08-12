package com.bitwig.extensions.controllers.embodme;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class Erae2ExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("a4f71be3-f377-411d-94b2-5a913c2080a3");
    
    public Erae2ExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "Erae 2";
    }
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "0.5";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareVendor() {
        return "Embodme";
    }
    
    @Override
    public String getHardwareModel() {
        return "Erae 2";
    }
    
    @Override
    public int getRequiredAPIVersion() {
        return 24;
    }
    
    @Override
    public int getNumMidiInPorts() {
        return 2;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 1;
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Embodme/Embodme Erae 2.pdf";
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        list.add(new String[] {"Erae 2", "MIDIIN2 (Erae 2)"}, new String[] {"Erae 2"});
    }
    
    @Override
    public Erae2ControllerExtension createInstance(final ControllerHost host) {
        return new Erae2ControllerExtension(this, host);
    }
}
