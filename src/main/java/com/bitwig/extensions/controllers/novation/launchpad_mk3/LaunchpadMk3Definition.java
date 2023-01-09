package com.bitwig.extensions.controllers.novation.launchpad_mk3;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public abstract class LaunchpadMk3Definition extends ControllerExtensionDefinition {

    protected final static String LAUNCHPAD_MINI_MODEL = "Mini";
    protected final static String LAUNCHPAD_X_MODEL = "X";
    private String MODEL_NAME;

    abstract String getModelName();

    @Override
    public String getHardwareVendor() {
        return "Novation";
    }

    @Override
    public String getHardwareModel() {
        return "Launchpad " + getModelName() + " mk3";
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
    //         list.add(new String[]{"Launchpad X", "Launchpad X"}, new String[]{"Launchpad X"});
    //         break;
    //      case WINDOWS:
    //         list.add(new String[]{"Launchpad X", "Launchpad X"}, new String[]{"Launchpad X"});
    //         break;
    //      case LINUX:
    //         list.add(new String[]{"Launchpad X", "Launchpad X"}, new String[]{"Launchpad X"});
    //         break;
    //   }
        
    }

    @Override
    public ControllerExtension createInstance(ControllerHost host) {
        return new LaunchpadMk3(this, host, getModelName());
    }

    @Override
    public String getName() {
        return "Launchpad " + getModelName() + " mk3";
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
    public int getRequiredAPIVersion() {
        return 16;
    }
    
}
