package com.bitwig.extensions.controllers.novation.launchpad_mini;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchpadMiniDefinition extends ControllerExtensionDefinition {

    private final static UUID ID = UUID.fromString("09bba665-d1df-4bc8-8cd6-b108875cacf7");

    @Override
    public String getHardwareVendor() {
        return "Novation";
    }

    @Override
    public String getHardwareModel() {
        return "Launchpad Mini mk3";
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
    public void listAutoDetectionMidiPortNames(AutoDetectionMidiPortNamesList list, PlatformType platformType) {
        switch (platformType)
      {
         case MAC:
            list.add(new String[]{"Launchpad X", "Launchpad X"}, new String[]{"Launchpad X"});
            break;
         case WINDOWS:
            list.add(new String[]{"Launchpad X", "Launchpad X"}, new String[]{"Launchpad X"});
            break;
         case LINUX:
            list.add(new String[]{"Launchpad X", "Launchpad X"}, new String[]{"Launchpad X"});
            break;
      }
        
    }

    @Override
    public ControllerExtension createInstance(ControllerHost host) {
        return new LaunchpadMini(this, host);
    }

    @Override
    public String getName() {
        return "Novation Launchpad Mini mk3";
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
        return ID;
    }

    @Override
    public int getRequiredAPIVersion() {
        return 16;
    }
    
}
