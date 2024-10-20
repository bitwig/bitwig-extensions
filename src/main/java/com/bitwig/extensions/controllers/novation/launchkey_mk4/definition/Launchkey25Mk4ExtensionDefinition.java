package com.bitwig.extensions.controllers.novation.launchkey_mk4.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.LaunchkeyMk4Extension;

public class Launchkey25Mk4ExtensionDefinition extends LaunchkeyMk4ExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("ef68e909-d5f1-4557-8526-2ce6978cabbb");

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public int numberOfKeys() {
        return 25;
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(
                new String[] {"MIDIIN2 (Launchkey MK4 25 MIDI", "Launchkey MK4 25 MIDI"},
                new String[] {"MIDIOUT2 (Launchkey MK4 25 MIDI", "Launchkey MK4 25 MIDI"}
            );
        }
        else if (platformType == PlatformType.MAC) {
            list.add(
                new String[] {"Launchkey MK4 25 DAW Out", "Launchkey MK4 25 MIDI Out"},
                new String[] {"Launchkey MK4 25 DAW In", "Launchkey MK4 25 MIDI In"}
            );
        }
        else if (platformType == PlatformType.LINUX) {
            list.add(
                new String[] {"Launchkey MK4 25 Launchkey", "Launchkey MK4 25 Launchkey #2"},
                new String[] {"Launchkey MK4 25 Launchkey", "Launchkey MK4 25 Launchkey #2"}
            );
        }
    }
    // Added MIDI IN: Launchkey MK4 49 MIDI Out
    // Added MIDI IN: Launchkey MK4 49 DAW Out

    @Override
    public LaunchkeyMk4Extension createInstance(final ControllerHost host) {
        return new LaunchkeyMk4Extension(this, host, false, false);
    }

}
