package com.bitwig.extensions.controllers.softube.console1.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;

public class Console1FaderExtensionDefinition extends Console1ExtensionDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("98630b94-ed23-4a11-9981-05d3995b4965");

    @Override
    public String getName() {
        return "Console 1 Fader Mk III";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getHardwareModel() {
        return "Console 1 Mk III";
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) { //
            list.add(new String[] {"Console 1 Fader Mk III"}, new String[] {"Console 1 Fader Mk III"});
        } else {
            list.add(new String[] {"Console 1 Fader Mk III"}, new String[] {"Console 1 Fader Mk III"});
            list.add(
                new String[] {"Console 1 Channel Mk III DAW FA0000000073"},
                new String[] {"Console 1 Channel Mk III DAW FA0000000073"});
        }
    }

}
