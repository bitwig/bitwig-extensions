package com.bitwig.extensions.controllers.allenheath.xonek3;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class XoneK3ExtensionX2Definition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("547d4e10-389a-492c-b7d7-a2c3c45df90f");
    
    public XoneK3ExtensionX2Definition() {
    }
    
    @Override
    public String getName() {
        return "Xone:K3 x2";
    }
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "0.9";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareVendor() {
        return "Allen & Heath";
    }
    
    @Override
    public String getHardwareModel() {
        return "Xone:K3 x2";
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
        return 2;
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/AllenHeath/Allen & Heath Xone K3.pdf";
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS || platformType == PlatformType.MAC) {
            list.add(new String[] {"XONE:K3", "XONE:K3 #2"}, new String[] {"XONE:K3", "XONE:K3 #2"});
            list.add(new String[] {"XONE:K3 Bitwig", "XONE:K3"}, new String[] {"XONE:K3 Bitwig", "XONE:K3"});
        } else {
            list.add(
                new String[] {"XONE:K3 MIDI 1", "XONE:K3 MIDI 2"}, new String[] {"XONE:K3 MIDI 1", "XONE:K3 MIDI 2"});
            list.add(
                new String[] {"XONE:K3 MIDI 1 Bitwig", "XONE:K3 MIDI 2"},
                new String[] {"XONE:K3 MIDI 1 Bitwig", "XONE:K3 MIDI 2"});
        }
    }
    
    @Override
    public XoneK3ControllerExtension createInstance(final ControllerHost host) {
        return new XoneK3ControllerExtension(this, host, 2);
    }
}
