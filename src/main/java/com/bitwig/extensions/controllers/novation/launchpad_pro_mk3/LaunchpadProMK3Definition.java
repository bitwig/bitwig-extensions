package com.bitwig.extensions.controllers.novation.launchpad_pro_mk3;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchpadProMK3Definition extends ControllerExtensionDefinition {

    private final static UUID ID = UUID.fromString("cbe66fe6-c620-47cc-997e-90a819e95c48");

    @Override
    public String getHardwareVendor() {
        return "Novation";
    }

    @Override
    public String getHardwareModel() {
        return "Launchpad Pro MK3";
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
    //     switch (platformType)
    //   {
    //      case MAC:
    //         list.add(new String[]{"Oxygen Pro 25 USB MIDI", "Oxygen Pro 25 Mackie/HUI"}, new String[]{"Oxygen Pro 25 Editor", "Oxygen Pro 25 Mackie/HUI"});
    //         break;
    //      case WINDOWS:
    //         // TODO: Edit
    //         list.add(new String[]{"Oxygen Pro 25 USB MIDI", "Oxygen Pro 25 Mackie/HUI"}, new String[]{"Oxygen Pro 25 Editor", "Oxygen Pro 25 Mackie/HUI"});
    //         break;
    //      case LINUX:
    //         // TODO: Edit
    //         list.add(new String[]{"Oxygen Pro 25 USB MIDI", "Oxygen Pro 25 Mackie/HUI"}, new String[]{"Oxygen Pro 25 Editor", "Oxygen Pro 25 Mackie/HUI"});
    //         break;
    //   }
    }

    @Override
    public ControllerExtension createInstance(ControllerHost host) {
        return new LaunchpadProMK3Workflow(this, host);
    }

    @Override
    public String getName() {
        return "Novation Launchpad Pro MK3";
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
