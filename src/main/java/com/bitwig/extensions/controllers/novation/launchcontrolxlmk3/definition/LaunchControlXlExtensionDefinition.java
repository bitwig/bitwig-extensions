package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlMk3Extension;

public class LaunchControlXlMk3ExtensionDefinition extends AbstractLaunchControlExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("cdee004a-1503-487c-bc13-a8311bf1724b");
    
    public LaunchControlXlMk3ExtensionDefinition() {
    }
    
    @Override
    public boolean isXlVersion() {
        return true;
    }
    
    @Override
    public String getName() {
        return "Launch Control XL Mk3";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareModel() {
        return "Launch Control XL Mk3";
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Novation/Launch Control XL Mk3.pdf";
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(new String[] {"MIDIIN2 (LCXL3 1 MIDI)"}, new String[] {"MIDIOUT2 (LCXL3 1 MIDI)"});
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[] {"LCXL3 1 DAW Out"}, new String[] {"LCXL3 1 DAW In"});
        } else if (platformType == PlatformType.LINUX) {
            list.add(new String[] {"LCXL3 1 LCXL3 1 DAW Out"}, new String[] {"LCXL3 1 LCXL3 1 DAW In"});
        }
    }
    
    @Override
    public LaunchControlXlMk3Extension createInstance(final ControllerHost host) {
        return new LaunchControlXlMk3Extension(this, host);
    }
}
