package com.bitwig.extensions.controllers.novation.launchkey_mk4.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.LaunchkeyMk4Extension;

public class Launchkey49Mk4ExtensionDefinition extends LaunchkeyMk4ExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("ef68e909-d5f1-4557-8526-2ce6978cabaa");

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(
                new String[] {"MIDIIN2 (Launchkey MK4 49 MIDI)", "Launchkey MK4 49 MIDI"},
                new String[] {"MIDIOUT2 (Launchkey MK4 49 MIDI", "Launchkey MK4 49 MIDI"}
            );
        } else if (platformType == PlatformType.MAC) {
            list.add(
                new String[] {"Launchkey MK4 49 DAW Out", "Launchkey MK4 49 MIDI Out"},
                new String[] {"Launchkey MK4 49 DAW In", "Launchkey MK4 49 MIDI In"}
            );
        } else if (platformType == PlatformType.LINUX) {
            list.add(
                new String[] {"Launchkey MK4 49 Launchkey", "Launchkey MK4 49 Launchkey #2"},
                new String[] {"Launchkey MK4 49 Launchkey", "Launchkey MK4 49 Launchkey #2"}
            );
        }
    }

    @Override
    public LaunchkeyMk4Extension createInstance(final ControllerHost host) {
        return new LaunchkeyMk4Extension(this, host, true, false);
    }

}
