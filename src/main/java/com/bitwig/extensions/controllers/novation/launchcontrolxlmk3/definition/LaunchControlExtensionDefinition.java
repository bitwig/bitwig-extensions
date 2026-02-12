package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMk3Extension;

public class LaunchControlExtensionDefinition extends AbstractLaunchControlExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("cdee004a-1503-487c-bc13-a8311bf1724c");
    
    public LaunchControlExtensionDefinition() {
    }
    
    @Override
    public boolean isXlVersion() {
        return false;
    }
    
    @Override
    public String getName() {
        return "Launch Control 3";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareModel() {
        return "Launch Control 3";
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Novation/Launch Control 3.pdf";
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(new String[] {"MIDIIN2 (LC3 1 MIDI)"}, new String[] {"MIDIOUT2 (LC3 1 MIDI)"});
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[] {"LC3 1 DAW Out"}, new String[] {"LC3 1 DAW In"});
        } else if (platformType == PlatformType.LINUX) {
            list.add(new String[] {"LC3 1 LC3 1 DAW Out"}, new String[] {"LC3 1 LC3 1 DAW In"});
        }
    }
    
    @Override
    public LaunchControlMk3Extension createInstance(final ControllerHost host) {
        return new LaunchControlMk3Extension(this, host);
    }
}
