package com.bitwig.extensions.controllers.arturia.keystep;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class KeyStepProExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("7dbf3c9c-f7a9-4e50-ba20-22de96cae64a");

    public KeyStepProExtensionDefinition() {
    }

    @Override
    public String getName() {
        return "Keystep Pro";
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
        return "Keystep Pro";
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
            list.add(new String[]{"KeyStep Pro MIDI IN"}, new String[]{"KeyStep Pro MIDI OUT"});
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[]{"Keystep Pro"}, new String[]{"Keystep Pro"});
        } else if (platformType == PlatformType.LINUX) {
            list.add(new String[]{"Keystep Pro"}, new String[]{"Keystep Pro"});
        }
    }

    @Override
    public KeyStepProExtension createInstance(final ControllerHost host) {
        return new KeyStepProExtension(this, host);
    }
}
