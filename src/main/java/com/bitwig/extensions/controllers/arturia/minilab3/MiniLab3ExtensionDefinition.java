package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class MiniLab3ExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("6d0d76fc-b9b0-11ec-8422-0242ac120002");

    private static final String MIDI_NAME = "Minilab3 MIDI";

    public MiniLab3ExtensionDefinition() {
    }

    @Override
    public String getHelpFilePath() {
        return "Controllers/Arturia/Arturia MiniLab 3.pdf";
    }

    @Override
    public String getName() {
        return "MiniLab 3";
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
        return "MiniLab 3";
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
            list.add(new String[]{MIDI_NAME}, new String[]{MIDI_NAME});
            list.add(new String[]{"2- " + MIDI_NAME}, new String[]{"2- " + MIDI_NAME});
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[]{MIDI_NAME}, new String[]{MIDI_NAME});
        } else if (platformType == PlatformType.LINUX) {
            list.add(new String[]{"Minilab3"}, new String[]{"Minilab3"});
        }
    }

    @Override
    public MiniLab3Extension createInstance(final ControllerHost host) {
        return new MiniLab3Extension(this, host);
    }
}
