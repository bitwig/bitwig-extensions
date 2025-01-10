package com.bitwig.extensions.controllers.reloop;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class KeypadProExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("40401c06-6792-11ee-8c99-0242ac120002");
    
    public KeypadProExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "Keypad Pro";
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
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareVendor() {
        return "Reloop";
    }
    
    @Override
    public String getHardwareModel() {
        return "Keypad Pro";
    }
    
    @Override
    public int getRequiredAPIVersion() {
        return 18;
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
        return "Controllers/Reloop/Reloop Keypad Pro.pdf";
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS || platformType == PlatformType.MAC) {
            list.add(new String[] {"Keypad Pro"}, new String[] {"Keypad Pro"});
        } else {
            list.add(new String[] {"Keypad Pro MIDI 1"}, new String[] {"Keypad Pro MIDI 1"});
        }
    }
    
    @Override
    public KeypadProControllerExtension createInstance(final ControllerHost host) {
        return new KeypadProControllerExtension(this, host);
    }
}
