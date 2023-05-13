package com.bitwig.extensions.controllers.arturia.keystep;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class KeyStepExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("6102eb09-7f47-42c1-8462-239de519dcc9");

    public KeyStepExtensionDefinition() {
    }

    @Override
    public String getName() {
        return "Keystep";
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
        return "Arturia";
    }

    @Override
    public String getHardwareModel() {
        return "Keystep";
    }

    @Override
    public int getRequiredAPIVersion() {
        return 16;
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
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                               final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(new String[]{"Arturia KeyStep 32"}, new String[]{"Arturia KeyStep 32"});
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[]{"Arturia KeyStep 32"}, new String[]{"Arturia KeyStep 32"});
        } else if (platformType == PlatformType.LINUX) {
            list.add(new String[]{"Arturia KeyStep 32"}, new String[]{"Arturia KeyStep 32"});
        }
    }

    @Override
    public KeyStepProExtension createInstance(final ControllerHost host) {
        return new KeyStepProExtension(this, host);
    }
}
