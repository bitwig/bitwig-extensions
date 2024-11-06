package com.bitwig.extensions.controllers.novation.launchkey_mk4.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.LaunchkeyMk4Extension;

public class Launchkey37MiniMk4ExtensionDefinition extends LaunchkeyMk4ExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("ef68e909-d5f2-4557-8526-2ce6978fabcd");

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(
                new String[] {"MIDIIN2 (Launchkey Mini MK4 37 MIDI", "Launchkey Mini MK4 37 MIDI"},
                new String[] {"MIDIOUT2 (Launchkey Mini MK4 37 MIDI", "Launchkey Mini MK4 37 MIDI"}
            );
        } else if (platformType == PlatformType.MAC) {
            list.add(
                new String[] {"Launchkey Mini MK4 27 DAW Out", "Launchkey Mini MK4 37 MIDI Out"},
                new String[] {"Launchkey Mini MK4 37 DAW In", "Launchkey Mini MK4 37 MIDI In"}
            );
        } else if (platformType == PlatformType.LINUX) {
            list.add(
                new String[] {"Launchkey Mini MK4 37 Launchkey", "Launchkey Mini MK4 37 Launchkey #2"},
                new String[] {"Launchkey Mini MK4 37 Launchkey", "Launchkey Mini MK4 37 Launchkey #2"}
            );
        }
    }

    @Override
    public LaunchkeyMk4Extension createInstance(final ControllerHost host) {
        return new LaunchkeyMk4Extension(this, host, false, true);
    }

}
