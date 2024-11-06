package com.bitwig.extensions.controllers.novation.launchkey_mk4.definition;

import java.util.List;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.LaunchkeyMk4Extension;

public class LaunchkeyMiniMk4ExtensionDefinition extends LaunchkeyMk4ExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("ef68e909-d5f2-7857-8526-2ce6978fab11");
    private static final List<Integer> VARIANTS = List.of(25, 37);

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getName() {
        return "Launchkey Mini Mk4";
    }

    @Override
    public String getHardwareModel() {
        return "Launchkey Mini Mk4";
    }

    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            VARIANTS.forEach(keys -> list.add(new String[] {
                "MIDIIN2 (Launchkey Mini MK4 %d MIDI)".formatted(keys), "Launchkey Mini MK4 %d MIDI".formatted(keys)
            }, new String[] {
                "MIDIOUT2 (Launchkey Mini MK4 %d MIDI".formatted(keys), "Launchkey Mini MK4 %d MIDI".formatted(keys)
            }));
        } else if (platformType == PlatformType.MAC) {
            VARIANTS.forEach(keys -> list.add(new String[] {
                "Launchkey Mini MK4 %d DAW Out".formatted(keys), "Launchkey Mini MK4 %d MIDI Out".formatted(keys)
            }, new String[] {
                "Launchkey Mini MK4 %d DAW In".formatted(keys), "Launchkey Mini MK4 %d MIDI In".formatted(keys)
            }));
        } else if (platformType == PlatformType.LINUX) {
            VARIANTS.forEach(keys -> list.add(new String[] {
                "Launchkey Mini MK4 %d Launchkey".formatted(keys), "Launchkey Mini MK4 %d Launchkey #2".formatted(keys)
            }, new String[] {
                "Launchkey Mini MK4 %d Launchkey".formatted(keys), "Launchkey Mini MK4 %d Launchkey #2".formatted(keys)
            }));
        }
    }


    @Override
    public LaunchkeyMk4Extension createInstance(final ControllerHost host) {
        return new LaunchkeyMk4Extension(this, host, false, true);
    }

}
