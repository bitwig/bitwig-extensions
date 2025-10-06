package com.bitwig.extensions.controllers.neuzeitinstruments;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class DropExtensionDefinition extends ControllerExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("a817b682-a2ae-4ea6-8ec0-a53acae289df");
    
    public DropExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "Drop";
    }
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "1.00";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareVendor() {
        return "Neuzeit Instruments";
    }
    
    @Override
    public String getHardwareModel() {
        return "Drop";
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
    
    @Override
    public boolean isUsingBetaAPI() {
        return false;
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Neuzeit Instruments/Neuzeit Instruments Drop.pdf";
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS || platformType == PlatformType.MAC) {
            list.add(new String[] {"DROP USB1"}, new String[] {"DROP USB1"});
        } else {
            list.add(new String[] {"DROP USB1 MIDI1"}, new String[] {"DROP USB1 MIDI1"});
        }
    }
    
    @Override
    public DropExtension createInstance(final ControllerHost host) {
        return new DropExtension(this, host);
    }
    
    
}
